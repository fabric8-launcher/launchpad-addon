/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.obsidiantoaster.generator.catalog;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.resource.DirectoryResource;
import org.obsidiantoaster.generator.CopyFileVisitor;
import org.yaml.snakeyaml.Yaml;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Singleton;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.obsidiantoaster.generator.Files.deleteRecursively;
import static org.obsidiantoaster.generator.Files.removeFileExtension;

/**
 * This service reads from the Quickstart catalog Github repository in
 * https://github.com/obsidian-toaster/quickstart-catalog and marshalls into {@link Quickstart} objects.
 * 
 * @author <a href="mailto:ggastald@redhat.com">George Gastaldi</a>
 */
@Singleton
public class QuickstartCatalogService
{
   private static final String GIT_REPOSITORY = "https://github.com/obsidian-toaster/quickstart-catalog.git";
   private static final String GIT_REF = "master";
   private static final Logger logger = Logger.getLogger(QuickstartCatalogService.class.getName());

   private final ReentrantReadWriteLock reentrantLock = new ReentrantReadWriteLock();

   private Path catalogPath;
   private volatile List<Quickstart> quickstarts;

   private ScheduledExecutorService executorService;

   /**
    * Clones the catalog git repository and reads the obsidian metadata on each quickstart repository
    */
   private void index() throws IOException
   {
      WriteLock lock = reentrantLock.writeLock();
      try
      {
         lock.lock();
         logger.info("Indexing contents ...");
         if (catalogPath == null)
         {
            catalogPath = Files.createTempDirectory("quickstart-catalog");

            logger.info("Created " + catalogPath);
            // Clone repository
            Git.cloneRepository()
                     .setURI(getEnvVarOrSysProp("CATALOG_GIT_REPOSITORY", GIT_REPOSITORY))
                     .setBranch(getEnvVarOrSysProp("CATALOG_GIT_REF", GIT_REF))
                     .setDirectory(catalogPath.toFile())
                     .call().close();
         }
         else
         {
            logger.info("Pulling changes to" + catalogPath);
            // Perform a git pull
            try (Git git = Git.open(catalogPath.toFile()))
            {
               git.pull().setRebase(true).call();
            }
            // Git pull on the existing repositories
            Path moduleRoot = catalogPath.resolve("modules");
            if (Files.isDirectory(moduleRoot))
            {
               for (File repository : moduleRoot.toFile().listFiles())
               {
                  try (Git git = Git.open(repository))
                  {
                     logger.info("Pulling changes to" + repository);
                     git.pull().setRebase(true).call();
                  }
               }
            }
         }
         final List<Quickstart> quickstarts = new ArrayList<>();
         final Yaml yaml = new Yaml();
         // Read the YAML files
         Files.walkFileTree(catalogPath, new SimpleFileVisitor<Path>()
         {
            @SuppressWarnings("unchecked")
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException
            {
               if (file.toString().endsWith(".yaml") || file.toString().endsWith(".yml"))
               {
                  String id = removeFileExtension(file.toFile().getName());
                  logger.info("Indexing " + file + " ...");
                  Path moduleDir = catalogPath.resolve("modules/" + id);
                  try (BufferedReader reader = Files.newBufferedReader(file))
                  {
                     // Read YAML entry
                     Quickstart quickstart = yaml.loadAs(reader, Quickstart.class);
                     // Quickstart ID = filename without extension
                     quickstart.setId(id);
                     // Module does not exist. Clone it
                     if (Files.notExists(moduleDir))
                     {
                        Git.cloneRepository()
                                 .setDirectory(moduleDir.toFile())
                                 .setURI("https://github.com/" + quickstart.getGithubRepo())
                                 .setBranch(quickstart.getGitRef())
                                 .call().close();
                     }
                     Path metadataPath = moduleDir.resolve(quickstart.getObsidianDescriptorPath());
                     try (BufferedReader metadataReader = Files.newBufferedReader(metadataPath))
                     {
                        Map<String, Object> metadata = yaml.loadAs(metadataReader, Map.class);
                        quickstart.setMetadata(metadata);
                     }
                     quickstarts.add(quickstart);
                  }
                  catch (GitAPIException gitException)
                  {
                     logger.log(Level.SEVERE, "Error while reading git repository", gitException);
                  }
               }
               return FileVisitResult.CONTINUE;
            }
         });
         quickstarts.sort(Comparator.comparing(Quickstart::getName));
         this.quickstarts = Collections.unmodifiableList(quickstarts);
      }
      catch (GitAPIException cause)
      {
         throw new IOException("Error while performing GIT operation", cause);
      }
      finally
      {
         logger.info("Finished content indexing");
         lock.unlock();
      }
   }

   @PostConstruct
   void init()
   {
      long indexPeriod = Long.parseLong(getEnvVarOrSysProp("CATALOG_INDEX_PERIOD", "30"));
      if (indexPeriod > 0L) 
      {
         executorService = Executors.newScheduledThreadPool(1);
         logger.info("Indexing every " + indexPeriod + " minutes");
         executorService.scheduleAtFixedRate(() -> {
            try
            {
               index();  
            }
            catch (IOException e)
            {
               logger.log(Level.SEVERE, "Error while indexing",e);
            }
         }, 0, indexPeriod, TimeUnit.MINUTES);
      } 
      else 
      {
         try
         {
            index();  
         }
         catch (IOException e)
         {
            logger.log(Level.SEVERE, "Error while indexing",e);
         }         
      }
   }

   @PreDestroy
   void destroy()
   {
      if (executorService != null)
      {
         executorService.shutdown();
      }
      if (catalogPath != null)
      {
         logger.info("Removing " + catalogPath);
         try
         {
         // Remove all the YAML files
            deleteRecursively(catalogPath);
         }
         catch (IOException e)
         {
            logger.log(Level.FINEST, "Error while deleting catalog path",e);
         }
      }
   }

   private static String getEnvVarOrSysProp(String name, String defaultValue)
   {
      return System.getProperty(name, System.getenv().getOrDefault(name, defaultValue));
   }

   public List<Quickstart> getQuickstarts()
   {
      Lock readLock = reentrantLock.readLock();
      try
      {
         readLock.lock();
         return quickstarts;
      }
      finally
      {
         readLock.unlock();
      }
   }

   /**
    * Copies the {@link Quickstart} contents to the specified {@link Project}
    */
   public Path copy(Quickstart quickstart, Project project, Predicate<Path> filter) throws IOException
   {
      Path modulePath = catalogPath.resolve("modules/" + quickstart.getId());
      Path to = project.getRoot().as(DirectoryResource.class).getUnderlyingResourceObject().toPath();
      return Files.walkFileTree(modulePath, new CopyFileVisitor(to, filter));
   }
}
