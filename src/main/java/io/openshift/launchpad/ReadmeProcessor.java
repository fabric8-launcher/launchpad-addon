/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package io.openshift.launchpad;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.net.URI;
import java.net.URL;
import java.util.Map;
import java.util.Properties;

import javax.inject.Singleton;

import org.apache.commons.lang3.text.StrSubstitutor;

/**
 * Reads the contents from the appdev-documentation repository
 *
 * @author <a href="mailto:ggastald@redhat.com">George Gastaldi</a>
 */
@Singleton
public class ReadmeProcessor
{
   static final String README_PROPERTIES_URL_PROPERTY = "LAUNCHPAD_BACKEND_README_PROPERTIES_URL";
   static final String README_TEMPLATE_URL_PROPERTY = "LAUNCHPAD_BACKEND_README_TEMPLATE_URL";

   private static final String README_TEMPLATE_URL = System.getProperty(README_TEMPLATE_URL_PROPERTY,
            System.getenv().getOrDefault(
                     README_TEMPLATE_URL_PROPERTY,
                     "https://raw.githubusercontent.com/openshiftio/appdev-documentation/master/docs/topics/readme/%s-README.adoc"));

   private static final String README_PROPERTIES_URL = System.getProperty(README_PROPERTIES_URL_PROPERTY,
            System.getenv().getOrDefault(
                     README_PROPERTIES_URL_PROPERTY,
                     "https://raw.githubusercontent.com/openshiftio/appdev-documentation/master/docs/topics/readme/%s-%s.properties"));

   URI getTemplateURI(String missionId)
   {
      return URI.create(String.format(README_TEMPLATE_URL, missionId));
   }

   URI getPropertiesURI(String missionId, String runtimeId)
   {
      return URI.create(String.format(README_PROPERTIES_URL, missionId, runtimeId));
   }

   public String getReadmeTemplate(String missionId) throws IOException
   {
      URL url = getTemplateURI(missionId).toURL();
      return loadContents(url);
   }

   @SuppressWarnings("all")
   public Map<String, String> getRuntimeProperties(String mission, String runtimeId)
   {
      Properties props = new Properties();
      try
      {
         props.load(getPropertiesURI(mission, runtimeId).toURL().openStream());
      }
      catch (IOException io)
      {
         // Do nothing
      }
      Map<String, String> map = (Map) props;
      return map;
   }

   public String processTemplate(String template, Map<String, String> values)
   {
      return new StrSubstitutor(values).replace(template);
   }

   private String loadContents(URL url) throws IOException
   {
      try (BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream())))
      {
         StringWriter writer = new StringWriter();
         char[] buffer = new char[1024];
         int c;
         while ((c = reader.read(buffer)) != -1)
         {
            writer.write(buffer, 0, c);
         }
         return writer.toString();
      }
   }

}
