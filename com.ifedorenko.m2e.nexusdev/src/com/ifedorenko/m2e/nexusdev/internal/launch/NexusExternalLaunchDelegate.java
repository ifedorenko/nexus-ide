package com.ifedorenko.m2e.nexusdev.internal.launch;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.maven.artifact.Artifact;
import org.codehaus.plexus.util.DirectoryScanner;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.WriterFactory;
import org.codehaus.plexus.util.io.RawInputStreamFacade;
import org.codehaus.plexus.util.xml.XmlStreamWriter;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.Xpp3DomWriter;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.jdt.launching.JavaLaunchDelegate;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.ArtifactKey;
import org.eclipse.m2e.core.embedder.IMaven;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.IMavenProjectRegistry;

import com.ifedorenko.m2e.binaryproject.BinaryProjectPlugin;
import com.ifedorenko.m2e.nexusdev.internal.NexusPluginXml;
import com.ifedorenko.m2e.nexusdev.internal.NexusdevActivator;
import com.ifedorenko.m2e.nexusdev.internal.preferences.NexusInstallation;
import com.ifedorenko.m2e.nexusdev.internal.preferences.NexusInstallations;
import com.ifedorenko.m2e.sourcelookup.internal.SourceLookupMavenLaunchParticipant;

@SuppressWarnings( "restriction" )
public class NexusExternalLaunchDelegate
    extends JavaLaunchDelegate
{
    public static final String LAUNCHTYPE_ID = "com.ifedorenko.m2e.nexusdev.externalLaunchType";

    /**
     * Boolean. true==standard. false==custom.
     */
    public static final String ATTR_STANDARD_INSTALLATION = "nexusdev.standardInstallation";

    /**
     * String. Standard installation id.
     */
    public static final String ATTR_STANDARD_INSTALLATION_ID = "nexusdev.standardInstallationId";

    /**
     * String. Custom installation location.
     */
    public static final String ATTR_INSTALLATION_LOCATION = "nexusdev.installationLocation";

    public static final String ATTR_WORKDIR_LOCATION = "nexusdev.workdirLocation";

    public static final String ATTR_APPLICATION_PORT = "nexusdev.applicationPort";

    public static final String ATTR_SELECTED_PROJECTS = "nexusdev.selectedProjects";

    /**
     * Boolean. true==automatically add required workspace plugins of selected projects.
     */
    public static final String ATTR_ADD_REQUIRED_PLUGINS = "nexusdev.addRequiredPlugins";

    private static final NexusInstallations installations = NexusInstallations.INSTANCE;

    private ILaunch launch;

    private IProgressMonitor monitor;

    private String mode;

    private final IMavenProjectRegistry projectRegistry;

    private IWorkspaceRoot root;

    public NexusExternalLaunchDelegate()
    {
        this.projectRegistry = MavenPlugin.getMavenProjectRegistry();
        this.root = ResourcesPlugin.getWorkspace().getRoot();
    }

    @Override
    public void launch( final ILaunchConfiguration configuration, final String mode, final ILaunch launch,
                        final IProgressMonitor monitor )
        throws CoreException
    {
        this.mode = mode;
        this.launch = launch;
        this.monitor = monitor;
        try
        {
            launch.setSourceLocator( SourceLookupMavenLaunchParticipant.newSourceLocator( mode ) );

            writePluginRepositoryXml( getPluginRepositoryXml( configuration ),
                                      SelectedProjects.fromLaunchConfig( configuration ) );

            super.launch( configuration, mode, launch, monitor );
        }
        finally
        {
            this.mode = null;
            this.launch = null;
            this.monitor = null;
        }
    }

    private File getPluginRepositoryXml( final ILaunchConfiguration configuration )
        throws CoreException
    {
        File parent = new File( NexusdevActivator.getStateLocation().toFile(), configuration.getName() );
        parent.mkdirs();
        return new File( parent, "plugin-repository.xml" );
    }

    private File getNexusWorkingDirectory( ILaunchConfiguration configuration )
        throws CoreException
    {
        // String defaultWorkdirLocation = ResourcesPlugin.getWorkspace().get;
        String location = configuration.getAttribute( ATTR_WORKDIR_LOCATION, (String) null );

        if ( location != null )
        {
            return new File( location ).getAbsoluteFile();
        }

        return new File( NexusdevActivator.getStateLocation().toFile(), configuration.getName() );
    }

    @Override
    public File verifyWorkingDirectory( ILaunchConfiguration configuration )
        throws CoreException
    {
        return getNexusInstallationDirectory( configuration );
    }

    private File getNexusInstallationDirectory( ILaunchConfiguration configuration )
        throws CoreException
    {
        if ( configuration.getAttribute( ATTR_STANDARD_INSTALLATION, true ) )
        {
            String installationId = configuration.getAttribute( ATTR_STANDARD_INSTALLATION_ID, (String) null );

            NexusInstallation installation;
            if ( installationId != null )
            {
                installation = installations.getInstallation( installationId );
                if ( installation == null )
                {
                    throw new CoreException( new Status( IStatus.ERROR, NexusdevActivator.BUNDLE_ID,
                                                         "Unknown installation id " + installationId ) );
                }
            }
            else
            {
                installation = installations.getDefaultInstallation();
            }

            IMaven maven = MavenPlugin.getMaven();
            Artifact artifact =
                maven.resolve( installation.getGroupId(), installation.getArtifactId(), installation.getVersion(),
                               "zip", "bundle", null, monitor );

            try
            {
                return unzipIfNecessary( installation, artifact );
            }
            catch ( IOException e )
            {
                throw new CoreException( new Status( IStatus.ERROR, NexusdevActivator.BUNDLE_ID,
                                                     "Could not create nexus installation", e ) );
            }
        }

        String location = configuration.getAttribute( ATTR_INSTALLATION_LOCATION, (String) null );

        if ( location == null || "".equals( location.trim() ) )
        {
            throw new CoreException( new Status( IStatus.ERROR, NexusdevActivator.BUNDLE_ID,
                                                 "Installation location is null" ) );
        }

        File directory = new File( location );

        if ( !directory.isDirectory() )
        {
            throw new CoreException( new Status( IStatus.ERROR, NexusdevActivator.BUNDLE_ID,
                                                 "Installation location is not not found or not a directory" ) );
        }

        return directory;
    }

    private File unzipIfNecessary( NexusInstallation installation, Artifact artifact )
        throws IOException
    {
        File location =
            new File( NexusdevActivator.getStateLocation().toFile(), "installations/" + installation.getId() );
        if ( location.isDirectory() && location.lastModified() > artifact.getFile().lastModified() )
        {
            return location;
        }
        FileUtils.deleteDirectory( location );
        ZipFile zip = new ZipFile( artifact.getFile() );
        try
        {
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while ( entries.hasMoreElements() )
            {
                ZipEntry entry = entries.nextElement();
                if ( !entry.isDirectory() )
                {
                    String name = entry.getName();
                    if ( !name.startsWith( "nexus-" ) )
                    {
                        continue;
                    }
                    int idx = name.indexOf( '/' );
                    if ( idx < 0 )
                    {
                        continue;
                    }
                    name = name.substring( idx + 1 );
                    File target = new File( location, name );
                    target.getParentFile().mkdirs();
                    FileUtils.copyStreamToFile( new RawInputStreamFacade( zip.getInputStream( entry ) ), target );
                }
            }
        }
        finally
        {
            zip.close();
        }
        location.setLastModified( artifact.getFile().lastModified() );
        return location;
    }

    @Override
    public String verifyMainTypeName( ILaunchConfiguration configuration )
        throws CoreException
    {
        return "org.sonatype.nexus.bootstrap.Launcher";
    }

    @Override
    public String[] getClasspath( ILaunchConfiguration configuration )
        throws CoreException
    {

        DirectoryScanner ds = new DirectoryScanner();
        ds.setBasedir( getNexusInstallationDirectory( configuration ) );
        ds.setIncludes( new String[] { "lib/*.jar", "conf" } );
        ds.scan();

        List<String> cp = new ArrayList<String>();

        for ( String path : ds.getIncludedFiles() )
        {
            cp.add( path );
        }

        for ( String path : ds.getIncludedDirectories() )
        {
            cp.add( path );
        }

        return cp.toArray( new String[cp.size()] );
    }

    @Override
    public String getProgramArguments( ILaunchConfiguration configuration )
        throws CoreException
    {
        return "./conf/jetty.xml";
    }

    @Override
    public String getVMArguments( ILaunchConfiguration configuration )
        throws CoreException
    {
        StringBuilder sb = new StringBuilder();
        append( sb, super.getVMArguments( configuration ) );
        if ( ILaunchManager.DEBUG_MODE.equals( mode ) )
        {
            append( sb, SourceLookupMavenLaunchParticipant.getVMArguments() );
        }
        sb.append( " -Dnexus.nexus-work=" ).append( quote( getNexusWorkingDirectory( configuration ) ) );
        sb.append( " -Dnexus.xml-plugin-repository=" ).append( quote( getPluginRepositoryXml( configuration ) ) );
        sb.append( " -Djetty.application-port=" ).append( configuration.getAttribute( ATTR_APPLICATION_PORT, "8081" ) );
        return sb.toString();
    }

    private String quote( File file )
    {
        return StringUtils.quoteAndEscape( file.getAbsolutePath(), '"' );
    }

    private void append( StringBuilder sb, String str )
    {
        if ( str != null && !"".equals( str.trim() ) )
        {
            sb.append( ' ' ).append( str );
        }
    }

    private void writePluginRepositoryXml( File pluginRepositoryXml, SelectedProjects selectedProjects )
        throws CoreException
    {
        Xpp3Dom repositoryDom = new Xpp3Dom( "plugin-repository" );

        Xpp3Dom artifactsDom = new Xpp3Dom( "artifacts" );
        repositoryDom.addChild( artifactsDom );

        Set<ArtifactKey> processed = new LinkedHashSet<ArtifactKey>();

        for ( IMavenProjectFacade project : projectRegistry.getProjects() )
        {
            if ( selectedProjects.isSelected( project ) )
            {
                addWorkspacePlugin( artifactsDom, project, processed );
            }
        }

        try
        {
            pluginRepositoryXml.getParentFile().mkdirs();
            XmlStreamWriter writer = WriterFactory.newXmlWriter( pluginRepositoryXml );
            try
            {
                Xpp3DomWriter.write( writer, repositoryDom );
            }
            finally
            {
                writer.close();
            }
        }
        catch ( IOException e )
        {
            throw new CoreException( new Status( IStatus.ERROR, NexusdevActivator.BUNDLE_ID,
                                                 "Could not write nexus plugin-repository.xlm file", e ) );
        }
    }

    private void addWorkspacePlugin( Xpp3Dom artifactsDom, IMavenProjectFacade project, Set<ArtifactKey> processed )
        throws CoreException
    {
        IFolder output = root.getFolder( project.getOutputLocation() );
        String packaging = project.getPackaging();
        if ( "nexus-plugin".equals( packaging ) && output.isAccessible() )
        {
            ArtifactKey artifactKey = project.getArtifactKey();
            if ( processed.add( artifactKey ) )
            {
                addArtifact( artifactsDom, artifactKey, packaging, output.getLocation().toOSString() );

                NexusPluginXml nexusPluginXml = new NexusPluginXml( project, monitor );

                for ( Map.Entry<ArtifactKey, Artifact> entry : nexusPluginXml.getClasspathDependencies().entrySet() )
                {
                    ArtifactKey dependencyKey = entry.getKey();
                    Artifact dependency = entry.getValue();
                    addArtifact( artifactsDom, dependencyKey, dependency.getArtifactHandler().getExtension(),
                                 dependency.getFile().getAbsolutePath() );
                }

                if ( launch.getLaunchConfiguration().getAttribute( ATTR_ADD_REQUIRED_PLUGINS, true ) )
                {
                    for ( Artifact dependency : nexusPluginXml.getPluginDependencies().values() )
                    {
                        IMavenProjectFacade other =
                            projectRegistry.getMavenProject( dependency.getGroupId(), dependency.getArtifactId(),
                                                             dependency.getBaseVersion() );
                        if ( other != null && other.getFullPath( dependency.getFile() ) != null )
                        {
                            addWorkspacePlugin( artifactsDom, other, processed );
                        }
                    }
                }
            }
        }
    }

    private void addArtifact( Xpp3Dom artifactsDom, ArtifactKey artifactKey, String packaging, String location )
        throws CoreException
    {
        String _location = null;
        if ( location.endsWith( "/pom.xml" ) )
        {
            // TODO find a better way to identify and resolve workspace binary projects

            IMavenProjectFacade facade =
                projectRegistry.getMavenProject( artifactKey.getGroupId(), artifactKey.getArtifactId(),
                                                 artifactKey.getVersion() );
            if ( facade != null )
            {
                _location = facade.getProject().getPersistentProperty( BinaryProjectPlugin.QNAME_JAR );
            }
        }

        if ( _location == null )
        {
            _location = location;
        }

        Xpp3Dom artifactDom = new Xpp3Dom( "artifact" );
        addChild( artifactDom, "location", _location );
        addChild( artifactDom, "groupId", artifactKey.getGroupId() );
        addChild( artifactDom, "artifactId", artifactKey.getArtifactId() );
        addChild( artifactDom, "version", artifactKey.getVersion() );
        addChild( artifactDom, "type", packaging );

        artifactsDom.addChild( artifactDom );
    }

    private static void addChild( Xpp3Dom dom, String name, String value )
    {
        Xpp3Dom child = new Xpp3Dom( name );
        child.setValue( value );
        dom.addChild( child );
    }

}
