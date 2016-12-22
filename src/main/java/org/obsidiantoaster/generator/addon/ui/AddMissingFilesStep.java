/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.obsidiantoaster.generator.addon.ui;

import java.net.URL;
import java.util.Map;

import javax.inject.Inject;

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
public class AddMissingFilesStep implements UIWizardStep
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

      // Create README.md
      FileResource<?> child = root.getChild("README.md").reify(FileResource.class);
      child.setContents(getTemplateFor("README.md").process(attributeMap));

      // Create src/main/fabric8 dir
      DirectoryResource fabric8Dir = root.getChildDirectory("src/main/fabric8");
      fabric8Dir.mkdirs();

      // Create route.yml
      FileResource<?> routeYml = fabric8Dir.getChild("route.yml").reify(FileResource.class);
      routeYml.setContents(getTemplateFor("route.yml").process(attributeMap));

      // Create svc.yml
      FileResource<?> svcYml = fabric8Dir.getChild("svc.yml").reify(FileResource.class);
      svcYml.setContents(getTemplateFor("svc.yml").process(attributeMap));

      return Results.success();
   }

   private Template getTemplateFor(String name)
   {
      Resource<URL> resource = resourceFactory.create(getClass().getResource(name));
      return templateFactory.create(resource, FreemarkerTemplate.class);
   }

}
