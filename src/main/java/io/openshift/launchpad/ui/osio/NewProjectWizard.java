/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package io.openshift.launchpad.ui.osio;

import io.openshift.booster.catalog.Booster;
import io.openshift.booster.catalog.BoosterCatalogService;
import io.openshift.booster.catalog.Mission;
import io.openshift.booster.catalog.Runtime;
import io.openshift.launchpad.BoosterCatalogServiceFactory;
import io.openshift.launchpad.ui.input.ProjectName;
import io.openshift.launchpad.ui.input.TopLevelPackage;
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

import javax.inject.Inject;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Creates a new project
 *
 * @author <a href="mailto:ggastald@redhat.com">George Gastaldi</a>
 */
public class NewProjectWizard implements UIWizard
{
   List<Booster> boosters = new ArrayList<>();
   @Inject
   @WithAttributes(label = "Booster", required = true)
   private UISelectOne<BoosterDTO> booster;
   @Inject
   private ProjectName named;

   /*
      @Inject
      private Version version;

   */
   @Inject
   private TopLevelPackage topLevelPackage;
   @Inject
   private BoosterCatalogServiceFactory catalogServiceFactory;
   @Inject
   private ProjectFactory projectFactory;
   @Inject
   private MavenBuildSystem mavenBuildSystem;

   @Override
   public void initializeUI(UIBuilder builder) throws Exception
   {
      List<BoosterDTO> boosterList = new ArrayList<>();
      boosters.clear();
      UIContext uiContext = builder.getUIContext();
      BoosterCatalogService catalogService = getCatalogService(uiContext);
      if (catalogService == null)
      {
         throw new IllegalArgumentException("No BoosterCatalogService");
      }
      Set<Mission> missions = catalogService.getMissions();
      for (Mission m : missions)
      {
         Set<Runtime> runtimes = catalogService.getRuntimes(m);
         for (Runtime r : runtimes)
         {
            Optional<Booster> optional = catalogService.getBooster(m, r);
            if (optional.isPresent())
            {
               Booster b = optional.get();
               boosters.add(b);
               boosterList.add(new BoosterDTO(b));
            }
         }
      }
      booster.setValueChoices(boosterList);
      booster.setItemLabelConverter(booster -> booster.getName());
      builder.add(booster).add(named).add(topLevelPackage);
      //.add(version);
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
   }

   @Override
   public NavigationResult next(UINavigationContext context) throws Exception
   {
      Map<Object, Object> attributeMap = context.getUIContext().getAttributeMap();
      attributeMap.put("name", named.getValue());
      attributeMap.put("type", getTypeValue());
      return null;
   }

   @Override
   public Result execute(UIExecutionContext context) throws Exception
   {
      BoosterDTO boosterDTO = getTypeValue();
      Booster booster = null;
      String boosterId = null;
      if (boosterDTO != null)
      {
         boosterId = boosterDTO.getId();
         for (Booster b : boosters)
         {
            if (boosterId.equals(b.getId()))
            {
               booster = b;
               break;
            }
         }
      }
      if (booster == null)
      {
         return Results.fail("Could not find boister " + boosterId);
      }
      DirectoryResource initialDir = (DirectoryResource) context.getUIContext().getInitialSelection().get();
      DirectoryResource projectDirectory = initialDir.getChildDirectory(named.getValue());
      // Using ProjectFactory to invoke bound listeners
      Project project = projectFactory.createProject(projectDirectory, mavenBuildSystem);
      // Do not cache anything
      projectFactory.invalidateCaches();
      MavenModelResource modelResource = projectDirectory.getChildOfType(MavenModelResource.class, "pom.xml");
      // Delete existing pom
      modelResource.delete();
      Path projectDirectoryPath = projectDirectory.getUnderlyingResourceObject().toPath();

      // Copy contents (including pom.xml if exists)
      BoosterCatalogService catalogService = getCatalogService(context.getUIContext());
      catalogService.copy(booster, projectDirectoryPath);
      // Perform model changes
      if (modelResource.exists())
      {
         Model model = modelResource.getCurrentModel();
         model.setGroupId(topLevelPackage.getValue());
         model.setArtifactId(named.getValue());

         // TODO whats the right property now?
         //model.setVersion(version.getValue());

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

   protected BoosterCatalogService getCatalogService(UIContext uiContext)
   {
      return catalogServiceFactory.getCatalogService(uiContext);
   }

   protected String getNameValue()
   {
      return named.getValue();
   }

   protected BoosterDTO getTypeValue()
   {
      return booster.getValue();
   }

   protected UISelectOne<BoosterDTO> getBooster()
   {
      return booster;
   }

   protected ProjectName getNamed()
   {
      return named;
   }
}