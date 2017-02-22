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
import java.io.InputStream;
import java.nio.file.Files;
import java.util.List;

import javax.inject.Inject;

import org.apache.maven.archetype.catalog.Archetype;
import org.jboss.forge.addon.maven.archetype.ArchetypeCatalogFactoryRegistry;
import org.jboss.forge.addon.maven.archetype.ArchetypeHelper;
import org.jboss.forge.addon.parser.java.utils.Packages;
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
import org.obsidiantoaster.generator.ui.ObsidianInitializer;
import org.obsidiantoaster.generator.ui.input.ProjectName;
import org.obsidiantoaster.generator.ui.input.TopLevelPackage;

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
   private ProjectName named;

   @Inject
   private TopLevelPackage topLevelPackage;

   @Inject
   @WithAttributes(label = "Project version", required = true, defaultValue = "1.0.0-SNAPSHOT")
   private UIInput<String> version;

   @Inject
   private ArchetypeCatalogFactoryRegistry registry;

   @Override
   public void initializeUI(UIBuilder builder) throws Exception
   {
      UIContext uiContext = builder.getUIContext();

      if (uiContext.getProvider().isGUI())
      {
         type.setItemLabelConverter(Archetype::getDescription);
      }
      List<Archetype> archetypes = registry.getArchetypeCatalogFactory(ObsidianInitializer.OBSIDIAN_QUICKSTARTS_CATALOG)
               .getArchetypeCatalog()
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
      InputStream artifactStream = getClass().getResourceAsStream(chosenArchetype.getArtifactId() + ".jar");
      File tmpDir = Files.createTempDirectory("projectdir").toFile();
      ArchetypeHelper archetypeHelper = new ArchetypeHelper(artifactStream, tmpDir,
               topLevelPackage.getValue(), named.getValue(),
               version.getValue());
      archetypeHelper.setPackageName(Packages.toValidPackageName(topLevelPackage.getValue()) + "."
               + Packages.toValidPackageName(named.getValue()));
      archetypeHelper.execute();

      context.getUIContext().setSelection(tmpDir);

      return Results.success("Project created in " + tmpDir);
   }

}
