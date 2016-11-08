/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.jboss.forge.addon.obsidian.generator.ui;

import java.util.List;

import org.jboss.forge.addon.obsidian.generator.model.Feature;
import org.jboss.forge.addon.obsidian.generator.model.LanguageRuntime;
import org.jboss.forge.addon.obsidian.generator.model.Pattern;
import org.jboss.forge.addon.obsidian.generator.model.Quickstart;
import org.jboss.forge.addon.ui.cdi.CommandScoped;

/**
 * This will store the chosen options
 * 
 * @author <a href="mailto:ggastald@redhat.com">George Gastaldi</a>
 */
@CommandScoped
public class GeneratorModel
{
   private LanguageRuntime languageRuntime;
   private List<Feature> features;
   private List<Pattern> patterns;

   private Quickstart quickstart;

   private String projectName;
   private String packageName;
   private String version;

   /**
    * @return the languageRuntime
    */
   public LanguageRuntime getLanguageRuntime()
   {
      return languageRuntime;
   }

   /**
    * @param languageRuntime the languageRuntime to set
    */
   public void setLanguageRuntime(LanguageRuntime languageRuntime)
   {
      this.languageRuntime = languageRuntime;
   }

   /**
    * @return the features
    */
   public List<Feature> getFeatures()
   {
      return features;
   }

   /**
    * @param features the features to set
    */
   public void setFeatures(List<Feature> features)
   {
      this.features = features;
   }

   /**
    * @return the quickstart
    */
   public Quickstart getQuickstart()
   {
      return quickstart;
   }

   /**
    * @param quickstart the quickstart to set
    */
   public void setQuickstart(Quickstart quickstart)
   {
      this.quickstart = quickstart;
   }

   /**
    * @return the patterns
    */
   public List<Pattern> getPatterns()
   {
      return patterns;
   }

   /**
    * @param patterns the patterns to set
    */
   public void setPatterns(List<Pattern> patterns)
   {
      this.patterns = patterns;
   }

   /**
    * @return the projectName
    */
   public String getProjectName()
   {
      return projectName;
   }

   /**
    * @param projectName the projectName to set
    */
   public void setProjectName(String projectName)
   {
      this.projectName = projectName;
   }

   /**
    * @return the packageName
    */
   public String getPackageName()
   {
      return packageName;
   }

   /**
    * @param packageName the packageName to set
    */
   public void setPackageName(String packageName)
   {
      this.packageName = packageName;
   }

   /**
    * @return the version
    */
   public String getVersion()
   {
      return version;
   }

   /**
    * @param version the version to set
    */
   public void setVersion(String version)
   {
      this.version = version;
   }

}
