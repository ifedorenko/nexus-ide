package com.ifedorenko.m2e.nexusdev.internal.launch;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.ILaunchGroup;
import org.eclipse.debug.ui.ILaunchShortcut;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

public class NexusExternalLaunchShortcut
    implements ILaunchShortcut
{
    private ILaunchManager manager = DebugPlugin.getDefault().getLaunchManager();

    @Override
    public void launch( ISelection selection, String mode )
    {
        try
        {
            IProject project = getProject( selection );
            if ( project != null )
            {
                ILaunchConfiguration config = getLaunchConfiguration( project );
                if ( config != null )
                {
                    if ( config.getAttribute( NexusExternalLaunchDelegate.ATTR_INSTALLATION_LOCATION, (String) null ) != null )
                    {
                        DebugUITools.launch( config, mode );
                    }
                    else
                    {
                        ILaunchGroup group = DebugUITools.getLaunchGroup( config, mode );
                        DebugUITools.openLaunchConfigurationPropertiesDialog( getShell(), config, group.getIdentifier() );
                    }
                }
            }
        }
        catch ( CoreException e )
        {

        }
    }

    private IProject getProject( ISelection selection )
    {
        if ( selection instanceof IStructuredSelection )
        {
            Object object = ( (IStructuredSelection) selection ).getFirstElement();
            if ( object instanceof IProject )
            {
                return (IProject) object;
            }
            if ( object instanceof IAdaptable )
            {
                return (IProject) ( (IAdaptable) object ).getAdapter( IProject.class );
            }
        }
        return null;
    }

    @Override
    public void launch( IEditorPart editor, String mode )
    {
    }

    private ILaunchConfiguration getLaunchConfiguration( IProject project )
        throws CoreException
    {
        if ( project == null )
        {
            return null;
        }

        IMavenProjectFacade facade = MavenPlugin.getMavenProjectRegistry().getProject( project );

        ILaunchConfigurationType launchType =
            manager.getLaunchConfigurationType( NexusExternalLaunchDelegate.LAUNCHTYPE_ID );

        ILaunchConfiguration[] launchConfigurations = manager.getLaunchConfigurations( launchType );

        for ( ILaunchConfiguration launchConfiguration : launchConfigurations )
        {
            if ( match( launchConfiguration, facade ) )
            {
                return launchConfiguration;
            }
        }

        String configName = manager.generateLaunchConfigurationName( project.getName() );
        ILaunchConfigurationWorkingCopy workingCopy = launchType.newInstance( null, configName );

        new SelectedProjects( project ).toLaunchConfig( workingCopy );

        // workingCopy.setAttribute( IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, project.getName() );

        workingCopy.setMappedResources( new IResource[] { project } );

        return workingCopy.doSave();
    }

    private boolean match( ILaunchConfiguration config, IMavenProjectFacade facade )
        throws CoreException
    {
        if ( facade == null )
        {
            return false;
        }
        IResource[] resources = config.getMappedResources();
        if ( resources == null || resources.length == 0 )
        {
            return false;
        }
        for ( IResource resource : resources )
        {
            if ( resource == facade.getProject() )
            {
                return true;
            }
        }
        return false;
    }

    public static Shell getShell()
    {
        IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        if ( window == null )
        {
            IWorkbenchWindow[] windows = PlatformUI.getWorkbench().getWorkbenchWindows();
            if ( windows.length > 0 )
            {
                return windows[0].getShell();
            }
        }
        else
        {
            return window.getShell();
        }
        return null;
    }
}
