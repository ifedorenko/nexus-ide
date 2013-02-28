package com.ifedorenko.m2e.nexusdev.internal.preferences;

public class NexusInstallation
{
    private final String kind;

    private final String version;

    private final String groupId;

    private final String artifactId;

    public NexusInstallation( String kind, String groupId, String artifactId, String version )
    {
        this.kind = kind;
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
    }

    public String getKind()
    {
        return kind;
    }

    public String getVersion()
    {
        return version;
    }

    public String getId()
    {
        return kind + "_" + version;
    }

    public String getGroupId()
    {
        return groupId;
    }

    public String getArtifactId()
    {
        return artifactId;
    }
}
