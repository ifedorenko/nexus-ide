package com.ifedorenko.m2e.nexusdev.internal;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class NexusdevActivator
    implements BundleActivator
{
    public static final String BUNDLE_ID = "com.ifedorenko.m2e.nexusdev";

    private static Bundle bundle;

    @Override
    public void start( BundleContext context )
        throws Exception
    {
        NexusdevActivator.bundle = context.getBundle();
    }

    @Override
    public void stop( BundleContext context )
        throws Exception
    {
        NexusdevActivator.bundle = null;
    }

    public static IPath getStateLocation()
    {
        return Platform.getStateLocation( bundle );
    }
}
