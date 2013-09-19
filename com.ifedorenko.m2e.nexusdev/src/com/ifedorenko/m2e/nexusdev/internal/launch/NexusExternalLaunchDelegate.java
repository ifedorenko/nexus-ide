package com.ifedorenko.m2e.nexusdev.internal.launch;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.maven.artifact.Artifact;
import org.codehaus.plexus.util.DirectoryScanner;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.io.RawInputStreamFacade;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.jdt.launching.JavaLaunchDelegate;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.IMaven;

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

    private static final XmlPluginRepository pluginRepository = new XmlPluginRepository();

    private IProgressMonitor monitor;

    private String mode;

    public NexusExternalLaunchDelegate()
    {
    }

    @Override
    public void launch( final ILaunchConfiguration configuration, final String mode, final ILaunch launch,
                        final IProgressMonitor monitor )
        throws CoreException
    {
        this.mode = mode;
        this.monitor = monitor;
        try
        {
            launch.setSourceLocator( SourceLookupMavenLaunchParticipant.newSourceLocator( mode ) );

            pluginRepository.writePluginRepositoryXml( configuration, monitor );

            super.launch( configuration, mode, launch, monitor );
        }
        finally
        {
            this.mode = null;
            this.monitor = null;
        }
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
        VMArguments.append( sb, super.getVMArguments( configuration ) );
        if ( ILaunchManager.DEBUG_MODE.equals( mode ) )
        {
            VMArguments.append( sb, SourceLookupMavenLaunchParticipant.getVMArguments() );
        }
        sb.append( " -Dnexus.nexus-work=" ).append( VMArguments.quote( getNexusWorkingDirectory( configuration ) ) );
        sb.append( " -Dnexus.xml-plugin-repository=" ).append( VMArguments.quote( pluginRepository.getLocation( configuration ) ) );
        sb.append( " -Djetty.application-port=" ).append( configuration.getAttribute( ATTR_APPLICATION_PORT, "8081" ) );
        return sb.toString();
    }

}
