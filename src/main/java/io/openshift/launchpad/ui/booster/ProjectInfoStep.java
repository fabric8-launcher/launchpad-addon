/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package io.openshift.launchpad.ui.booster;

import java.io.FileNotFoundException;
import java.net.URL;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;

import org.apache.commons.lang3.text.StrSubstitutor;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.jboss.forge.addon.maven.resources.MavenModelResource;
import org.jboss.forge.addon.parser.json.resource.JsonResource;
import org.jboss.forge.addon.resource.DirectoryResource;
import org.jboss.forge.addon.resource.FileResource;
import org.jboss.forge.addon.resource.ResourceFactory;
import org.jboss.forge.addon.resource.URLResource;
import org.jboss.forge.addon.ui.context.UIBuilder;
import org.jboss.forge.addon.ui.context.UIContext;
import org.jboss.forge.addon.ui.context.UIExecutionContext;
import org.jboss.forge.addon.ui.context.UIValidationContext;
import org.jboss.forge.addon.ui.input.UIInput;
import org.jboss.forge.addon.ui.metadata.UICommandMetadata;
import org.jboss.forge.addon.ui.metadata.WithAttributes;
import org.jboss.forge.addon.ui.result.Result;
import org.jboss.forge.addon.ui.result.Results;
import org.jboss.forge.addon.ui.util.Categories;
import org.jboss.forge.addon.ui.util.Metadata;
import org.jboss.forge.addon.ui.wizard.UIWizardStep;
import org.jboss.forge.furnace.util.Strings;

import io.openshift.booster.catalog.Booster;
import io.openshift.booster.catalog.BoosterCatalogService;
import io.openshift.booster.catalog.Mission;
import io.openshift.booster.catalog.Runtime;
import io.openshift.launchpad.ui.input.ProjectName;

/**
 *
 * @author <a href="mailto:ggastald@redhat.com">George Gastaldi</a>
 */
public class ProjectInfoStep implements UIWizardStep
{
   private static final Logger logger = Logger.getLogger(ProjectInfoStep.class.getName());

   @Inject
   private BoosterCatalogService catalogService;

   @Inject
   private ProjectName named;

   @Inject
   private MissionControlValidator missionControlValidator;

   /**
    * Used in LaunchpadResource
    */
   @Inject
   @WithAttributes(label = "GitHub Repository", note = "If empty, it will assume the project name")
   private UIInput<String> gitHubRepositoryName;

   @Inject
   @WithAttributes(label = "Group Id", defaultValue = "io.openshift.booster", required = true)
   private UIInput<String> groupId;

   @Inject
   @WithAttributes(label = "Artifact Id", required = true)
   private UIInput<String> artifactId;

   @Inject
   @WithAttributes(label = "Version", required = true, defaultValue = "1.0.0-SNAPSHOT")
   private UIInput<String> version;

   @Inject
   private ResourceFactory resourceFactory;

   private static final String TEMPLATE_URL = System.getenv().getOrDefault("LAUNCHPAD_BACKEND_README_TEMPLATE_URL",
            "https://raw.githubusercontent.com/openshiftio/appdev-documentation/master/docs/topics/%s-README.adoc");

   @Override
   public void initializeUI(UIBuilder builder) throws Exception
   {
      UIContext context = builder.getUIContext();
      artifactId.setDefaultValue(() -> {
         Mission mission = (Mission) context.getAttributeMap().get(Mission.class);
         Runtime runtime = (Runtime) context.getAttributeMap().get(Runtime.class);

         String missionPrefix = (mission == null) ? "" : "-" + mission.getId();
         String runtimeSuffix = (runtime == null) ? "" : "-" + runtime.getId().replaceAll("\\.", "");

         return "booster" + missionPrefix + runtimeSuffix;
      });
      DeploymentType deploymentType = (DeploymentType) context.getAttributeMap().get(DeploymentType.class);
      if (deploymentType == DeploymentType.CONTINUOUS_DELIVERY)
      {
         builder.add(named).add(gitHubRepositoryName);
      }
      Runtime runtime = (Runtime) context.getAttributeMap().get(Runtime.class);
      if (isNodeJS(runtime))
      {
         // NodeJS only requires the name and version
         artifactId.setLabel("Name");
         version.setDefaultValue("1.0.0");
         builder.add(artifactId).add(version);
      }
      else
      {
         builder.add(groupId).add(artifactId).add(version);
      }
   }

   @Override
   public void validate(UIValidationContext context)
   {
      UIContext uiContext = context.getUIContext();
      if ("next".equals(uiContext.getAttributeMap().get("action")))
      {
         // Do not validate again if next() was called
         return;
      }
      DeploymentType deploymentType = (DeploymentType) uiContext.getAttributeMap()
               .get(DeploymentType.class);
      if (deploymentType == DeploymentType.CONTINUOUS_DELIVERY
               && System.getenv("LAUNCHPAD_MISSION_CONTROL_VALIDATION_SKIP") == null)
      {
         if (missionControlValidator.validateOpenShiftTokenExists(context))
         {
            missionControlValidator.validateOpenShiftProjectExists(context, named.getValue());
         }
         if (missionControlValidator.validateGitHubTokenExists(context))
         {
            String repository = gitHubRepositoryName.getValue();
            if (Strings.isNullOrEmpty(repository))
            {
               repository = named.getValue();
            }
            missionControlValidator.validateGitHubRepositoryExists(context, repository);
         }
      }
   }

   @Override
   public UICommandMetadata getMetadata(UIContext context)
   {
      return Metadata.forCommand(getClass()).name("Project Info")
               .description("Project Information")
               .category(Categories.create("Openshift.io"));
   }

   @SuppressWarnings("unchecked")
   @Override
   public Result execute(UIExecutionContext context) throws Exception
   {
      Map<Object, Object> attributeMap = context.getUIContext().getAttributeMap();
      Mission mission = (Mission) attributeMap.get(Mission.class);
      Runtime runtime = (Runtime) attributeMap.get(Runtime.class);
      DeploymentType deploymentType = (DeploymentType) attributeMap.get(DeploymentType.class);
      Booster booster = catalogService.getBooster(mission, runtime).get();
      DirectoryResource initialDir = (DirectoryResource) context.getUIContext().getInitialSelection().get();
      String childDirectory = deploymentType == DeploymentType.CONTINUOUS_DELIVERY ? named.getValue()
               : artifactId.getValue();
      DirectoryResource projectDirectory = initialDir.getChildDirectory(childDirectory);
      projectDirectory.mkdirs();
      Path projectDirectoryPath = projectDirectory.getUnderlyingResourceObject().toPath();
      // Copy contents
      catalogService.copy(booster, projectDirectoryPath);
      // Is it a maven project?
      MavenModelResource modelResource = projectDirectory.getChildOfType(MavenModelResource.class, "pom.xml");

      // Perform model changes
      if (modelResource.exists())
      {
         Model model = modelResource.getCurrentModel();
         model.setGroupId(groupId.getValue());
         model.setArtifactId(artifactId.getValue());
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
         modelResource.setCurrentModel(model);
      }

      // If NodeJS, just change name and version
      if (isNodeJS(runtime))
      {
         JsonResource packageJsonResource = projectDirectory.getChildOfType(JsonResource.class, "package.json");
         if (packageJsonResource.exists())
         {
            JsonObjectBuilder job = Json.createObjectBuilder();
            job.add("name", artifactId.getValue());
            job.add("version", version.getValue());
            for (Entry<String, JsonValue> entry : packageJsonResource.getJsonObject().entrySet())
            {
               String key = entry.getKey();
               // Do not copy name or version
               if (key.equals("name") || key.equals("version"))
               {
                  continue;
               }
               job.add(key, entry.getValue());
            }
            packageJsonResource.setContents(job.build());
         }
      }
      // Create README.adoc file
      FileResource<?> readmeAdoc = projectDirectory.getChildOfType(FileResource.class, "README.adoc");
      String formattedTemplateUrl = String.format(TEMPLATE_URL, mission.getId());
      URLResource templateResource = resourceFactory.create(URLResource.class, new URL(formattedTemplateUrl));
      Map<String, String> values = new HashMap<>();
      values.put("missionId", mission.getId());
      values.put("mission", mission.getName());
      values.put("runtimeId", runtime.getId());
      values.put("runtime", runtime.getName());
      values.put("openShiftProject", named.getValue());
      values.put("groupId", groupId.getValue());
      values.put("artifactId", artifactId.getValue());
      values.put("version", version.getValue());
      values.put("targetRepository", Objects.toString(gitHubRepositoryName.getValue(), named.getValue()));
      try
      {
         String readmeOutput = new StrSubstitutor(values).replace(templateResource.getContents());
         readmeAdoc.setContents(readmeOutput);
         // Delete README.md
         projectDirectory.getChildOfType(FileResource.class, "README.md").delete();
      }
      catch (Exception e)
      {
         if (e instanceof FileNotFoundException)
         {
            logger.log(Level.WARNING, "No README.adoc template found for " + mission.getId());
         }
         else
         {
            logger.log(Level.SEVERE, "Error while creating README.adoc", e);
         }
      }

      context.getUIContext().setSelection(projectDirectory);
      return Results.success();
   }

   private boolean isNodeJS(Runtime runtime)
   {
      return runtime != null && "nodejs".equals(runtime.getId());
   }

}
