/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package io.openshift.launchpad.ui.booster;

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
import org.jboss.forge.addon.ui.context.UIValidationContext;
import org.jboss.forge.addon.ui.input.UISelectOne;
import org.jboss.forge.addon.ui.metadata.UICommandMetadata;
import org.jboss.forge.addon.ui.metadata.WithAttributes;
import org.jboss.forge.addon.ui.result.Result;
import org.jboss.forge.addon.ui.result.Results;
import org.jboss.forge.addon.ui.util.Categories;
import org.jboss.forge.addon.ui.util.Metadata;
import org.jboss.forge.addon.ui.wizard.UIWizardStep;

import io.openshift.launchpad.catalog.Booster;
import io.openshift.launchpad.catalog.BoosterCatalogService;
import io.openshift.launchpad.catalog.Mission;
import io.openshift.launchpad.catalog.Runtime;
import io.openshift.launchpad.ui.input.ProjectName;
import io.openshift.launchpad.ui.input.TopLevelPackage;
import io.openshift.launchpad.ui.input.Version;

/**
 *
 * @author <a href="mailto:ggastald@redhat.com">George Gastaldi</a>
 */
public class ChooseRuntimeStep implements UIWizardStep
{
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
      UIContext context = builder.getUIContext();
      if (context.getProvider().isGUI())
      {
         runtime.setItemLabelConverter(Runtime::getName);
      }
      else
      {
         runtime.setItemLabelConverter(Runtime::getId);
      }
      runtime.setValueChoices(() -> {
         Mission mission = (Mission) context.getAttributeMap().get(Mission.class);
         return catalogService.getRuntimes(mission);
      });
      builder.add(runtime).add(named).add(topLevelPackage).add(version);
   }

   @Override
   public void validate(UIValidationContext context)
   {
      UIContext uiContext = context.getUIContext();
      Mission mission = (Mission) uiContext.getAttributeMap().get(Mission.class);

      Optional<Booster> booster = catalogService.getBooster(mission, runtime.getValue());
      if (!booster.isPresent())
      {
         context.addValidationError(runtime,
                  "No booster found for mission '" + mission + "' and runtime '" + runtime.getValue() + "'");
      }
   }

   @Override
   public UICommandMetadata getMetadata(UIContext context)
   {
      return Metadata.forCommand(getClass()).name("Runtime")
               .description("Choose the runtime for your mission")
               .category(Categories.create("Openshift.io"));
   }

   @Override
   public Result execute(UIExecutionContext context) throws Exception
   {
      Map<Object, Object> attributeMap = context.getUIContext().getAttributeMap();
      Mission mission = (Mission) attributeMap.get(Mission.class);
      Runtime runtime = (Runtime) attributeMap.get(Runtime.class);
      Booster booster = catalogService.getBooster(mission, runtime).get();
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
