/**
 * Copyright 2005-2015 Red Hat, Inc.
 *
 * Red Hat licenses this file to you under the Apache License, version
 * 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */
package org.obsidiantoaster.generator.addon.ui;

import java.util.List;
import java.util.Map;

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
import org.jboss.forge.addon.ui.result.navigation.NavigationResultBuilder;
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
      ProjectType projectType = type.getValue();
      Iterable<Class<? extends ProjectFacet>> requiredFacets = projectType.getRequiredFacets();
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
      Map<Object, Object> attributeMap = uiContext.getAttributeMap();
      attributeMap.put(Project.class, project);
      attributeMap.put("chosenProjectType", projectType.toString());

      return Results.success();
   }

   @Override
   public NavigationResult next(UINavigationContext context) throws Exception
   {
      NavigationResultBuilder builder = NavigationResultBuilder.create();
      ProjectType nextStep = type.getValue();
      if (nextStep != null)
      {
         builder.add(nextStep.next(context));
      }
      builder.add(InstallFabric8PluginStep.class);
      builder.add(AddMissingFilesStep.class);
      return builder.build();
   }

}
