/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package io.openshift.launchpad.catalog;

import static io.openshift.launchpad.Files.deleteRecursively;
import static io.openshift.launchpad.Files.removeFileExtension;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Singleton;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.resource.DirectoryResource;
import org.yaml.snakeyaml.Yaml;

import io.openshift.launchpad.CopyFileVisitor;

/**
 * This service reads from the Booster catalog Github repository in
 * https://github.com/obsidian-toaster/quickstart-catalog and marshalls into {@link Booster} objects.
 * 
 * @author <a href="mailto:ggastald@redhat.com">George Gastaldi</a>
 */
@Singleton
public class BoosterCatalogService
{
   private static final String GITHUB_URL = "https://github.com/";
   private static final String MODULES_DIR = "modules";
   private static final String CATALOG_INDEX_PERIOD_PROPERTY_NAME = "LAUNCHPAD_BACKEND_CATALOG_INDEX_PERIOD";
   private static final String CATALOG_GIT_REF_PROPERTY_NAME = "LAUNCHPAD_BACKEND_CATALOG_GIT_REF";
   private static final String CATALOG_GIT_REPOSITORY_PROPERTY_NAME = "LAUNCHPAD_BACKEND_CATALOG_GIT_REPOSITORY";

   private static final String DEFAULT_INDEX_PERIOD = "0";
   private static final String DEFAULT_GIT_REF = "master";
   private static final String DEFAULT_GIT_REPOSITORY_URL = "https://github.com/openshiftio/booster-catalog.git";
   /**
    * Files to be excluded from project creation
    */
   private static final List<String> EXCLUDED_PROJECT_FILES = Arrays.asList(".git", ".travis", ".travis.yml",
            ".ds_store",
            ".obsidian", ".gitmodules");

   private static final Logger logger = Logger.getLogger(BoosterCatalogService.class.getName());

   private final ReentrantReadWriteLock reentrantLock = new ReentrantReadWriteLock();

   private Path catalogPath;
   private volatile List<Booster> boosters = Collections.emptyList();

   private ScheduledExecutorService executorService;

   /**
    * Clones the catalog git repository and reads the obsidian metadata on each quickstart repository
    */
   private void index()
   {
      WriteLock lock = reentrantLock.writeLock();
      try
      {
         lock.lock();
         String catalogRepositoryURI = getEnvVarOrSysProp(CATALOG_GIT_REPOSITORY_PROPERTY_NAME,
                  DEFAULT_GIT_REPOSITORY_URL);
         String catalogRef = getEnvVarOrSysProp(CATALOG_GIT_REF_PROPERTY_NAME, DEFAULT_GIT_REF);
         logger.log(Level.INFO, "Indexing contents from {0} using {1} ref",
                  new Object[] { catalogRepositoryURI, catalogRef });
         if (catalogPath == null)
         {
            catalogPath = Files.createTempDirectory("booster-catalog");

            logger.info(() -> "Created " + catalogPath);
            // Clone repository
            Git.cloneRepository()
                     .setURI(catalogRepositoryURI)
                     .setBranch(catalogRef)
                     .setDirectory(catalogPath.toFile())
                     .call().close();
         }
         else
         {
            logger.info(() -> "Pulling changes to" + catalogPath);
            // Perform a git pull
            try (Git git = Git.open(catalogPath.toFile()))
            {
               git.pull().setRebase(true).call();
            }
            // Git pull on the existing repositories
            Path moduleRoot = catalogPath.resolve(MODULES_DIR);
            if (Files.isDirectory(moduleRoot))
            {
               try (DirectoryStream<Path> repositories = Files.newDirectoryStream(moduleRoot, Files::isDirectory))
               {
                  for (Path repository : repositories)
                  {
                     try
                     {
                        try (Git git = Git.open(repository.toFile()))
                        {
                           logger.info(() -> "Pulling changes to" + repository);
                           git.pull().setRebase(true).call();
                        }
                     }
                     catch (GitAPIException e)
                     {
                        logger.log(Level.SEVERE, "Error while performing Git operation", e);
                     }
                  }
               }
            }
         }
         final Path moduleRoot = catalogPath.resolve(MODULES_DIR);
         final List<Booster> quickstarts = new ArrayList<>();
         // Read the YAML files
         Files.walkFileTree(catalogPath, new SimpleFileVisitor<Path>()
         {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException
            {
               File ioFile = file.toFile();
               if (ioFile.getName().endsWith(".yaml") || ioFile.getName().endsWith(".yml"))
               {
                  String id = removeFileExtension(ioFile.getName());
                  Path modulePath = catalogPath.resolve(MODULES_DIR + File.separator + id);
                  indexBooster(id, file, modulePath).ifPresent(quickstarts::add);
               }
               return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException
            {
               return dir.startsWith(moduleRoot) ? FileVisitResult.SKIP_SUBTREE : FileVisitResult.CONTINUE;
            }
         });
         quickstarts.sort(Comparator.comparing(Booster::getName));
         this.boosters = Collections.unmodifiableList(quickstarts);
      }
      catch (GitAPIException e)
      {
         logger.log(Level.SEVERE, "Error while performing Git operation", e);
      }
      catch (Exception e)
      {
         logger.log(Level.SEVERE, "Error while indexing", e);
      }
      finally
      {
         logger.info(() -> "Finished content indexing");
         lock.unlock();
      }
   }

   /**
    * Takes a YAML file from the repository and indexes it
    * 
    * @param file A YAML file from the quickstart-catalog repository
    * @return an {@link Optional} containing a {@link Booster}
    */
   @SuppressWarnings("unchecked")
   private Optional<Booster> indexBooster(String id, Path file, Path moduleDir)
   {
      final Yaml yaml = new Yaml();
      logger.info(() -> "Indexing " + file + " ...");

      Booster booster = null;
      try (BufferedReader reader = Files.newBufferedReader(file))
      {
         // Read YAML entry
         booster = yaml.loadAs(reader, Booster.class);
         // Booster ID = filename without extension
         booster.setId(id);
         // Module does not exist. Clone it
         if (Files.notExists(moduleDir))
         {
            Git.cloneRepository()
                     .setDirectory(moduleDir.toFile())
                     .setURI(GITHUB_URL + booster.getGithubRepo())
                     .setBranch(booster.getGitRef())
                     .call().close();
         }
         Path metadataPath = moduleDir.resolve(booster.getBoosterDescriptorPath());
         try (BufferedReader metadataReader = Files.newBufferedReader(metadataPath))
         {
            Map<String, Object> metadata = yaml.loadAs(metadataReader, Map.class);
            booster.setMetadata(metadata);
         }

         Path descriptionPath = moduleDir.resolve(booster.getBoosterDescriptionPath());
         if (Files.exists(descriptionPath))
         {
            byte[] descriptionContent = Files.readAllBytes(descriptionPath);
            booster.setDescription(new String(descriptionContent));
         }
      }
      catch (GitAPIException gitException)
      {
         logger.log(Level.SEVERE, "Error while reading git repository", gitException);
      }
      catch (Exception e)
      {
         logger.log(Level.SEVERE, "Error while reading metadata from " + file, e);
      }
      return Optional.ofNullable(booster);
   }

   @PostConstruct
   void init()
   {
      long indexPeriod = Long.parseLong(getEnvVarOrSysProp(CATALOG_INDEX_PERIOD_PROPERTY_NAME, DEFAULT_INDEX_PERIOD));
      if (indexPeriod > 0L)
      {
         executorService = Executors.newScheduledThreadPool(1);
         logger.info(() -> "Indexing every " + indexPeriod + " minutes");
         executorService.scheduleAtFixedRate(this::index, 0, indexPeriod, TimeUnit.MINUTES);
      }
      else
      {
         index();
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
         logger.info(() -> "Removing " + catalogPath);
         try
         {
            // Remove all the YAML files
            deleteRecursively(catalogPath);
         }
         catch (IOException e)
         {
            logger.log(Level.FINEST, "Error while deleting catalog path", e);
         }
      }
   }

   private static String getEnvVarOrSysProp(String name, String defaultValue)
   {
      return System.getProperty(name, System.getenv().getOrDefault(name, defaultValue));
   }

   public List<Booster> getBoosters()
   {
      Lock readLock = reentrantLock.readLock();
      try
      {
         readLock.lock();
         return boosters;
      }
      finally
      {
         readLock.unlock();
      }
   }

   /**
    * Copies the {@link Booster} contents to the specified {@link Project}
    */
   public Path copy(Booster booster, Project project) throws IOException
   {
      Lock readLock = reentrantLock.readLock();
      try
      {
         readLock.lock();
         Path modulePath = catalogPath.resolve(MODULES_DIR + File.separator + booster.getId());
         Path to = project.getRoot().as(DirectoryResource.class).getUnderlyingResourceObject().toPath();
         return Files.walkFileTree(modulePath,
                  new CopyFileVisitor(to,
                           (p) -> !EXCLUDED_PROJECT_FILES.contains(p.toFile().getName().toLowerCase())));
      }
      finally
      {
         readLock.unlock();
      }
   }
}
