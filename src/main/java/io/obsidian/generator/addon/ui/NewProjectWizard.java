/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package io.obsidian.generator.addon.ui;

import java.util.Arrays;

import javax.inject.Inject;

import org.jboss.forge.addon.facets.FacetFactory;
import org.jboss.forge.addon.maven.projects.MavenBuildSystem;
import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.projects.ProjectFacet;
import org.jboss.forge.addon.projects.ProjectFactory;
import org.jboss.forge.addon.projects.ProjectType;
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

import io.fabric8.forge.devops.springboot.SpringBootProjectType;
import io.vertx.forge.project.VertxProjectType;

/**
 * The project type for
 * 
 * @author <a href="mailto:ggastald@redhat.com">George Gastaldi</a>
 */
public class NewProjectWizard implements UIWizard
{
   @Inject
   @WithAttributes(label = "Project type", required = true)
   private UISelectOne<ProjectType> type;

   @Inject
   @WithAttributes(label = "Project name", required = true, defaultValue = "demo")
   private UIInput<String> named;

   @Inject
   @WithAttributes(label = "Top level package", defaultValue = "com.example")
   private UIInput<String> topLevelPackage;

   @Inject
   private MavenBuildSystem buildSystem;

   @Inject
   private FacetFactory facetFactory;

   @Inject
   private ProjectFactory projectFactory;

   @Inject
   private SpringBootProjectType springBootProjectType;

   @Inject
   private VertxProjectType vertxProjectType;

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
      type.setDefaultValue(springBootProjectType).setValueChoices(getAllowedProjectTypes());

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

   private Iterable<ProjectType> getAllowedProjectTypes()
   {
      return Arrays.asList(springBootProjectType, vertxProjectType);
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
      Project project = projectFactory.createTempProject(buildSystem);
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
               Class<? extends ProjectFacet> buildSystemFacet = buildSystem.resolveProjectFacet(facet);
               if (!project.hasFacet(buildSystemFacet))
               {
                  facetFactory.install(project, buildSystemFacet);
               }
            }
         }
      }
      uiContext.setSelection(project.getRoot());
      uiContext.getAttributeMap().put(Project.class, project);

      return Results.success("Project created in " + project.getRoot().getFullyQualifiedName());
   }

}
