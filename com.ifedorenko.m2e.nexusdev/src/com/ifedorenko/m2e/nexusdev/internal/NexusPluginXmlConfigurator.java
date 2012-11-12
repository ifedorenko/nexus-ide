package com.ifedorenko.m2e.nexusdev.internal;

import java.util.Set;

import org.apache.maven.plugin.MojoExecution;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.m2e.core.lifecyclemapping.model.IPluginExecutionMetadata;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.configurator.AbstractBuildParticipant;
import org.eclipse.m2e.core.project.configurator.AbstractProjectConfigurator;
import org.eclipse.m2e.core.project.configurator.MojoExecutionBuildParticipant;
import org.eclipse.m2e.core.project.configurator.ProjectConfigurationRequest;

public class NexusPluginXmlConfigurator
    extends AbstractProjectConfigurator
{

    @Override
    public void configure( ProjectConfigurationRequest request, IProgressMonitor monitor )
        throws CoreException
    {
    }

    @Override
    public AbstractBuildParticipant getBuildParticipant( IMavenProjectFacade projectFacade, MojoExecution execution,
                                                         IPluginExecutionMetadata executionMetadata )
    {
        return new MojoExecutionBuildParticipant( execution, true )
        {
            public Set<IProject> build( int kind, IProgressMonitor monitor )
                throws Exception
            {
                IMavenProjectFacade facade = getMavenProjectFacade();
                IProject project = facade.getProject();
                IPath pluginXmlPath = facade.getOutputLocation().append( "META-INF/nexus/plugin.xml" );
                IFile pluginXml = project.getWorkspace().getRoot().getFile( pluginXmlPath );
                if ( shouldRegenerate( pluginXml ) )
                {
                    super.build( kind, monitor );
                    pluginXml.refreshLocal( IResource.DEPTH_ONE, monitor );
                }
                return null;
            }

            boolean shouldRegenerate( IFile pluginXml )
            {
                if ( !pluginXml.isAccessible() )
                {
                    return true;
                }

                IResourceDelta delta = getDelta( pluginXml.getProject() );

                if ( delta == null )
                {
                    // full build, iirc
                    return true;
                }

                return delta.findMember( pluginXml.getProjectRelativePath() ) != null;
            }
        };
    }
}
