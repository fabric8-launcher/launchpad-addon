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

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

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
   /**
    * Files to be deleted after project creation (if exists)
    */
   private static final List<String> FILES_TO_BE_DELETED = Arrays.asList(".git", ".travis", ".travis.yml", ".ds_store",
            ".openshiftio");

   @Inject
   @WithAttributes(label = "Booster", required = true)
   private UISelectOne<Booster> type;

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
      UIContext uiContext = builder.getUIContext();

      if (uiContext.getProvider().isGUI())
      {
         type.setItemLabelConverter(Booster::getName);
      }
      else
      {
         type.setItemLabelConverter(Booster::getId);
      }
      List<Booster> quickstarts = catalogService.getBoosters();
      type.setValueChoices(quickstarts);
      if (!quickstarts.isEmpty())
      {
         type.setDefaultValue(quickstarts.get(0));
      }
      Callable<String> description = () -> type.getValue() != null ? type.getValue().getDescription() : null;
      type.setDescription(description).setNote(description);
      builder.add(type).add(named).add(topLevelPackage).add(version);
   }

   @Override
   public UICommandMetadata getMetadata(UIContext context)
   {
      return Metadata.forCommand(getClass()).name("Launchpad: New Project")
               .description("Generate your project from a booster")
               .category(Categories.create("Openshift.io"));
   }

   @Override
   public NavigationResult next(UINavigationContext context) throws Exception
   {
      Map<Object, Object> attributeMap = context.getUIContext().getAttributeMap();
      attributeMap.put("name", named.getValue());
      attributeMap.put("type", type.getValue());
      return null;
   }

   @Override
   public Result execute(UIExecutionContext context) throws Exception
   {
      Booster qs = type.getValue();
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
      catalogService.copy(qs, project, (p) -> !FILES_TO_BE_DELETED.contains(p.toFile().getName().toLowerCase()));
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
