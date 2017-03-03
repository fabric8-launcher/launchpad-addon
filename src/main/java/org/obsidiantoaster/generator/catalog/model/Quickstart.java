/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.obsidiantoaster.generator.catalog.model;

import java.beans.Transient;

/**
 * A quickstart representation
 * 
 * @author <a href="mailto:ggastald@redhat.com">George Gastaldi</a>
 */
public class Quickstart
{
   private String id;
   private String githubRepo;
   private String gitRef;
   private String openshiftTemplatePath;
   private String obsidianDescriptorPath;

   private QuickstartMetadata metadata;

   public Quickstart()
   {
   }

   public String getName()
   {
      return getMetadata() != null ? getMetadata().getName() : null;
   }

   public String getDescription()
   {
      return getMetadata() != null ? getMetadata().getDescription() : null;
   }

   /**
    * @return the id
    */
   public String getId()
   {
      return id;
   }

   /**
    * @return the githubRepo
    */
   public String getGithubRepo()
   {
      return githubRepo;
   }

   /**
    * @return the gitRef
    */
   public String getGitRef()
   {
      return gitRef;
   }

   /**
    * @return the openshiftTemplatePath
    */
   public String getOpenshiftTemplatePath()
   {
      return openshiftTemplatePath;
   }

   /**
    * @return the obsidianDescriptorPath
    */
   public String getObsidianDescriptorPath()
   {
      return obsidianDescriptorPath;
   }

   /**
    * @param id the id to set
    */
   public void setId(String id)
   {
      this.id = id;
   }

   /**
    * @param githubRepo the githubRepo to set
    */
   public void setGithubRepo(String githubRepo)
   {
      this.githubRepo = githubRepo;
   }

   /**
    * @param gitRef the gitRef to set
    */
   public void setGitRef(String gitRef)
   {
      this.gitRef = gitRef;
   }

   /**
    * @param openshiftTemplatePath the openshiftTemplatePath to set
    */
   public void setOpenshiftTemplatePath(String openshiftTemplatePath)
   {
      this.openshiftTemplatePath = openshiftTemplatePath;
   }

   /**
    * @param obsidianDescriptorPath the obsidianDescriptorPath to set
    */
   public void setObsidianDescriptorPath(String obsidianDescriptorPath)
   {
      this.obsidianDescriptorPath = obsidianDescriptorPath;
   }

   /**
    * @return the metadata
    */
   @Transient
   public QuickstartMetadata getMetadata()
   {
      return metadata;
   }

   /**
    * @param metadata the metadata to set
    */
   public void setMetadata(QuickstartMetadata metadata)
   {
      this.metadata = metadata;
   }
}
