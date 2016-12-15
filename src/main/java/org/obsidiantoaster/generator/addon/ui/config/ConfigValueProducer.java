package org.obsidiantoaster.generator.addon.ui.config;

import javax.annotation.PostConstruct;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.inject.Named;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

@Named
public class ConfigValueProducer 
{
   private Properties properties;

   @PostConstruct
   public void doInit() 
   {
      this.properties = new Properties();
      try(InputStream stream = ConfigValueProducer.class.getResourceAsStream("/settings.properties"))
      {
         this.properties.load(stream);
      }
      catch (final IOException e)
      {
         throw new RuntimeException("Configuration could not be loaded!");
      }
   }

   @Produces
   @ConfigValue("")
   @Dependent
   public String configValueProducer(InjectionPoint ip) 
   {
      ConfigValue configValue = ip.getAnnotated().getAnnotation(ConfigValue.class);
      final String property = System.getProperty(configValue.value());
      return property != null ? property : properties.getProperty(configValue.value());
   }
}
