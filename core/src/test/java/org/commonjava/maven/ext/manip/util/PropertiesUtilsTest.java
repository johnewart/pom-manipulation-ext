/**
 * Copyright (C) 2012 Red Hat, Inc. (jcasey@redhat.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.commonjava.maven.ext.manip.util;

import org.apache.commons.lang.reflect.FieldUtils;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.DefaultArtifactRepository;
import org.apache.maven.artifact.repository.layout.DefaultRepositoryLayout;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.settings.Settings;
import org.commonjava.maven.atlas.ident.ref.SimpleProjectVersionRef;
import org.commonjava.maven.ext.manip.ManipulationSession;
import org.commonjava.maven.ext.manip.io.ModelIO;
import org.commonjava.maven.ext.manip.model.Project;
import org.commonjava.maven.ext.manip.resolver.GalleyAPIWrapper;
import org.commonjava.maven.ext.manip.resolver.GalleyInfrastructure;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PropertiesUtilsTest
{
    @Rule
    public TemporaryFolder temp = new TemporaryFolder();

    @Test
    public void testCheckStrictValue() throws Exception
    {
        ManipulationSession session = new ManipulationSession();
        assertFalse( PropertiesUtils.checkStrictValue( session, null, "1.0" ) );
        assertFalse( PropertiesUtils.checkStrictValue( session, "1.0", null ) );
    }

    @Test
    public void testCacheProperty() throws Exception
    {
        Map propertyMap = new HashMap();

        assertFalse( PropertiesUtils.cacheProperty( propertyMap, null, "2.0", null, false ) );
        assertFalse( PropertiesUtils.cacheProperty( propertyMap, "1.0", "2.0", null, false ) );
        assertTrue( PropertiesUtils.cacheProperty( propertyMap, "${version.org.jboss}", "2.0", null, false ) );
    }

    @Test
    public void testResolveProperties() throws Exception
    {
        final Model modelChild = resolveModelResource( "inherited-properties.pom" );
        final Model modelParent = resolveRemoteModel( "org.infinispan:infinispan-bom:8.2.0.Final" );

        Project pP = new Project( modelParent );
        Project pC = new Project( modelChild );
        List<Project> al = new ArrayList<>();
        al.add( pC );
        al.add( pP );

        String result = PropertiesUtils.resolveProperties( al, "${version.scala.major}.${version.scala.minor}" );
        assertTrue( result.equals( "2.11.7" ) );

        result = PropertiesUtils.resolveProperties( al,
                                                    "TestSTART.and.${version.scala.major}.now.${version.scala.minor}" );
        assertTrue( result.equals( "TestSTART.and.2.11.now.7" ) );

        result = PropertiesUtils.resolveProperties( al, "${project.version}" );
        assertTrue( result.equals( "1" ) );
   }

    private Model resolveModelResource( final String resourceName ) throws Exception
    {
        final URL resource = Thread.currentThread().getContextClassLoader().getResource( resourceName );

        assertTrue( resource != null );

        return new MavenXpp3Reader().read( new FileReader( new File( resource.getPath() ) ) );
    }

    private Model resolveRemoteModel( final String resourceName ) throws Exception
    {
        List<ArtifactRepository> artifactRepos = new ArrayList<>();
        @SuppressWarnings( "deprecation" ) ArtifactRepository ar =
                        new DefaultArtifactRepository( "central", "http://central.maven.org/maven2/", new DefaultRepositoryLayout() );
        artifactRepos.add( ar );

        final GalleyInfrastructure galleyInfra =
                        new GalleyInfrastructure( temp.newFolder(), artifactRepos, null, new Settings(), Collections.<String>emptyList(), null, null, null,
                                                  temp.newFolder( "cache-dir" ) );
        final GalleyAPIWrapper wrapper = new GalleyAPIWrapper( galleyInfra );
        final ModelIO model = new ModelIO();
        FieldUtils.writeField( model, "galleyWrapper", wrapper, true );

        return model.resolveRawModel( SimpleProjectVersionRef.parse( resourceName ) );
    }
}