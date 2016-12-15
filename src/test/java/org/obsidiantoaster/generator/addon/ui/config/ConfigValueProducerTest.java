package org.obsidiantoaster.generator.addon.ui.config;

import org.jglue.cdiunit.AdditionalClasses;
import org.jglue.cdiunit.CdiRunner;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.inject.Inject;

@RunWith(CdiRunner.class)
@AdditionalClasses(ConfigValueProducer.class)
public class ConfigValueProducerTest {

    @Inject
    @ConfigValue("archetype.catalog")
    private String archetypeCatalogLocation;

    @Test
    public void shouldInjectCatalogLocationUrl() {
        Assert.assertNotNull(archetypeCatalogLocation);
    }
}
