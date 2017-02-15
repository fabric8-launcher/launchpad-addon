/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.obsidiantoaster.generator.ui.quickstart;

import java.io.File;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URL;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import javax.enterprise.inject.Vetoed;

import org.apache.maven.archetype.catalog.Archetype;
import org.apache.maven.archetype.catalog.ArchetypeCatalog;
import org.apache.maven.archetype.catalog.io.xpp3.ArchetypeCatalogXpp3Reader;
import org.jboss.forge.addon.dependencies.Dependency;
import org.jboss.forge.addon.dependencies.builder.CoordinateBuilder;
import org.jboss.forge.addon.dependencies.builder.DependencyQueryBuilder;
import org.jboss.forge.addon.maven.dependencies.MavenDependencyResolver;
import org.jboss.forge.addon.resource.AbstractFileResource;
import org.jboss.forge.addon.resource.DefaultFileOperations;
import org.jboss.forge.addon.resource.FileOperations;
import org.jboss.forge.addon.resource.FileResource;
import org.jboss.forge.addon.resource.Resource;
import org.jboss.forge.addon.resource.ResourceFactory;
import org.jboss.forge.addon.resource.ResourceFilter;
import org.jboss.forge.addon.resource.monitor.ResourceMonitor;
import org.jboss.forge.addon.resource.transaction.ResourceTransaction;
import org.jboss.forge.addon.resource.transaction.ResourceTransactionListener;
import org.jboss.forge.furnace.spi.ListenerRegistration;
import org.jboss.forge.furnace.util.Assert;
import org.jboss.forge.furnace.util.Streams;

/**
 *
 * Flushes an archetype to the output directory
 * 
 * @author <a href="mailto:ggastald@redhat.com">George Gastaldi</a>
 */
@Vetoed
public class FlushArchetypes
{
   /**
    * Takes 2 arguments
    * 
    * - 1: the URL to the archetype
    * 
    * - 2: the path to flush the archetypes
    * 
    * @param args
    * @throws Exception
    */
   public static void main(String... args) throws Exception
   {
      Logger log = Logger.getLogger(FlushArchetypes.class.getName());
      String archetypeURL = args[0];
      String outputPath = args[1];
      Assert.notNull(archetypeURL, "Archetype URL was not specified");
      Assert.notNull(outputPath, "Output path was not specified");
      MavenDependencyResolver resolver = new MavenDependencyResolver(new FileResourceFactory());

      try (InputStream is = new URL(archetypeURL).openStream())
      {
         String archetypeContents = Streams.toString(is);
         File targetArchetype = new File(new File(outputPath).getParent(), "archetype-catalog.xml");
         log.info(String.format("Flushing archetype catalog XML to %s %n", targetArchetype));
         Files.write(targetArchetype.toPath(), archetypeContents.getBytes());
         ArchetypeCatalog catalog = new ArchetypeCatalogXpp3Reader().read(new StringReader(archetypeContents));
         for (Archetype archetype : catalog.getArchetypes())
         {
            CoordinateBuilder coordinate = CoordinateBuilder.create()
                     .setGroupId(archetype.getGroupId())
                     .setArtifactId(archetype.getArtifactId())
                     .setVersion(archetype.getVersion());
            Dependency artifact = resolver.resolveArtifact(DependencyQueryBuilder.create(coordinate));
            FileResource<?> file = artifact.getArtifact();
            File targetFile = new File(outputPath, archetype.getArtifactId() + ".jar").getAbsoluteFile();
            log.info(String.format("Copying %s to %s %n", file, targetFile));
            io.fabric8.utils.Files.copy(file.getUnderlyingResourceObject(), targetFile);
         }
      }
   }

   @Vetoed
   static class FileResourceFactory implements ResourceFactory
   {
      @Override
      @SuppressWarnings("unchecked")
      public <E, T extends Resource<E>> T create(Class<T> type, E underlyingResource)
      {
         return (T) create(underlyingResource);
      }

      @Override
      @SuppressWarnings("unchecked")
      public <E> Resource<E> create(E underlyingResource)
      {
         if (underlyingResource instanceof File)
            return (Resource<E>) createFileResource((File) underlyingResource);
         return null;
      }

      @SuppressWarnings({ "rawtypes", "unchecked" })
      private <E> Resource<E> createFileResource(File resource)
      {
         return new AbstractFileResource(this, resource)
         {
            @Override
            public Resource createFrom(File file)
            {
               return createFileResource(file);
            }

            @Override
            protected List<File> doListResources()
            {
               return Collections.emptyList();
            }
         };
      }

      @Override
      public ResourceMonitor monitor(Resource<?> resource)
      {
         throw new UnsupportedOperationException();
      }

      @Override
      public ResourceMonitor monitor(Resource<?> resource, ResourceFilter resourceFilter)
      {
         throw new UnsupportedOperationException();
      }

      @Override
      public ResourceTransaction getTransaction()
      {
         throw new UnsupportedOperationException();
      }

      @Override
      public FileOperations getFileOperations()
      {
         return DefaultFileOperations.INSTANCE;
      }

      @Override
      public ListenerRegistration<ResourceTransactionListener> addTransactionListener(
               ResourceTransactionListener listener)
      {
         throw new UnsupportedOperationException();
      }
   }
}