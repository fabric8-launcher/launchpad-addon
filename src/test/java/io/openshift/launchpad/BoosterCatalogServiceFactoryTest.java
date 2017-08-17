/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package io.openshift.launchpad;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;

import io.openshift.booster.catalog.BoosterCatalogService;

/**
 *
 * @author <a href="mailto:ggastald@redhat.com">George Gastaldi</a>
 */
public class BoosterCatalogServiceFactoryTest
{

   private BoosterCatalogServiceFactory factory;

   @Before
   public void setUp()
   {
      System.setProperty(BoosterCatalogServiceFactory.CATALOG_GIT_REF_PROPERTY_NAME, "openshift-online-free");
      factory = new BoosterCatalogServiceFactory();
      // Forcing CDI initialization here
      factory.init(null);
   }

   @Test
   public void testDefaultCatalogServiceNotNullAndIsSingleton()
   {
      BoosterCatalogService defaultService = factory.getDefaultBoosterCatalogService();
      assertThat(defaultService).isNotNull();
      assertThat(factory.getDefaultBoosterCatalogService()).isSameAs(defaultService);
   }

   @Test
   public void testMasterCatalogIsNotSameAsDefault()
   {
      // A null catalogURL means use default repository URL
      BoosterCatalogService masterService = factory.getCatalogService(null, "master");
      assertThat(masterService).isNotNull();
      assertThat(factory.getDefaultBoosterCatalogService()).isNotSameAs(masterService);
   }

}
