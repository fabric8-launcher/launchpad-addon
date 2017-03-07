/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.obsidiantoaster.generator.catalog;

import static org.obsidiantoaster.generator.Files.deleteRecursively;
import static org.obsidiantoaster.generator.Files.removeFileExtension;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.WebTarget;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.jboss.forge.addon.rest.ClientFactory;
import org.obsidiantoaster.generator.catalog.model.Quickstart;
import org.obsidiantoaster.generator.catalog.model.QuickstartMetadata;
import org.yaml.snakeyaml.Yaml;

/**
 * This service reads from the Quickstart catalog Github repository in
 * https://github.com/obsidian-toaster/quickstart-catalog and marshalls into {@link Quickstart} objects.
 * 
 * @author <a href="mailto:ggastald@redhat.com">George Gastaldi</a>
 */
@Singleton
public class QuickstartCatalogService
{
   private static final String QUICKSTART_METADATA_URL_TEMPLATE = "https://raw.githubusercontent.com/{githubRepo}/{githubRef}/{obsidianDescriptorPath}";
   private static final String GIT_REPOSITORY = "https://github.com/obsidian-toaster/quickstart-catalog.git";
   private static final Logger logger = Logger.getLogger(QuickstartCatalogService.class.getName());

   private Path catalogPath;
   private List<Quickstart> quickstarts = new ArrayList<>();

   private ScheduledExecutorService executorService;
   private ReentrantReadWriteLock reentrantLock = new ReentrantReadWriteLock();

   @Inject
   private ClientFactory clientFactory;

   /**
    * Clones the catalog git repository and reads the obsidian metadata on each quickstart repository
    */
   void index() throws IOException
   {
      Client client = clientFactory.createClient();
      WriteLock lock = reentrantLock.writeLock();
      try
      {
         lock.lock();
         if (catalogPath == null)
         {
            catalogPath = Files.createTempDirectory("quickstart-catalog");
            logger.info("Created " + catalogPath);
            // Clone repository here
            Git.cloneRepository().setURI(GIT_REPOSITORY).setDirectory(catalogPath.toFile()).call().close();
         }
         else
         {
            logger.info("Pulling changes to" + catalogPath);
            // Perform a git pull
            try (Git git = Git.open(catalogPath.toFile()))
            {
               git.pull().setRebase(true).call();
            }
         }
         final List<Quickstart> quickstarts = new ArrayList<>();
         final Yaml yaml = new Yaml();
         WebTarget target = client.target(QUICKSTART_METADATA_URL_TEMPLATE);
         // Read the YAML files
         Files.walkFileTree(catalogPath, new SimpleFileVisitor<Path>()
         {
            @SuppressWarnings("unchecked")
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException
            {
               if (file.toString().endsWith(".yaml") || file.toString().endsWith(".yml"))
               {
                  logger.info("Reading " + file + " ...");
                  try (BufferedReader reader = Files.newBufferedReader(file))
                  {
                     // Read YAML entry
                     Quickstart quickstart = yaml.loadAs(reader, Quickstart.class);
                     // Quickstart ID = filename without extension
                     quickstart.setId(removeFileExtension(file.toFile().getName()));
                     Map<String, Object> templateValues = new HashMap<>();
                     templateValues.put("githubRepo", quickstart.getGithubRepo());
                     templateValues.put("githubRef", quickstart.getGitRef());
                     templateValues.put("obsidianDescriptorPath", quickstart.getObsidianDescriptorPath());

                     String response = target.resolveTemplates(templateValues).request().get(String.class);
                     Map<String, String> values = yaml.loadAs(response, Map.class);
                     QuickstartMetadata qm = new QuickstartMetadata();
                     qm.setName(values.get("name"));
                     qm.setDescription(values.get("description"));
                     quickstart.setMetadata(qm);

                     quickstarts.add(quickstart);
                  }
               }
               return FileVisitResult.CONTINUE;
            }
         });
         Collections.sort(quickstarts, (l, r) -> l.getName().compareTo(r.getName()));
         this.quickstarts = quickstarts;
      }
      catch (GitAPIException cause)
      {
         throw new IOException("Error while performing GIT operation", cause);
      }
      finally
      {
         lock.unlock();
         client.close();
      }
   }

   @PostConstruct
   void init()
   {
      executorService = Executors.newScheduledThreadPool(1);
      long indexPeriod = Long
               .parseLong(System.getProperty("INDEX_PERIOD", System.getenv().getOrDefault("INDEX_PERIOD", "30")));
      logger.info("Indexing every " + indexPeriod + " minutes");
      executorService.scheduleAtFixedRate(() -> {
         try
         {
            logger.info("Indexing contents ...");
            index();
            logger.info("Finished content indexing");
         }
         catch (IOException e)
         {
            e.printStackTrace();
         }
      }, 0, indexPeriod, TimeUnit.MINUTES);
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
         // Remove all the YAML files
         try
         {
            deleteRecursively(catalogPath);
         }
         catch (IOException ignored)
         {
         }
      }
   }

   /**
    * @return a copy of the indexed {@link QuickstartMetadata} files
    */
   public List<Quickstart> getQuickstarts()
   {
      Lock readLock = reentrantLock.readLock();
      try
      {
         readLock.lock();
         return Collections.unmodifiableList(quickstarts);
      }
      finally
      {
         readLock.unlock();
      }
   }
}
