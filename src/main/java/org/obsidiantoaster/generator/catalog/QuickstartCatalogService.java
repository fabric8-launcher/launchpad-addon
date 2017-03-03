/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.obsidiantoaster.generator.catalog;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.WebTarget;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.TextProgressMonitor;
import org.jboss.forge.addon.rest.ClientFactory;
import org.obsidiantoaster.generator.catalog.model.Quickstart;
import org.obsidiantoaster.generator.catalog.model.QuickstartMetadata;
import org.yaml.snakeyaml.Yaml;

/**
 * The {@link QuickstartCatalogService} reads from the Quickstart catalog populates {@link Quickstart} objects
 * 
 * @author <a href="mailto:ggastald@redhat.com">George Gastaldi</a>
 */
@Singleton
public class QuickstartCatalogService
{
   private static final String QUICKSTART_METADATA_URL_TEMPLATE = "https://raw.githubusercontent.com/{githubRepo}/{githubRef}/{obsidianDescriptorPath}";

   private static final String GIT_REPOSITORY = "https://github.com/obsidian-toaster/quickstart-catalog.git";

   private Logger logger = Logger.getLogger(getClass().getName());

   private Path catalogPath;

   private List<Quickstart> quickstarts = new ArrayList<>();

   @Inject
   private ClientFactory clientFactory;

   /**
    * Find a {@link Quickstart} by its id
    */
   public Optional<Quickstart> getQuickstart(String id)
   {
      return quickstarts.stream().filter(q -> id.equals(q.getId())).findAny();
   }

   public void index() throws IOException
   {
      try
      {
         if (catalogPath == null)
         {
            catalogPath = Files.createTempDirectory("quickstart-catalog");
            logger.info("Created " + catalogPath);
            // Clone repository here
            Git.cloneRepository().setProgressMonitor(new TextProgressMonitor()).setURI(GIT_REPOSITORY)
                     .setDirectory(catalogPath.toFile()).call().close();
         }
         else
         {
            logger.info("Pulling changes to" + catalogPath);
            // Perform a git pull
            try (Git git = Git.open(catalogPath.toFile()))
            {
               git.pull().setProgressMonitor(new TextProgressMonitor()).setRebase(true).call();
            }
         }
         final List<Quickstart> quickstarts = new ArrayList<>();
         final Yaml yaml = new Yaml();
         Client client = clientFactory.createClient();
         WebTarget target = client.target(QUICKSTART_METADATA_URL_TEMPLATE);
         // Read the YAML files
         Files.walkFileTree(catalogPath, new SimpleFileVisitor<Path>()
         {
            @SuppressWarnings("unchecked")
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException
            {
               if (file.toString().endsWith(".yaml"))
               {
                  logger.info("Reading " + file + " ...");
                  try (BufferedReader reader = Files.newBufferedReader(file))
                  {
                     // Read YAML entry
                     Quickstart quickstart = yaml.loadAs(reader, Quickstart.class);
                     quickstart.setId(org.obsidiantoaster.generator.Files.removeFileExtension(file.toFile().getName()));
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
         this.quickstarts = quickstarts;
      }
      catch (GitAPIException cause)
      {
         throw new IOException("Error while performing GIT operation", cause);
      }
   }

   @PostConstruct
   void init()
   {
      try
      {
         index();
      }
      catch (IOException e)
      {
         e.printStackTrace();
      }
   }

   @PreDestroy
   void destroy()
   {
      if (catalogPath != null)
      {
         logger.info("Removing " + catalogPath);
         // Remove all the YAML files
         try
         {
            org.obsidiantoaster.generator.Files.deleteRecursively(catalogPath);
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
      List<Quickstart> result = new ArrayList<>();
      result.addAll(quickstarts);
      return result;
   }
}
