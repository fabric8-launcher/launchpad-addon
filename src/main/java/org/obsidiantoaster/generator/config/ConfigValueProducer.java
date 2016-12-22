/**
 * Copyright 2005-2015 Red Hat, Inc.
 *
 * Red Hat licenses this file to you under the Apache License, version
 * 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */
package org.obsidiantoaster.generator.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import javax.annotation.PostConstruct;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.inject.Named;

@Named
public class ConfigValueProducer
{
   private Properties properties;

   @PostConstruct
   public void doInit()
   {
      this.properties = new Properties();
      try (InputStream stream = ConfigValueProducer.class.getResourceAsStream("/settings.properties"))
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
      String property = System.getProperty(configValue.value(), System.getenv(configValue.value()));
      return property != null ? property : properties.getProperty(configValue.value());
   }
}
