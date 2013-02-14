package com.ifedorenko.m2e.nexusdev.internal.launch.ui;

import org.eclipse.debug.ui.AbstractLaunchConfigurationTabGroup;
import org.eclipse.debug.ui.CommonTab;
import org.eclipse.debug.ui.EnvironmentTab;
import org.eclipse.debug.ui.ILaunchConfigurationDialog;
import org.eclipse.debug.ui.ILaunchConfigurationTab;
import org.eclipse.debug.ui.sourcelookup.SourceLookupTab;
import org.eclipse.jdt.debug.ui.launchConfigurations.JavaArgumentsTab;
import org.eclipse.jdt.debug.ui.launchConfigurations.JavaClasspathTab;
import org.eclipse.jdt.debug.ui.launchConfigurations.JavaJRETab;

public class NexusExternalLaunchTabGroup
    extends AbstractLaunchConfigurationTabGroup
{

    @Override
    public void createTabs( ILaunchConfigurationDialog dialog, String mode )
    {
        ILaunchConfigurationTab[] tabs = new ILaunchConfigurationTab[] { //
            new NexusExternalInstanceLaunchTab(), //
                new NexusPluginsLaunchTab(), //
                new JavaArgumentsTab(), //
                new JavaClasspathTab(), //
                new JavaJRETab(), //
                new SourceLookupTab(), //
                new EnvironmentTab(), //
                new CommonTab() };
        setTabs( tabs );
    }

}
