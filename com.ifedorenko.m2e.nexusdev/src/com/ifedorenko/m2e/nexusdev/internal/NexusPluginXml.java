package com.ifedorenko.m2e.nexusdev.internal;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.ReaderFactory;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.Xpp3DomBuilder;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.m2e.core.embedder.ArtifactKey;
import org.eclipse.m2e.core.project.IMavenProjectFacade;

public class NexusPluginXml
{
    private final Map<ArtifactKey, Artifact> classpathDependencies;

    private final Map<ArtifactKey, Artifact> pluginDependencies;

    public NexusPluginXml( IMavenProjectFacade project, IProgressMonitor monitor )
        throws CoreException
    {
        IFile nexusPluginXml =
            project.getProject().getWorkspace().getRoot().getFile( project.getOutputLocation().append( "META-INF/nexus/plugin.xml" ) );
        MavenProject mavenProject = project.getMavenProject( monitor );
        Map<ArtifactKey, Artifact> dependencies = toDependencyMap( mavenProject.getArtifacts() );

        Map<ArtifactKey, Artifact> classpathDependencies = new LinkedHashMap<ArtifactKey, Artifact>();
        Map<ArtifactKey, Artifact> pluginDependencies = new LinkedHashMap<ArtifactKey, Artifact>();

        try
        {
            Xpp3Dom dom = Xpp3DomBuilder.build( ReaderFactory.newPlatformReader( nexusPluginXml.getContents() ) );
            Xpp3Dom cpd = dom.getChild( "classpathDependencies" );
            if ( cpd != null )
            {
                addDependencies( classpathDependencies, dependencies, cpd.getChildren( "classpathDependency" ) );
            }
            Xpp3Dom pd = dom.getChild( "pluginDependencies" );
            if ( pd != null )
            {
                addDependencies( pluginDependencies, dependencies, pd.getChildren( "pluginDependency" ) );
            }
        }
        catch ( IOException e )
        {
        }
        catch ( XmlPullParserException e )
        {
        }

        this.classpathDependencies = Collections.unmodifiableMap( classpathDependencies );
        this.pluginDependencies = Collections.unmodifiableMap( pluginDependencies );
    }

    static void addDependencies( Map<ArtifactKey, Artifact> classpathDependencies,
                                 Map<ArtifactKey, Artifact> dependencies, Xpp3Dom[] xxx )
    {
        for ( Xpp3Dom cpe : xxx )
        {
            ArtifactKey dependencyKey = toDependencyKey( cpe );
            Artifact dependency = dependencies.get( dependencyKey );

            // dependency == null means workspace project was not fully resolved
            if ( dependency != null )
            {
                classpathDependencies.put( dependencyKey, dependency );
            }
        }
    }

    public Map<ArtifactKey, Artifact> getClasspathDependencies()
    {
        return classpathDependencies;
    }

    public Map<ArtifactKey, Artifact> getPluginDependencies()
    {
        return pluginDependencies;
    }

    static Map<ArtifactKey, Artifact> toDependencyMap( Set<Artifact> artifacts )
    {
        Map<ArtifactKey, Artifact> result = new LinkedHashMap<ArtifactKey, Artifact>();
        for ( Artifact a : artifacts )
        {
            ArtifactKey k = new ArtifactKey( a.getGroupId(), a.getArtifactId(), a.getBaseVersion(), a.getClassifier() );
            result.put( k, a );
        }
        return result;
    }

    static ArtifactKey toDependencyKey( Xpp3Dom cpe )
    {
        String groupId = cpe.getChild( "groupId" ).getValue();
        String artifactId = cpe.getChild( "artifactId" ).getValue();
        String version = cpe.getChild( "version" ).getValue();
        String classifier = getChildText( cpe, "classifier" );

        return new ArtifactKey( groupId, artifactId, version, classifier );
    }

    static String getChildText( Xpp3Dom dom, String childName )
    {
        Xpp3Dom child = dom.getChild( childName );
        return child != null ? child.getValue() : null;
    }
}
