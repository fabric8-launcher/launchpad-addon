/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.obsidiantoaster.generator.catalog;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

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
   private String obsidianDescriptorPath = ".obsidian/obsidian.yaml";

   private Map<String, Object> metadata = Collections.emptyMap();

   public Quickstart()
   {
   }

   public String getName()
   {
      return Objects.toString(getMetadata().get("name"), getId());
   }

   public String getDescription()
   {
      return Objects.toString(getMetadata().get("description"), "No description available");
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
    * @param obsidianDescriptorPath the obsidianDescriptorPath to set
    */
   public void setObsidianDescriptorPath(String obsidianDescriptorPath)
   {
      this.obsidianDescriptorPath = obsidianDescriptorPath;
   }

   /**
    * @return the metadata
    */
   public Map<String, Object> getMetadata()
   {
      return metadata;
   }

   /**
    * @param metadata the metadata to set
    */
   public void setMetadata(Map<String, Object> metadata)
   {
      this.metadata = metadata;
   }
}
