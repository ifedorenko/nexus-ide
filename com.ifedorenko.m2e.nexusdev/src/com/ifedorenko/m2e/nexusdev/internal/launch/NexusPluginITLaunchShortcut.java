package com.ifedorenko.m2e.nexusdev.internal.launch;

import org.eclipse.jdt.junit.launcher.JUnitLaunchShortcut;

public class NexusPluginITLaunchShortcut
    extends JUnitLaunchShortcut
{
    protected String getLaunchConfigurationTypeId()
    {
        return "com.ifedorenko.m2e.nexusdev.pluginITLaunchType";
    }
}
