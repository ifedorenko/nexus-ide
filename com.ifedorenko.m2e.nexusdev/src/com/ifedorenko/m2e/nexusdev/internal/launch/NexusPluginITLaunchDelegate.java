package com.ifedorenko.m2e.nexusdev.internal.launch;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.jdt.junit.launcher.JUnitLaunchConfigurationDelegate;

import com.ifedorenko.m2e.sourcelookup.internal.SourceLookupMavenLaunchParticipant;

@SuppressWarnings( "restriction" )
public class NexusPluginITLaunchDelegate
    extends JUnitLaunchConfigurationDelegate
{
    private static final XmlPluginRepository pluginRepository = new XmlPluginRepository();

    private String mode;

    @Override
    public synchronized void launch( ILaunchConfiguration configuration, String mode, ILaunch launch,
                                     IProgressMonitor monitor )
        throws CoreException
    {
        this.mode = mode;
        try
        {
            launch.setSourceLocator( SourceLookupMavenLaunchParticipant.newSourceLocator( mode ) );

            pluginRepository.writePluginRepositoryXml( configuration, monitor );

            super.launch( configuration, mode, launch, monitor );
        }
        finally
        {
            this.mode = null;
        }
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
        sb.append( " -Dnexus.xml-plugin-repository=" ).append( VMArguments.quote( pluginRepository.getLocation( configuration ) ) );
        return sb.toString();
    }
}
