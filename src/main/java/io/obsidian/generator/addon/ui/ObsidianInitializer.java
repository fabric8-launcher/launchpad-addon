/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package io.obsidian.generator.addon.ui;

import java.net.URL;

import javax.enterprise.event.Observes;

import org.jboss.forge.addon.maven.archetype.ArchetypeCatalogFactoryRegistry;
import org.jboss.forge.furnace.container.cdi.events.Local;
import org.jboss.forge.furnace.event.PostStartup;

/**
 *
 * @author <a href="mailto:ggastald@redhat.com">George Gastaldi</a>
 */
public class ObsidianInitializer
{

   public void onInit(@Observes @Local PostStartup startup, ArchetypeCatalogFactoryRegistry registry) throws Exception
   {
      // TODO: Change Archetype URL
      registry.addArchetypeCatalogFactory("Quickstarts", new URL(
               "https://gist.githubusercontent.com/cmoulliard/e9fac6b3040526780f2e96dcfa980c35/raw/5efe2d244fbe31bf9207c5dd5cb857e05a0e1928/gistfile1.txt"));
   }
}
