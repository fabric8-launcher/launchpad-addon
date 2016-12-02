/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package io.obsidian.generator.addon.ui;

import java.util.List;

import javax.inject.Inject;

import org.jboss.forge.addon.facets.FacetFactory;
import org.jboss.forge.addon.maven.projects.MavenBuildSystem;
import org.jboss.forge.addon.parser.java.utils.Packages;
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

/**
 * The project type for
 * 
 * @author <a href="mailto:ggastald@redhat.com">George Gastaldi</a>
 */
public class NewProjectGeneratorWizard implements UIWizard
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
   @WithAttributes(label = "Project version", required = true, defaultValue = "1.0.0-SNAPSHOT")
   private UIInput<String> version;

   @Inject
   private MavenBuildSystem mavenBuildSystem;

   @Inject
   private List<ProjectType> supportedTypes;

   @Inject
   private ProjectFactory projectFactory;

   @Inject
   private FacetFactory facetFactory;

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
      type.setValueChoices(supportedTypes);
      if (!supportedTypes.isEmpty())
      {
         type.setDefaultValue(supportedTypes.get(0));
      }
      builder.add(type).add(named).add(topLevelPackage).add(version);
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
      Project project = projectFactory.createTempProject(mavenBuildSystem);
      MetadataFacet metadataFacet = project.getFacet(MetadataFacet.class);
      metadataFacet.setProjectName(named.getValue());
      metadataFacet.setProjectVersion(version.getValue());
      metadataFacet.setProjectGroupName(Packages.toValidPackageName(topLevelPackage.getValue()));
      // Install the required facets
      Iterable<Class<? extends ProjectFacet>> requiredFacets = type.getValue().getRequiredFacets();
      if (requiredFacets != null)
      {
         for (Class<? extends ProjectFacet> facet : requiredFacets)
         {
            Class<? extends ProjectFacet> buildSystemFacet = mavenBuildSystem.resolveProjectFacet(facet);
            if (!project.hasFacet(buildSystemFacet))
            {
               facetFactory.install(project, buildSystemFacet);
            }
         }
      }
      UIContext uiContext = context.getUIContext();
      uiContext.setSelection(project.getRoot());
      uiContext.getAttributeMap().put(Project.class, project);

      return Results.success();
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

}
