package com.ifedorenko.m2e.nexusdev.internal.preferences;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class NexusInstallations
{
    private final Map<String, NexusInstallation> installations;

    public static final NexusInstallations INSTANCE = new NexusInstallations();

    private NexusInstallations()
    {
        Map<String, NexusInstallation> installations = new LinkedHashMap<String, NexusInstallation>();
        addNexusVersion( installations, "2.4-SNAPSHOT" );
        addNexusVersion( installations, "2.3.1-01" );

        this.installations = Collections.unmodifiableMap( installations );
    }

    private static void addNexusVersion( Map<String, NexusInstallation> installations, String version )
    {
        NexusInstallation oss = new NexusInstallation( "oss", "org.sonatype.nexus", "nexus-oss-webapp", version );
        installations.put( oss.getId(), oss );
        NexusInstallation pro = new NexusInstallation( "pro", "com-sonatype-nexus", "nexus-professional", version );
        installations.put( pro.getId(), pro );
    }

    public Map<String, NexusInstallation> getInstallations()
    {
        return installations;
    }

    public NexusInstallation getDefaultInstallation()
    {
        return installations.values().iterator().next();
    }

    public NexusInstallation getInstallation( String installationId )
    {
        return installationId != null ? installations.get( installationId ) : null;
    }
}
