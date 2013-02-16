package com.ifedorenko.m2e.nexusdev.internal.build;

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;

import org.apache.maven.plugin.MojoExecution;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
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

public abstract class AbstractMetadataGenerationConfigurator
    extends AbstractProjectConfigurator
{
    private Collection<String> paths;

    protected AbstractMetadataGenerationConfigurator( String... paths )
    {
        this.paths = Arrays.asList( paths );
    }

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
                if ( shouldRegenerate( facade ) )
                {
                    super.build( kind, monitor );
                    // add to build context to take advantage of the workaround for bug 368376
                    for ( String path : paths )
                    {
                        IFile ifile = getFile( facade, path );
                        getBuildContext().refresh( ifile.getLocation().toFile() );
                    }
                }
                return null;
            }

            boolean shouldRegenerate( IMavenProjectFacade facade )
            {
                for ( String path : paths )
                {
                    IFile ifile = getFile( facade, path );

                    if ( !ifile.isAccessible() )
                    {
                        return true;
                    }

                    IResourceDelta delta = getDelta( ifile.getProject() );

                    if ( delta == null )
                    {
                        // full build, iirc
                        return true;
                    }

                    if ( delta.findMember( ifile.getProjectRelativePath() ) != null )
                    {
                        return true;
                    }
                }
                return false;
            }

            IFile getFile( IMavenProjectFacade facade, String path )
            {
                IProject project = facade.getProject();
                IPath ipath = facade.getOutputLocation().append( path );
                IFile ifile = project.getWorkspace().getRoot().getFile( ipath );
                return ifile;
            }
        };
    }

}
