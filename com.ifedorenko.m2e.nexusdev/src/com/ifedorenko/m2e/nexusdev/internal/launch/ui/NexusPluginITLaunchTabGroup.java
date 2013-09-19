package com.ifedorenko.m2e.nexusdev.internal.launch.ui;

import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTabGroup;
import org.eclipse.debug.ui.CommonTab;
import org.eclipse.debug.ui.EnvironmentTab;
import org.eclipse.debug.ui.ILaunchConfigurationDialog;
import org.eclipse.debug.ui.ILaunchConfigurationTab;
import org.eclipse.debug.ui.sourcelookup.SourceLookupTab;
import org.eclipse.jdt.debug.ui.launchConfigurations.JavaArgumentsTab;
import org.eclipse.jdt.debug.ui.launchConfigurations.JavaClasspathTab;
import org.eclipse.jdt.debug.ui.launchConfigurations.JavaJRETab;
import org.eclipse.jdt.junit.launcher.JUnitLaunchConfigurationTab;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.m2e.jdt.internal.launch.MavenRuntimeClasspathProvider;

@SuppressWarnings( "restriction" )
public class NexusPluginITLaunchTabGroup
    extends AbstractLaunchConfigurationTabGroup
{

    @Override
    public void createTabs( ILaunchConfigurationDialog dialog, String mode )
    {
        ILaunchConfigurationTab[] tabs = new ILaunchConfigurationTab[] { //
            new JUnitLaunchConfigurationTab(), //
                new NexusPluginsLaunchTab(), //
                new JavaArgumentsTab(), //
                new JavaClasspathTab(), //
                new JavaJRETab(), //
                new SourceLookupTab(), //
                new EnvironmentTab(), //
                new CommonTab() //
            };
        setTabs( tabs );
    }

    @Override
    public void performApply( ILaunchConfigurationWorkingCopy configuration )
    {
        super.performApply( configuration );

        // this is a really odd place to set default configuration attribute values

        configuration.setAttribute( IJavaLaunchConfigurationConstants.ATTR_CLASSPATH_PROVIDER,
                                    MavenRuntimeClasspathProvider.MAVEN_CLASSPATH_PROVIDER );
        configuration.setAttribute( IJavaLaunchConfigurationConstants.ATTR_SOURCE_PATH_PROVIDER,
                                    MavenRuntimeClasspathProvider.MAVEN_SOURCEPATH_PROVIDER );
    }
}
