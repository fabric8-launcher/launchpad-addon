/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.obsidiantoaster.generator.addon.facet;

import org.jboss.forge.addon.maven.plugins.ConfigurationBuilder;
import org.jboss.forge.addon.maven.plugins.ConfigurationElementBuilder;
import org.jboss.forge.addon.maven.plugins.ExecutionBuilder;
import org.jboss.forge.addon.maven.plugins.MavenPluginBuilder;
import org.jboss.forge.addon.maven.projects.MavenPluginFacet;
import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.projects.facets.AbstractProjectFacet;

import io.fabric8.forge.addon.utils.MavenHelpers;
import io.fabric8.forge.addon.utils.VersionHelper;

/**
 *
 * @author <a href="mailto:ggastald@redhat.com">George Gastaldi</a>
 */
public class Fabric8MavenPluginFacet extends AbstractProjectFacet
{

   @Override
   public boolean install()
   {
      Project project = getFaceted();
      ConfigurationElementBuilder elementBuilder = ConfigurationElementBuilder.create().setName("resources");
      elementBuilder.addChild("labels").addChild("service").addChild("expose").setText("true");
      MavenPluginBuilder pluginBuilder = MavenPluginBuilder.create()
               .setCoordinate(MavenHelpers.createCoordinate("io.fabric8", "fabric8-maven-plugin",
                        VersionHelper.fabric8MavenPluginVersion()))
               .setConfiguration(ConfigurationBuilder.create().addConfigurationElement(elementBuilder))
               .addExecution(ExecutionBuilder.create().addGoal("resource").addGoal("build"));
      MavenPluginFacet pluginFacet = project.getFacet(MavenPluginFacet.class);
      pluginFacet.addPlugin(pluginBuilder);
      return isInstalled();
   }

   @Override
   public boolean isInstalled()
   {
      return MavenHelpers.findPlugin(getFaceted(), "io.fabric8", "fabric8-maven-plugin") != null;
   }

}
