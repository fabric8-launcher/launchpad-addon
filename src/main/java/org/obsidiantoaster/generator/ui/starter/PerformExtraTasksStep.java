/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.obsidiantoaster.generator.ui.starter;

import java.io.StringReader;
import java.net.URL;
import java.util.Map;

import javax.inject.Inject;

import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.jboss.forge.addon.maven.projects.MavenFacet;
import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.resource.DirectoryResource;
import org.jboss.forge.addon.resource.FileResource;
import org.jboss.forge.addon.resource.Resource;
import org.jboss.forge.addon.resource.ResourceFactory;
import org.jboss.forge.addon.templates.Template;
import org.jboss.forge.addon.templates.TemplateFactory;
import org.jboss.forge.addon.templates.freemarker.FreemarkerTemplate;
import org.jboss.forge.addon.ui.context.UIContext;
import org.jboss.forge.addon.ui.context.UIExecutionContext;
import org.jboss.forge.addon.ui.result.Result;
import org.jboss.forge.addon.ui.result.Results;
import org.jboss.forge.addon.ui.wizard.UIWizardStep;

/**
 *
 * @author <a href="mailto:ggastald@redhat.com">George Gastaldi</a>
 */
public class PerformExtraTasksStep implements UIWizardStep
{
   @Inject
   TemplateFactory templateFactory;

   @Inject
   ResourceFactory resourceFactory;

   @Override
   public Result execute(UIExecutionContext context) throws Exception
   {
      UIContext uiContext = context.getUIContext();
      Map<Object, Object> attributeMap = uiContext.getAttributeMap();
      Project project = (Project) attributeMap.get(Project.class);
      DirectoryResource root = project.getRoot().reify(DirectoryResource.class);

      // Add Fabric8 plugin
      String pluginPomXml = getTemplateFor("plugin-template.xml.ftl").process(attributeMap);
      Model pluginModel = new MavenXpp3Reader().read(new StringReader(pluginPomXml));
      Plugin fabric8MavenPlugin = pluginModel.getBuild().getPlugins().get(0);
      MavenFacet mavenFacet = project.getFacet(MavenFacet.class);
      Model model = mavenFacet.getModel();
      Build build = model.getBuild();
      if (build == null)
      {
         build = new Build();
         model.setBuild(build);
      }
      build.addPlugin(fabric8MavenPlugin);
      mavenFacet.setModel(model);

      // Create README.md
      FileResource<?> child = root.getChild("README.md").reify(FileResource.class);
      child.setContents(getTemplateFor("README.md.ftl").process(attributeMap));

      // Create src/main/fabric8 dir
      DirectoryResource fabric8Dir = root.getChildDirectory("src/main/fabric8");
      fabric8Dir.mkdirs();

      // Create route.yml
      FileResource<?> routeYml = fabric8Dir.getChild("route.yml").reify(FileResource.class);
      routeYml.setContents(getTemplateFor("route.yml.ftl").process(attributeMap));

      // Create svc.yml
      FileResource<?> svcYml = fabric8Dir.getChild("svc.yml").reify(FileResource.class);
      svcYml.setContents(getTemplateFor("svc.yml.ftl").process(attributeMap));

      return Results.success();
   }

   private Template getTemplateFor(String name)
   {
      Resource<URL> resource = resourceFactory.create(getClass().getResource(name));
      return templateFactory.create(resource, FreemarkerTemplate.class);
   }

}
