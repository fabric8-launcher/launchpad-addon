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
package io.openshift.launchpad.ui.quickstart;

import java.util.Iterator;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.jboss.forge.addon.maven.projects.MavenBuildSystem;
import org.jboss.forge.addon.maven.resources.MavenModelResource;
import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.projects.ProjectFactory;
import org.jboss.forge.addon.resource.DirectoryResource;
import org.jboss.forge.addon.ui.context.UIBuilder;
import org.jboss.forge.addon.ui.context.UIContext;
import org.jboss.forge.addon.ui.context.UIExecutionContext;
import org.jboss.forge.addon.ui.context.UINavigationContext;
import org.jboss.forge.addon.ui.context.UIValidationContext;
import org.jboss.forge.addon.ui.input.UISelectOne;
import org.jboss.forge.addon.ui.metadata.UICommandMetadata;
import org.jboss.forge.addon.ui.metadata.WithAttributes;
import org.jboss.forge.addon.ui.result.NavigationResult;
import org.jboss.forge.addon.ui.result.Result;
import org.jboss.forge.addon.ui.result.Results;
import org.jboss.forge.addon.ui.util.Categories;
import org.jboss.forge.addon.ui.util.Metadata;
import org.jboss.forge.addon.ui.wizard.UIWizard;

import io.openshift.launchpad.catalog.Booster;
import io.openshift.launchpad.catalog.BoosterCatalogService;
import io.openshift.launchpad.catalog.Mission;
import io.openshift.launchpad.catalog.Runtime;
import io.openshift.launchpad.ui.input.ProjectName;
import io.openshift.launchpad.ui.input.TopLevelPackage;
import io.openshift.launchpad.ui.input.Version;

/**
 * Creates a new project
 * 
 * @author <a href="mailto:ggastald@redhat.com">George Gastaldi</a>
 */
public class NewProjectWizard implements UIWizard
{
   @Inject
   @WithAttributes(label = "Mission", required = true)
   private UISelectOne<Mission> mission;

   @Inject
   @WithAttributes(label = "Runtime", required = true)
   private UISelectOne<Runtime> runtime;

   @Inject
   private ProjectName named;

   @Inject
   private TopLevelPackage topLevelPackage;

   @Inject
   private Version version;

   @Inject
   private BoosterCatalogService catalogService;

   @Inject
   private ProjectFactory projectFactory;

   @Inject
   private MavenBuildSystem mavenBuildSystem;

   @Override
   public void initializeUI(UIBuilder builder) throws Exception
   {
      if (builder.getUIContext().getProvider().isGUI())
      {
         mission.setItemLabelConverter(Mission::getName);
         runtime.setItemLabelConverter(Runtime::getName);
      }
      else
      {
         mission.setItemLabelConverter(Mission::getId);
         runtime.setItemLabelConverter(Runtime::getId);
      }
      mission.setValueChoices(catalogService.getMissions());
      mission.setDefaultValue(() -> {
         Iterator<Mission> iterator = mission.getValueChoices().iterator();
         if (iterator.hasNext())
         {
            return iterator.next();
         }
         else
         {
            return null;
         }
      });

      runtime.setValueChoices(() -> catalogService.getRuntimes(mission.getValue()));
      runtime.setDefaultValue(() -> {
         Iterator<Runtime> iterator = runtime.getValueChoices().iterator();
         if (iterator.hasNext())
         {
            return iterator.next();
         }
         else
         {
            return null;
         }
      });
      builder.add(mission).add(runtime).add(named).add(topLevelPackage).add(version);
   }

   @Override
   public UICommandMetadata getMetadata(UIContext context)
   {
      return Metadata.forCommand(getClass()).name("Launchpad: New Project")
               .description("Generate your project from a booster")
               .category(Categories.create("Openshift.io"));
   }

   @Override
   public void validate(UIValidationContext context)
   {
      Optional<Booster> booster = catalogService.getBooster(mission.getValue(), runtime.getValue());
      if (!booster.isPresent())
      {
         context.addValidationError(mission,
                  "No booster found for mission '" + mission.getValue() + "' and runtime '" + runtime.getValue() + "'");
      }
   }

   @Override
   public NavigationResult next(UINavigationContext context) throws Exception
   {
      Map<Object, Object> attributeMap = context.getUIContext().getAttributeMap();
      attributeMap.put("name", named.getValue());
      if (mission.hasValue() && runtime.hasValue())
      {
         catalogService.getBooster(mission.getValue(), runtime.getValue())
                  .ifPresent(b -> attributeMap.put("booster", b));
      }
      return null;
   }

   @Override
   public Result execute(UIExecutionContext context) throws Exception
   {
      Booster booster = catalogService.getBooster(mission.getValue(), runtime.getValue()).get();
      DirectoryResource initialDir = (DirectoryResource) context.getUIContext().getInitialSelection().get();
      DirectoryResource projectDirectory = initialDir.getChildDirectory(named.getValue());
      // Using ProjectFactory to invoke bound listeners
      Project project = projectFactory.createProject(projectDirectory, mavenBuildSystem);
      // Do not cache anything
      projectFactory.invalidateCaches();
      MavenModelResource modelResource = projectDirectory.getChildOfType(MavenModelResource.class, "pom.xml");
      // Delete existing pom
      modelResource.delete();
      // Copy contents (including pom.xml if exists)
      catalogService.copy(booster, project);
      // Perform model changes
      if (modelResource.exists())
      {
         Model model = modelResource.getCurrentModel();
         model.setGroupId(topLevelPackage.getValue());
         model.setArtifactId(named.getValue());
         model.setVersion(version.getValue());

         // Change child modules
         for (String module : model.getModules())
         {
            DirectoryResource moduleDirResource = projectDirectory.getChildDirectory(module);
            MavenModelResource moduleModelResource = moduleDirResource.getChildOfType(MavenModelResource.class,
                     "pom.xml");
            Model moduleModel = moduleModelResource.getCurrentModel();
            Parent parent = moduleModel.getParent();
            if (parent != null)
            {
               parent.setGroupId(model.getGroupId());
               parent.setArtifactId(model.getArtifactId());
               parent.setVersion(model.getVersion());
               moduleModelResource.setCurrentModel(moduleModel);
            }
         }
         // TODO: Change package name
         modelResource.setCurrentModel(model);
      }
      context.getUIContext().setSelection(projectDirectory);
      return Results.success();
   }
}
