/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.jboss.forge.addon.obsidian.generator.model;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author <a href="mailto:ggastald@redhat.com">George Gastaldi</a>
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class LanguageRuntime
{
   private final String name;

   private Set<Feature> features = new HashSet<>();
   private Set<Quickstart> quickstarts = new HashSet<>();

   public LanguageRuntime(String name)
   {
      this.name = name;
   }

   public String getName()
   {
      return name;
   }

   /**
    * @param features the features to set
    */
   public void setFeatures(Set<Feature> features)
   {
      this.features = features;
   }

   /**
    * @return the features
    */
   public Set<Feature> getFeatures()
   {
      return Collections.unmodifiableSet(features);
   }

   /**
    * @param quickstarts the quickstarts to set
    */
   public void setQuickstarts(Set<Quickstart> quickstarts)
   {
      this.quickstarts = quickstarts;
   }

   public Set<Quickstart> getQuickstarts()
   {
      return Collections.unmodifiableSet(quickstarts);
   }

}
