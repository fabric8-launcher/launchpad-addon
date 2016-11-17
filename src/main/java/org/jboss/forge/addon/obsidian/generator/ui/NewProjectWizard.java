/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.jboss.forge.addon.obsidian.generator.ui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import org.jboss.forge.addon.facets.FacetFactory;
import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.projects.ProjectFacet;
import org.jboss.forge.addon.projects.ProjectFactory;
import org.jboss.forge.addon.projects.ProjectProvider;
import org.jboss.forge.addon.projects.ProjectType;
import org.jboss.forge.addon.projects.ProvidedProjectFacet;
import org.jboss.forge.addon.projects.facets.MetadataFacet;
import org.jboss.forge.addon.ui.context.UIBuilder;
import org.jboss.forge.addon.ui.context.UIContext;
import org.jboss.forge.addon.ui.context.UIExecutionContext;
import org.jboss.forge.addon.ui.context.UINavigationContext;
import org.jboss.forge.addon.ui.input.UIInput;
import org.jboss.forge.addon.ui.input.UISelectOne;
import org.jboss.forge.addon.ui.metadata.UICommandMetadata;
import org.jboss.forge.addon.ui.metadata.WithAttributes;
import org.jboss.forge.addon.ui.result.NavigationResult;
import org.jboss.forge.addon.ui.result.Result;
import org.jboss.forge.addon.ui.result.Results;
import org.jboss.forge.addon.ui.util.Categories;
import org.jboss.forge.addon.ui.util.Metadata;
import org.jboss.forge.addon.ui.wizard.UIWizard;
import org.jboss.forge.furnace.services.Imported;

/**
 *
 * @author <a href="mailto:ggastald@redhat.com">George Gastaldi</a>
 */
public class NewProjectWizard implements UIWizard
{
   @Inject
   @WithAttributes(label = "Project name", required = true, defaultValue = "demo")
   private UIInput<String> named;

   @Inject
   @WithAttributes(label = "Top level package", defaultValue = "com.example")
   private UIInput<String> topLevelPackage;

   @Inject
   @WithAttributes(label = "Project type", required = true)
   private UISelectOne<ProjectType> type;

   @Inject
   @WithAttributes(label = "Build system")
   private UISelectOne<ProjectProvider> buildSystem;

   @Inject
   private FacetFactory facetFactory;

   @Inject
   private ProjectFactory projectFactory;

   @Inject
   private Imported<ProjectProvider> projectProviders;

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
         type.setItemLabelConverter(ProjectType::getType);
      }

      // Add Project types
      List<ProjectType> projectTypes = new ArrayList<>();
      for (ProjectType projectType : type.getValueChoices())
      {
         for (ProjectProvider buildSystem : projectProviders)
         {
            if (projectType.isEnabled(uiContext) && isProjectTypeBuildable(projectType, buildSystem))
            {
               projectTypes.add(projectType);
               break;
            }
         }
      }
      Collections.sort(projectTypes, (left, right) -> left.priority() - right.priority());
      if (!projectTypes.isEmpty())
      {
         type.setDefaultValue(projectTypes.get(0));
      }
      type.setValueChoices(projectTypes);

      buildSystem.setItemLabelConverter(ProjectProvider::getType);
      buildSystem.setDefaultValue(() -> {
         Iterator<ProjectProvider> iterator = buildSystem.getValueChoices().iterator();
         if (iterator.hasNext())
         {
            return iterator.next();
         }
         return null;
      });
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

      builder.add(type).add(buildSystem).add(named).add(topLevelPackage);
   }

   private boolean isProjectTypeBuildable(ProjectType type, ProjectProvider buildSystem)
   {
      boolean result = false;
      Iterable<Class<? extends ProvidedProjectFacet>> requiredFacets = getRequiredBuildSystemFacets(type);
      if (requiredFacets == null || !requiredFacets.iterator().hasNext())
      {
         result = true;
      }
      else
      {
         for (Class<? extends ProvidedProjectFacet> required : requiredFacets)
         {
            result = false;
            for (Class<? extends ProvidedProjectFacet> provided : buildSystem.getProvidedFacetTypes())
            {
               if (provided.isAssignableFrom(required))
                  result = true;
            }
            if (!result)
               break;
         }
      }
      return result;
   }

   @SuppressWarnings("unchecked")
   private Iterable<Class<? extends ProvidedProjectFacet>> getRequiredBuildSystemFacets(ProjectType type)
   {
      Set<Class<? extends ProvidedProjectFacet>> result = new HashSet<>();
      Iterable<Class<? extends ProjectFacet>> requiredFacets = type.getRequiredFacets();
      if (requiredFacets != null)
      {
         for (Class<? extends ProjectFacet> facetType : requiredFacets)
         {
            if (ProvidedProjectFacet.class.isAssignableFrom(facetType))
            {
               result.add((Class<? extends ProvidedProjectFacet>) facetType);
            }
         }
      }
      return result;
   }

   @Override
   public UICommandMetadata getMetadata(UIContext context)
   {
      return Metadata.forCommand(getClass()).name("Obsidian: New Project")
               .description("Generate your project")
               .category(Categories.create("Obsidian"));
   }

   @Override
   public NavigationResult next(UINavigationContext context) throws Exception
   {
      ProjectType nextStep = type.getValue();
      if (nextStep != null)
      {
         return nextStep.next(context);
      }
      else
      {
         return null;
      }
   }

   @Override
   public Result execute(UIExecutionContext context) throws Exception
   {
      ProjectType value = type.getValue();
      ProjectProvider buildSystemValue = buildSystem.getValue();
      Project project = projectFactory.createTempProject(buildSystemValue);
      UIContext uiContext = context.getUIContext();
      MetadataFacet metadataFacet = project.getFacet(MetadataFacet.class);
      metadataFacet.setProjectName(named.getValue());
      metadataFacet.setProjectVersion("1.0.0-SNAPSHOT");
      metadataFacet.setProjectGroupName(topLevelPackage.getValue());
      // Install the required facets
      if (value != null)
      {
         Iterable<Class<? extends ProjectFacet>> requiredFacets = value.getRequiredFacets();
         if (requiredFacets != null)
         {
            for (Class<? extends ProjectFacet> facet : requiredFacets)
            {
               Class<? extends ProjectFacet> buildSystemFacet = buildSystemValue.resolveProjectFacet(facet);
               if (!project.hasFacet(buildSystemFacet))
               {
                  facetFactory.install(project, buildSystemFacet);
               }
            }
         }
      }
      uiContext.setSelection(project.getRoot());
      uiContext.getAttributeMap().put(Project.class, project);

      return Results.success(project.getRoot().getFullyQualifiedName());
   }

}
