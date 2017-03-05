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
package org.obsidiantoaster.generator.ui.quickstart;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import javax.inject.Inject;

import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.eclipse.jgit.api.Git;
import org.jboss.forge.addon.maven.resources.MavenModelResource;
import org.jboss.forge.addon.resource.ResourceFactory;
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
import org.obsidiantoaster.generator.catalog.QuickstartCatalogService;
import org.obsidiantoaster.generator.catalog.model.Quickstart;
import org.obsidiantoaster.generator.ui.input.ProjectName;
import org.obsidiantoaster.generator.ui.input.TopLevelPackage;

/**
 * The project type for
 * 
 * @author <a href="mailto:ggastald@redhat.com">George Gastaldi</a>
 */
public class NewProjectFromQuickstartWizard implements UIWizard
{
   /**
    * Files to be deleted after project creation (if exists)
    */
   private static final String[] FILES_TO_BE_DELETED = { ".git", ".travis", ".travis.yml" };

   @Inject
   @WithAttributes(label = "Project type", required = true)
   private UISelectOne<Quickstart> type;

   @Inject
   private ProjectName named;

   @Inject
   private TopLevelPackage topLevelPackage;

   @Inject
   @WithAttributes(label = "Project version", required = true, defaultValue = "1.0.0-SNAPSHOT")
   private UIInput<String> version;

   @Inject
   private ResourceFactory resourceFactory;

   @Inject
   private QuickstartCatalogService catalogService;

   @Override
   public void initializeUI(UIBuilder builder) throws Exception
   {
      UIContext uiContext = builder.getUIContext();

      if (uiContext.getProvider().isGUI())
      {
         type.setItemLabelConverter(Quickstart::getName);
      }
      else
      {
         type.setItemLabelConverter(Quickstart::getId);
      }
      List<Quickstart> quickstarts = catalogService.getQuickstarts();
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
      return Metadata.forCommand(getClass()).name("Obsidian: New Quickstart")
               .description("Generate your project from a quickstart")
               .category(Categories.create("Obsidian"));
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
      Quickstart qs = type.getValue();
      File projectDir = Files.createTempDirectory("projectdir").toFile();
      // Clone Git repository
      Git.cloneRepository()
               .setDirectory(projectDir)
               .setURI("https://github.com/" + qs.getGithubRepo())
               .setBranch(qs.getGitRef())
               .setCloneAllBranches(false)
               .call().close();
      // Perform changes
      MavenModelResource modelResource = resourceFactory.create(MavenModelResource.class,
               new File(projectDir, "pom.xml"));
      Model model = modelResource.getCurrentModel();
      model.setGroupId(topLevelPackage.getValue());
      model.setArtifactId(named.getValue());
      model.setVersion(version.getValue());

      // Change child modules
      for (String module : model.getModules())
      {
         File moduleDir = new File(projectDir, module);
         MavenModelResource moduleModelResource = resourceFactory.create(MavenModelResource.class,
                  new File(moduleDir, "pom.xml"));
         Model moduleModel = modelResource.getCurrentModel();
         Parent parent = moduleModel.getParent();
         if (parent != null)
         {
            parent.setGroupId(model.getGroupId());
            parent.setArtifactId(model.getArtifactId());
            parent.setVersion(model.getVersion());
            moduleModelResource.setCurrentModel(moduleModel);
         }
      }
      // FIXME: Change package name
      modelResource.setCurrentModel(model);
      // Delete unwanted files
      deleteUnwantedFiles(projectDir);
      context.getUIContext().setSelection(projectDir);
      return Results.success("Project created in " + projectDir);
   }

   private void deleteUnwantedFiles(File projectDir)
   {
      for (String file : FILES_TO_BE_DELETED)
      {
         Path pathToDelete = projectDir.toPath().resolve(file);
         try
         {
            org.obsidiantoaster.generator.Files.deleteRecursively(pathToDelete);
         }
         catch (IOException ignored)
         {
         }
      }
   }
}
