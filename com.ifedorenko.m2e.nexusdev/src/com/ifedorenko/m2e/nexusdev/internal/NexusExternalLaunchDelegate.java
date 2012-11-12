package com.ifedorenko.m2e.nexusdev.internal;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.codehaus.plexus.util.DirectoryScanner;
import org.codehaus.plexus.util.WriterFactory;
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
import org.eclipse.debug.core.sourcelookup.ISourceLookupParticipant;
import org.eclipse.jdt.internal.launching.JavaSourceLookupDirector;
import org.eclipse.jdt.launching.JavaLaunchDelegate;
import org.eclipse.jdt.launching.sourcelookup.containers.JavaSourceLookupParticipant;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.project.IMavenProjectFacade;

import com.ifedorenko.m2e.sourcelookup.internal.SourceLookupMavenLaunchParticipant;

@SuppressWarnings( "restriction" )
public class NexusExternalLaunchDelegate
    extends JavaLaunchDelegate
{
    public static final String ATTR_INSTALLATION_LOCATION = "nexusdev.installationLocation";

    private static final SourceLookupMavenLaunchParticipant sourcelookup = new SourceLookupMavenLaunchParticipant();

    private ILaunch launch;

    private IProgressMonitor monitor;

    private String mode;

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
            JavaSourceLookupDirector sourceLocator = new JavaSourceLookupDirector()
            {
                @Override
                public void initializeParticipants()
                {
                    List<ISourceLookupParticipant> participants = new ArrayList<ISourceLookupParticipant>();
                    if ( ILaunchManager.DEBUG_MODE.equals( mode ) )
                    {
                        participants.addAll( sourcelookup.getSourceLookupParticipants( configuration, launch, monitor ) );
                    }
                    participants.add( new JavaSourceLookupParticipant() );
                    addParticipants( participants.toArray( new ISourceLookupParticipant[participants.size()] ) );
                }
            };
            sourceLocator.initializeParticipants();

            launch.setSourceLocator( sourceLocator );

            writePluginRepositoryXml( new File( getNexusWorkingDirectory(), "plugin-repository.xml" ) );

            super.launch( configuration, mode, launch, monitor );
        }
        finally
        {
            this.mode = null;
            this.launch = null;
            this.monitor = null;
        }
    }

    private File getNexusWorkingDirectory()
    {
        // XXX
        return new File( "/tmp/sonatype-work/nexus" );
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
                                                 "Installation location is not a directory" ) );
        }

        return directory;
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
            append( sb, sourcelookup.getVMArguments( configuration, launch, monitor ) );
        }
        sb.append( " -Dnexus-work=" ).append( getNexusWorkingDirectory().getAbsolutePath() );
        return sb.toString();
    }

    private void append( StringBuilder sb, String str )
    {
        if ( str != null && !"".equals( str.trim() ) )
        {
            sb.append( ' ' ).append( str );
        }
    }

    private void writePluginRepositoryXml( File pluginRepositoryXml )
        throws CoreException
    {
        final IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();

        Xpp3Dom repositoryDom = new Xpp3Dom( "plugin-repository" );

        Xpp3Dom artifactsDom = new Xpp3Dom( "artifacts" );
        repositoryDom.addChild( artifactsDom );

        for ( IMavenProjectFacade project : MavenPlugin.getMavenProjectRegistry().getProjects() )
        {
            IFolder output = root.getFolder( project.getOutputLocation() );
            if ( "nexus-plugin".equals( project.getPackaging() ) && output.isAccessible() )
            {
                Xpp3Dom artifactDom = new Xpp3Dom( "artifact" );

                addChild( artifactDom, "location", output.getLocation().toOSString() );

                addChild( artifactDom, "groupId", project.getArtifactKey().getGroupId() );
                addChild( artifactDom, "artifactId", project.getArtifactKey().getArtifactId() );
                addChild( artifactDom, "version", project.getArtifactKey().getVersion() );
                addChild( artifactDom, "groupId", project.getArtifactKey().getGroupId() );
                addChild( artifactDom, "type", project.getPackaging() );

                artifactsDom.addChild( artifactDom );
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

    private static void addChild( Xpp3Dom dom, String name, String value )
    {
        Xpp3Dom child = new Xpp3Dom( name );
        child.setValue( value );
        dom.addChild( child );
    }
}
