/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.obsidiantoaster.generator.addon.ui;

import java.io.InputStream;
import java.util.Map;
import java.util.regex.Pattern;

import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.projects.facets.MetadataFacet;
import org.jboss.forge.addon.resource.DirectoryResource;
import org.jboss.forge.addon.resource.FileResource;
import org.jboss.forge.addon.ui.context.UIContext;
import org.jboss.forge.addon.ui.context.UIExecutionContext;
import org.jboss.forge.addon.ui.result.Result;
import org.jboss.forge.addon.ui.result.Results;
import org.jboss.forge.addon.ui.wizard.UIWizardStep;
import org.jboss.forge.furnace.util.Streams;

/**
 *
 * @author <a href="mailto:ggastald@redhat.com">George Gastaldi</a>
 */
public class AddMissingFilesStep implements UIWizardStep
{
   @Override
   public Result execute(UIExecutionContext context) throws Exception
   {
      UIContext uiContext = context.getUIContext();
      Map<Object, Object> attributeMap = uiContext.getAttributeMap();
      Project project = (Project) attributeMap.get(Project.class);
      MetadataFacet metadataFacet = project.getFacet(MetadataFacet.class);
      String name = metadataFacet.getProjectName();
      DirectoryResource root = project.getRoot().reify(DirectoryResource.class);
      FileResource<?> child = root.getChild("README.md").reify(FileResource.class);
      try (InputStream stream = getClass().getResourceAsStream("README.md"))
      {
         child.setContents(stream);
      }
      // Create src/main/fabric8 dir
      DirectoryResource fabric8Dir = root.getChildDirectory("src/main/fabric8");
      fabric8Dir.mkdirs();
      FileResource<?> routeYml = fabric8Dir.getChild("route.yml").reify(FileResource.class);
      try (InputStream stream = getClass().getResourceAsStream("route.yml"))
      {
         String contents = Streams.toString(stream);
         routeYml.setContents(contents.replaceAll(Pattern.quote("${name}"), name));
      }
      FileResource<?> svcYml = fabric8Dir.getChild("svc.yml").reify(FileResource.class);
      try (InputStream stream = getClass().getResourceAsStream("svc.yml"))
      {
         String contents = Streams.toString(stream);
         svcYml.setContents(contents.replaceAll(Pattern.quote("${name}"), name));
      }

      return Results.success();
   }

}
