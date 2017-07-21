/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package io.openshift.launchpad;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Produces;
import javax.inject.Singleton;

import org.jboss.forge.furnace.container.cdi.events.Local;
import org.jboss.forge.furnace.event.PostStartup;

import io.openshift.booster.catalog.BoosterCatalogService;

/**
 * Application scoped producer for {@link BoosterCatalogService}
 * 
 * @author <a href="mailto:ggastald@redhat.com">George Gastaldi</a>
 */
@ApplicationScoped
public class BoosterCatalogServiceProducer
{
   private static final String CATALOG_GIT_REF_PROPERTY_NAME = "LAUNCHPAD_BACKEND_CATALOG_GIT_REF";
   private static final String CATALOG_GIT_REPOSITORY_PROPERTY_NAME = "LAUNCHPAD_BACKEND_CATALOG_GIT_REPOSITORY";

   private static final String DEFAULT_GIT_REF = "master";
   private static final String DEFAULT_GIT_REPOSITORY_URL = "https://github.com/openshiftio/booster-catalog.git";

   private BoosterCatalogService boosterCatalog;

   public void init(@Observes @Local PostStartup event)
   {
      this.boosterCatalog = new BoosterCatalogService.Builder()
               .catalogRepository(getEnvVarOrSysProp(CATALOG_GIT_REPOSITORY_PROPERTY_NAME, DEFAULT_GIT_REPOSITORY_URL))
               .catalogRef(getEnvVarOrSysProp(CATALOG_GIT_REF_PROPERTY_NAME, DEFAULT_GIT_REF))
               .build();
      boosterCatalog.index();
   }

   @Produces
   @Singleton
   public BoosterCatalogService produceBoosterCatalogService()
   {
      return boosterCatalog;
   }

   private static String getEnvVarOrSysProp(String name, String defaultValue)
   {
      return System.getProperty(name, System.getenv().getOrDefault(name, defaultValue));
   }
}