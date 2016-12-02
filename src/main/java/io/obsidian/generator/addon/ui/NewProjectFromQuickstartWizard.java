/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package io.obsidian.generator.addon.ui;

import java.io.File;
import java.nio.file.Files;
import java.util.List;

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
import org.jboss.forge.addon.parser.java.utils.Packages;
import org.jboss.forge.addon.resource.FileResource;
import org.jboss.forge.addon.ui.command.UICommand;
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

/**
 * The project type for
 * 
 * @author <a href="mailto:ggastald@redhat.com">George Gastaldi</a>
 */
public class NewProjectFromQuickstartWizard implements UICommand
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
   @WithAttributes(label = "Project version", required = true, defaultValue = "1.0.0-SNAPSHOT")
   private UIInput<String> version;

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
      List<Archetype> archetypes = registry.getArchetypeCatalogFactory("Quickstarts").getArchetypeCatalog()
               .getArchetypes();
      type.setValueChoices(archetypes);
      if (!archetypes.isEmpty())
      {
         type.setDefaultValue(archetypes.get(0));
      }
      builder.add(type).add(named).add(topLevelPackage).add(version);
   }

   @Override
   public UICommandMetadata getMetadata(UIContext context)
   {
      return Metadata.forCommand(getClass()).name("Obsidian: New Quickstart")
               .description("Generate your project from a quickstart")
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
      // TODO: Using JBoss nexus repository for now
      depQuery.setRepositories(new DependencyRepository("archetypes",
               "https://repository.jboss.org/nexus/content/repositories/snapshots"));
      Dependency resolvedArtifact = dependencyResolver.resolveArtifact(depQuery);
      FileResource<?> artifact = resolvedArtifact.getArtifact();
      File tmpDir = Files.createTempDirectory("projectdir").toFile();
      ArchetypeHelper archetypeHelper = new ArchetypeHelper(artifact.getResourceInputStream(), tmpDir,
               topLevelPackage.getValue(), named.getValue(),
               version.getValue());
      archetypeHelper.setPackageName(Packages.toValidPackageName(topLevelPackage.getValue()) + "."
               + Packages.toValidPackageName(named.getValue()));
      archetypeHelper.execute();

      context.getUIContext().setSelection(tmpDir);

      return Results.success("Project created in " + tmpDir);
   }

}
