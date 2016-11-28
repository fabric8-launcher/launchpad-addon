/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package io.obsidian.generator.addon.ui;

import java.io.File;
import java.nio.file.Files;

import javax.inject.Inject;

import org.apache.maven.archetype.catalog.Archetype;
import org.jboss.forge.addon.dependencies.Coordinate;
import org.jboss.forge.addon.dependencies.Dependency;
import org.jboss.forge.addon.dependencies.DependencyRepository;
import org.jboss.forge.addon.dependencies.DependencyResolver;
import org.jboss.forge.addon.dependencies.builder.CoordinateBuilder;
import org.jboss.forge.addon.dependencies.builder.DependencyQueryBuilder;
import org.jboss.forge.addon.maven.archetype.ArchetypeCatalogFactoryRegistry;
import org.jboss.forge.addon.maven.projects.archetype.ArchetypeHelper;
import org.jboss.forge.addon.resource.FileResource;
import org.jboss.forge.addon.resource.ResourceFactory;
import org.jboss.forge.addon.ui.context.UIBuilder;
import org.jboss.forge.addon.ui.context.UIContext;
import org.jboss.forge.addon.ui.context.UIExecutionContext;
import org.jboss.forge.addon.ui.input.UIInput;
import org.jboss.forge.addon.ui.input.UISelectOne;
import org.jboss.forge.addon.ui.metadata.UICommandMetadata;
import org.jboss.forge.addon.ui.metadata.WithAttributes;
import org.jboss.forge.addon.ui.result.Result;
import org.jboss.forge.addon.ui.result.Results;
import org.jboss.forge.addon.ui.util.Categories;
import org.jboss.forge.addon.ui.util.Metadata;
import org.jboss.forge.addon.ui.wizard.UIWizard;
import org.jboss.forge.furnace.util.Strings;

/**
 * The project type for
 * 
 * @author <a href="mailto:ggastald@redhat.com">George Gastaldi</a>
 */
public class NewProjectWizard implements UIWizard
{
   @Inject
   @WithAttributes(label = "Project type", required = true)
   private UISelectOne<Archetype> type;

   @Inject
   @WithAttributes(label = "Project name", required = true, defaultValue = "demo")
   private UIInput<String> named;

   @Inject
   @WithAttributes(label = "Top level package", defaultValue = "com.example")
   private UIInput<String> topLevelPackage;

   @Inject
   private ResourceFactory resourceFactory;

   @Inject
   private ArchetypeCatalogFactoryRegistry registry;

   @Inject
   private DependencyResolver dependencyResolver;

   @Override
   public void initializeUI(UIBuilder builder) throws Exception
   {
      UIContext uiContext = builder.getUIContext();
      named.addValidator(context -> {
         if (named.getValue() != null && named.getValue().matches(".*[^-_.a-zA-Z0-9].*"))
            context.addValidationError(named,
                     "Project name must not contain spaces or special characters.");
      });

      if (uiContext.getProvider().isGUI())
      {
         type.setItemLabelConverter(Archetype::getDescription);
      }
      type.setValueChoices(registry.getArchetypeCatalogFactory("Quickstarts").getArchetypeCatalog().getArchetypes());
      topLevelPackage.setDefaultValue(() -> {
         String result = named.getValue();
         if (result != null)
         {
            result = ("org." + result).replaceAll("\\W+", ".");
            result = result.trim();
            result = result.replaceAll("^\\.", "");
            result = result.replaceAll("\\.$", "");
         }
         else
            result = "org.example";
         return result;
      });

      builder.add(type).add(named).add(topLevelPackage);
   }

   @Override
   public UICommandMetadata getMetadata(UIContext context)
   {
      return Metadata.forCommand(getClass()).name("Obsidian: New Project")
               .description("Generate your project")
               .category(Categories.create("Obsidian"));
   }

   @Override
   public Result execute(UIExecutionContext context) throws Exception
   {
      Archetype chosenArchetype = type.getValue();
      Coordinate coordinate = CoordinateBuilder.create().setGroupId(chosenArchetype.getGroupId())
               .setArtifactId(chosenArchetype.getArtifactId())
               .setVersion(chosenArchetype.getVersion());
      DependencyQueryBuilder depQuery = DependencyQueryBuilder.create(coordinate);
      String repository = chosenArchetype.getRepository();
      if (!Strings.isNullOrEmpty(repository))
      {
         if (repository.endsWith(".xml"))
         {
            int lastRepositoryPath = repository.lastIndexOf('/');
            if (lastRepositoryPath > -1)
               repository = repository.substring(0, lastRepositoryPath);
         }
         if (!repository.isEmpty())
         {
            depQuery.setRepositories(new DependencyRepository("archetype", repository));
         }
      }
      Dependency resolvedArtifact = dependencyResolver.resolveArtifact(depQuery);
      FileResource<?> artifact = resolvedArtifact.getArtifact();
      File tmpDir = Files.createTempDirectory("projectdir").toFile();
      ArchetypeHelper archetypeHelper = new ArchetypeHelper(artifact.getResourceInputStream(), tmpDir,
               topLevelPackage.getValue(), named.getValue(), "1.0.0-SNAPSHOT");
      archetypeHelper.setPackageName(topLevelPackage.getValue());
      archetypeHelper.execute();

      context.getUIContext().setSelection(resourceFactory.create(tmpDir));

      return Results.success("Project created in " + tmpDir);
   }

}
