/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package io.openshift.launchpad.ui.osio;

import io.openshift.booster.catalog.Booster;
import io.openshift.booster.catalog.Mission;
import io.openshift.booster.catalog.Runtime;

/**
 */
public class BoosterDTO
{
   private String id;
   private String name;
   private String description;
   private String missionId;
   private String missionName;
   private String runtimeId;
   private String runtimeName;

   public BoosterDTO()
   {
   }

   public BoosterDTO(Booster booster)
   {
      this.id = booster.getId();
      this.name = booster.getName();
      this.description = booster.getDescription();
      Mission mission = booster.getMission();
      if (mission != null)
      {
         this.missionId = mission.getId();
         this.missionName = mission.getName();
      }
      Runtime runtime = booster.getRuntime();
      if (runtime != null)
      {
         this.runtimeId = runtime.getId();
         this.runtimeName = runtime.getName();
      }
   }

   @Override public String toString()
   {
      return "BoosterDTO{" +
               "name='" + name + '\'' +
               '}';
   }

   public String getId()
   {
      return id;
   }

   public void setId(String id)
   {
      this.id = id;
   }

   public String getName()
   {
      return name;
   }

   public void setName(String name)
   {
      this.name = name;
   }

   public String getDescription()
   {
      return description;
   }

   public void setDescription(String description)
   {
      this.description = description;
   }

   public String getMissionId()
   {
      return missionId;
   }

   public void setMissionId(String missionId)
   {
      this.missionId = missionId;
   }

   public String getMissionName()
   {
      return missionName;
   }

   public void setMissionName(String missionName)
   {
      this.missionName = missionName;
   }

   public String getRuntimeId()
   {
      return runtimeId;
   }

   public void setRuntimeId(String runtimeId)
   {
      this.runtimeId = runtimeId;
   }

   public String getRuntimeName()
   {
      return runtimeName;
   }

   public void setRuntimeName(String runtimeName)
   {
      this.runtimeName = runtimeName;
   }
}
