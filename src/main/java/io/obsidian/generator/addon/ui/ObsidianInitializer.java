/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package io.obsidian.generator.addon.ui;

import java.net.URL;
import java.util.Arrays;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Produces;

import org.jboss.forge.addon.maven.archetype.ArchetypeCatalogFactoryRegistry;
import org.jboss.forge.addon.projects.ProjectType;
import org.jboss.forge.addon.swarm.project.WildFlySwarmProjectType;
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

   public void onInit(@Observes @Local PostStartup startup, ArchetypeCatalogFactoryRegistry registry) throws Exception
   {
      registry.addArchetypeCatalogFactory("Quickstarts", new URL(
               "https://repository.jboss.org/nexus/service/local/artifact/maven/redirect?r=snapshots&g=io.obsidian&a=archetypes-catalog&v=1.0.0-SNAPSHOT&e=xml&c=archetype-catalog"));
   }

   @Produces
   @ApplicationScoped
   public List<ProjectType> getSupportedProjectTypes(
            SpringBootProjectType springBoot,
            WildFlySwarmProjectType wildFlySwarm,
            VertxProjectType vertx)
   {
      return Arrays.asList(springBoot, wildFlySwarm, vertx);
   }
}
