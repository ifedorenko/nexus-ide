package com.ifedorenko.m2e.nexusdev.internal.build;

public class NexusPluginXmlConfigurator
    extends AbstractMetadataGenerationConfigurator
{
    public NexusPluginXmlConfigurator()
    {
        super( "META-INF/nexus/plugin.xml", "nexus-plugin-bundle/plugin.classpath" );
    }
}
