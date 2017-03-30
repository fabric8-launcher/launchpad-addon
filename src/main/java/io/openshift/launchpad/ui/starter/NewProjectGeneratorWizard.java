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
package io.openshift.launchpad.ui.starter;

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
import org.jboss.forge.addon.ui.result.navigation.NavigationResultBuilder;
import org.jboss.forge.addon.ui.util.Categories;
import org.jboss.forge.addon.ui.util.Metadata;
import org.jboss.forge.addon.ui.wizard.UIWizard;

import io.openshift.launchpad.ui.input.ProjectName;
import io.openshift.launchpad.ui.input.TopLevelPackage;
import io.openshift.launchpad.ui.input.Version;

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
   private ProjectName named;

   @Inject
   private TopLevelPackage topLevelPackage;

   @Inject
   private Version version;

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
      return Metadata.forCommand(getClass()).name("Launchpad: New Project")
               .description("Generate your project")
               .category(Categories.create("Openshift.io"));
   }

   @Override
   public Result execute(UIExecutionContext context) throws Exception
   {
      UIContext uiContext = context.getUIContext();
      DirectoryResource initialSelection = (DirectoryResource) uiContext.getInitialSelection().get();
      Project project = projectFactory.createProject(initialSelection.getChildDirectory(named.getValue()),
               mavenBuildSystem);
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
      uiContext.setSelection(project.getRoot());
      Map<Object, Object> attributeMap = uiContext.getAttributeMap();
      attributeMap.put(Project.class, project);

      attributeMap.put("name", metadataFacet.getProjectName());
      attributeMap.put("type", projectType.toString());

      return Results.success();
   }

   @Override
   public NavigationResult next(UINavigationContext context) throws Exception
   {
      Map<Object, Object> attributeMap = context.getUIContext().getAttributeMap();
      attributeMap.put("name", named.getValue());
      attributeMap.put("type", type.getValue());

      NavigationResultBuilder builder = NavigationResultBuilder.create();
      ProjectType nextStep = type.getValue();
      if (nextStep != null)
      {
         builder.add(nextStep.next(context));
      }
      builder.add(PerformExtraTasksStep.class);
      return builder.build();
   }

}
