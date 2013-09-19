package com.ifedorenko.m2e.nexusdev.internal.launch;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
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
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.ArtifactKey;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.IMavenProjectRegistry;

import com.ifedorenko.m2e.binaryproject.BinaryProjectPlugin;
import com.ifedorenko.m2e.nexusdev.internal.NexusPluginXml;
import com.ifedorenko.m2e.nexusdev.internal.NexusdevActivator;

@SuppressWarnings( "restriction" )
class XmlPluginRepository
{
    private final IMavenProjectRegistry projectRegistry;

    private final IWorkspaceRoot root;

    public XmlPluginRepository()
    {
        this.projectRegistry = MavenPlugin.getMavenProjectRegistry();
        this.root = ResourcesPlugin.getWorkspace().getRoot();
    }

    public File getLocation( ILaunchConfiguration configuration )
        throws CoreException
    {
        File parent = new File( NexusdevActivator.getStateLocation().toFile(), configuration.getName() );
        parent.mkdirs();
        return new File( parent, "plugin-repository.xml" );
    }

    public void writePluginRepositoryXml( ILaunchConfiguration configuration, IProgressMonitor monitor )
        throws CoreException
    {
        Xpp3Dom repositoryDom = new Xpp3Dom( "plugin-repository" );

        Xpp3Dom artifactsDom = new Xpp3Dom( "artifacts" );
        repositoryDom.addChild( artifactsDom );

        Set<ArtifactKey> processed = new LinkedHashSet<ArtifactKey>();

        SelectedProjects selectedProjects = SelectedProjects.fromLaunchConfig( configuration );
        for ( IMavenProjectFacade project : projectRegistry.getProjects() )
        {
            if ( selectedProjects.isSelected( project ) )
            {
                addWorkspacePlugin( configuration, artifactsDom, project, processed, monitor );
            }
        }

        try
        {
            File pluginRepositoryXml = getLocation( configuration );
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

    private void addWorkspacePlugin( ILaunchConfiguration configuration, Xpp3Dom artifactsDom,
                                     IMavenProjectFacade project, Set<ArtifactKey> processed, IProgressMonitor monitor )
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

                if ( configuration.getAttribute( NexusExternalLaunchDelegate.ATTR_ADD_REQUIRED_PLUGINS, true ) )
                {
                    for ( Artifact dependency : nexusPluginXml.getPluginDependencies().values() )
                    {
                        IMavenProjectFacade other =
                            projectRegistry.getMavenProject( dependency.getGroupId(), dependency.getArtifactId(),
                                                             dependency.getBaseVersion() );
                        if ( other != null && other.getFullPath( dependency.getFile() ) != null )
                        {
                            addWorkspacePlugin( configuration, artifactsDom, other, processed, monitor );
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
