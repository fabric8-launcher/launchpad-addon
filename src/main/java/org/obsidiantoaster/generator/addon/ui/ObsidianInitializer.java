/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.obsidiantoaster.generator.addon.ui;

import java.net.URL;
import java.util.Arrays;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Produces;

import io.obsidian.generator.addon.ui.config.ConfigValue;
import org.jboss.forge.addon.maven.archetype.ArchetypeCatalogFactoryRegistry;
import org.jboss.forge.addon.projects.ProjectType;
import org.jboss.forge.furnace.container.cdi.events.Local;
import org.jboss.forge.furnace.event.PostStartup;

import io.fabric8.forge.devops.springboot.SpringBootProjectType;
import io.vertx.forge.project.VertxProjectType;

/**
 *
 * @author <a href="mailto:ggastald@redhat.com">George Gastaldi</a>
 */
public class ObsidianInitializer
{

   @ConfigValue("archetype.catalog")
   private String archetypeCatalogLocation;

   public void onInit(@Observes @Local PostStartup startup, ArchetypeCatalogFactoryRegistry registry) throws Exception
   {
      registry.addArchetypeCatalogFactory("Quickstarts", new URL(archetypeCatalogLocation));
   }

   @Produces
   @ApplicationScoped
   public List<ProjectType> getSupportedProjectTypes(
            SpringBootProjectType springBoot,
            // WildFlySwarmProjectType wildFlySwarm,
            VertxProjectType vertx)
   {
      return Arrays.asList(springBoot, vertx);
   }
}
