/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package io.openshift.launchpad.ui.booster;

import java.util.List;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.core.HttpHeaders;

import org.jboss.forge.addon.ui.context.UIValidationContext;

import io.openshift.launchpad.MissionControl;

/**
 *
 * @author <a href="mailto:ggastald@redhat.com">George Gastaldi</a>
 */
@SuppressWarnings("unchecked")
@ApplicationScoped
public class MissionControlValidator
{
   @Inject
   private MissionControl missionControlFacade;

   public boolean validateGitHubTokenExists(UIValidationContext context)
   {
      Map<Object, Object> attributeMap = context.getUIContext().getAttributeMap();
      String validationMessage = (String) attributeMap.get("token_github_exists");
      if (validationMessage == null)
      {
         List<String> authList = (List<String>) attributeMap.get(HttpHeaders.AUTHORIZATION);
         String authHeader = (authList == null || authList.isEmpty()) ? null : authList.get(0);
         try
         {
            validationMessage = missionControlFacade.validateGitHubTokenExists(authHeader);
         }
         finally
         {
            if (validationMessage != null)
            {
               attributeMap.put("token_github_exists", validationMessage);
            }
         }
      }
      if (validationMessage != null && !MissionControl.VALIDATION_MESSAGE_OK.equals(validationMessage))
      {
         context.addValidationError(context.getCurrentInputComponent(), validationMessage);
         return false;
      }
      return true;
   }

   public boolean validateOpenShiftTokenExists(UIValidationContext context)
   {
      Map<Object, Object> attributeMap = context.getUIContext().getAttributeMap();
      String validationMessage = (String) attributeMap.get("token_openshift_exists");
      if (validationMessage == null)
      {
         List<String> authList = (List<String>) attributeMap.get(HttpHeaders.AUTHORIZATION);
         String authHeader = (authList == null || authList.isEmpty()) ? null : authList.get(0);
         try
         {
            validationMessage = missionControlFacade.validateOpenShiftTokenExists(authHeader);
         }
         finally
         {
            if (validationMessage != null)
            {
               attributeMap.put("token_openshift_exists", validationMessage);
            }
         }
      }
      if (validationMessage != null && !MissionControl.VALIDATION_MESSAGE_OK.equals(validationMessage))
      {
         context.addValidationError(context.getCurrentInputComponent(), validationMessage);
         return false;
      }
      return true;

   }

   public void validateGitHubRepositoryExists(UIValidationContext context, String repository)
   {
      Map<Object, Object> attributeMap = context.getUIContext().getAttributeMap();
      String validationMessage = (String) attributeMap.get("validate_repo_" + repository);
      if (validationMessage == null)
      {
         List<String> authList = (List<String>) attributeMap.get(HttpHeaders.AUTHORIZATION);
         String authHeader = (authList == null || authList.isEmpty()) ? null : authList.get(0);
         try
         {
            validationMessage = missionControlFacade.validateGitHubRepositoryExists(authHeader, repository);
         }
         finally
         {
            if (validationMessage != null)
            {
               attributeMap.put("validate_repo_" + repository, validationMessage);
            }
         }
      }
      if (validationMessage != null && !MissionControl.VALIDATION_MESSAGE_OK.equals(validationMessage))
      {
         context.addValidationError(context.getCurrentInputComponent(), validationMessage);
      }
   }

   public void validateOpenShiftProjectExists(UIValidationContext context, String project)
   {
      Map<Object, Object> attributeMap = context.getUIContext().getAttributeMap();
      String validationMessage = (String) attributeMap.get("validate_project_" + project);
      if (validationMessage == null)
      {
         List<String> authList = (List<String>) attributeMap.get(HttpHeaders.AUTHORIZATION);
         String authHeader = (authList == null || authList.isEmpty()) ? null : authList.get(0);
         try
         {
            validationMessage = missionControlFacade.validateOpenShiftProjectExists(authHeader, project);
         }
         finally
         {
            if (validationMessage != null)
            {
               attributeMap.put("validate_project_" + project, validationMessage);
            }
         }
      }
      if (validationMessage != null && !MissionControl.VALIDATION_MESSAGE_OK.equals(validationMessage))
      {
         context.addValidationError(context.getCurrentInputComponent(), validationMessage);
      }
   }
}