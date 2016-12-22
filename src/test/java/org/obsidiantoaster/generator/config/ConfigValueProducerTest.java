package org.obsidiantoaster.generator.config;

import javax.inject.Inject;

import org.jglue.cdiunit.AdditionalClasses;
import org.jglue.cdiunit.CdiRunner;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(CdiRunner.class)
@AdditionalClasses(ConfigValueProducer.class)
public class ConfigValueProducerTest
{

   @Inject
   @ConfigValue("ARCHETYPE_CATALOG")
   private String archetypeCatalogLocation;

   @Test
   public void shouldInjectCatalogLocationUrl()
   {
      Assert.assertNotNull(archetypeCatalogLocation);
   }
}
