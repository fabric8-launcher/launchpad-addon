/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package io.openshift.launchpad.catalog;

import java.beans.Transient;
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
   @Transient
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

   @Override
   public int hashCode()
   {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((gitRef == null) ? 0 : gitRef.hashCode());
      result = prime * result + ((githubRepo == null) ? 0 : githubRepo.hashCode());
      result = prime * result + ((id == null) ? 0 : id.hashCode());
      result = prime * result + ((obsidianDescriptorPath == null) ? 0 : obsidianDescriptorPath.hashCode());
      return result;
   }

   @Override
   public boolean equals(Object obj)
   {
      if (this == obj)
         return true;
      if (obj == null)
         return false;
      if (getClass() != obj.getClass())
         return false;
      Quickstart other = (Quickstart) obj;
      if (gitRef == null)
      {
         if (other.gitRef != null)
            return false;
      }
      else if (!gitRef.equals(other.gitRef))
         return false;
      if (githubRepo == null)
      {
         if (other.githubRepo != null)
            return false;
      }
      else if (!githubRepo.equals(other.githubRepo))
         return false;
      if (id == null)
      {
         if (other.id != null)
            return false;
      }
      else if (!id.equals(other.id))
         return false;
      if (obsidianDescriptorPath == null)
      {
         if (other.obsidianDescriptorPath != null)
            return false;
      }
      else if (!obsidianDescriptorPath.equals(other.obsidianDescriptorPath))
         return false;
      return true;
   }

   @Override
   public String toString()
   {
      return "Quickstart [githubRepo=" + githubRepo + ", gitRef=" + gitRef + ", obsidianDescriptorPath="
               + obsidianDescriptorPath + ", metadata=" + metadata + ", getName()=" + getName() + ", getDescription()="
               + getDescription() + "]";
   }
}
