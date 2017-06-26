/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package io.openshift.launchpad.ui.booster;

import java.net.ConnectException;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import org.jboss.forge.addon.ui.context.UIValidationContext;

/**
 *
 * @author <a href="mailto:ggastald@redhat.com">George Gastaldi</a>
 */
@SuppressWarnings("unchecked")
@ApplicationScoped
public class MissionControlValidator
{
   private static final String LAUNCHPAD_MISSIONCONTROL_SERVICE_HOST = "LAUNCHPAD_MISSIONCONTROL_SERVICE_HOST";
   private static final String LAUNCHPAD_MISSIONCONTROL_SERVICE_PORT = "LAUNCHPAD_MISSIONCONTROL_SERVICE_PORT";

   private static final String VALIDATION_MESSAGE_OK = "OK";

   private URI missionControlURI;

   public boolean validateGitHubTokenExists(UIValidationContext context)
   {
      Map<Object, Object> attributeMap = context.getUIContext().getAttributeMap();
      String validationMessage = (String) attributeMap.get("token_github_exists");
      if (validationMessage == null)
      {
         List<String> authList = (List<String>) attributeMap.get(HttpHeaders.AUTHORIZATION);
         String authHeader = (authList == null || authList.isEmpty()) ? null : authList.get(0);
         Client client = null;
         try
         {
            client = ClientBuilder.newClient();
            URI targetURI = UriBuilder.fromUri(missionControlURI).path("/token/github").build();
            Response response = client.target(targetURI).request()
                     .header(HttpHeaders.AUTHORIZATION, authHeader)
                     .head();
            if (response.getStatus() == Response.Status.OK.getStatusCode())
            {
               validationMessage = VALIDATION_MESSAGE_OK;
            }
            else
            {
               validationMessage = "GitHub Token does not exist";
            }
         }
         catch (Exception e)
         {
            String message = e.getMessage();
            Throwable root = e;
            while (root.getCause() != null)
            {
               root = root.getCause();
            }
            if (root instanceof UnknownHostException || root instanceof ConnectException)
            {
               validationMessage = "Mission Control is offline and cannot validate if the GitHub token exists";
            }
            else
            {
               if (root.getMessage() != null)
               {
                  message = root.getMessage();
               }
               validationMessage = "Error while validating if the GitHub Token exists: " + message;
            }
         }
         finally
         {
            if (validationMessage != null)
            {
               attributeMap.put("token_github_exists", validationMessage);
            }
            if (client != null)
            {
               client.close();
            }
         }
      }
      if (validationMessage != null && !VALIDATION_MESSAGE_OK.equals(validationMessage))
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
         Client client = null;
         try
         {
            client = ClientBuilder.newClient();
            URI targetURI = UriBuilder.fromUri(missionControlURI).path("/token/openshift").build();
            Response response = client.target(targetURI).request()
                     .header(HttpHeaders.AUTHORIZATION, authHeader)
                     .head();
            if (response.getStatus() == Response.Status.OK.getStatusCode())
            {
               validationMessage = VALIDATION_MESSAGE_OK;
            }
            else
            {
               validationMessage = "OpenShift Token does not exist";
            }
         }
         catch (Exception e)
         {
            String message = e.getMessage();
            Throwable root = e;
            while (root.getCause() != null)
            {
               root = root.getCause();
            }
            if (root instanceof UnknownHostException || root instanceof ConnectException)
            {
               validationMessage = "Mission Control is offline and cannot validate if the OpenShift token exists";
            }
            else
            {
               if (root.getMessage() != null)
               {
                  message = root.getMessage();
               }
               validationMessage = "Error while validating if the OpenShift Token exists: " + message;
            }
         }
         finally
         {
            if (validationMessage != null)
            {
               attributeMap.put("token_openshift_exists", validationMessage);
            }
            if (client != null)
            {
               client.close();
            }
         }
      }
      if (validationMessage != null && !VALIDATION_MESSAGE_OK.equals(validationMessage))
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
         Client client = null;
         try
         {
            client = ClientBuilder.newClient();
            URI targetURI = UriBuilder.fromUri(missionControlURI).path("/repository/" + repository).build();
            Response response = client.target(targetURI).request()
                     .header(HttpHeaders.AUTHORIZATION, authHeader)
                     .head();
            if (response.getStatus() == Response.Status.OK.getStatusCode())
            {
               validationMessage = "GitHub Repository '" + repository + "' already exists";
            }
            else
            {
               validationMessage = VALIDATION_MESSAGE_OK;
            }
         }
         catch (Exception e)
         {
            String message = e.getMessage();
            Throwable root = e;
            while (root.getCause() != null)
            {
               root = root.getCause();
            }
            if (root instanceof UnknownHostException || root instanceof ConnectException)
            {
               validationMessage = "Mission Control is offline and cannot validate the GitHub Repository Name";
            }
            else
            {
               if (root.getMessage() != null)
               {
                  message = root.getMessage();
               }
               validationMessage = "Error while validating GitHub Repository Name: " + message;
            }
         }
         finally
         {
            if (validationMessage != null)
            {
               attributeMap.put("validate_repo_" + repository, validationMessage);
            }
            if (client != null)
            {
               client.close();
            }
         }
      }
      if (validationMessage != null && !VALIDATION_MESSAGE_OK.equals(validationMessage))
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
         Client client = null;
         try
         {
            client = ClientBuilder.newClient();
            URI targetURI = UriBuilder.fromUri(missionControlURI).path("/project/" + project).build();
            Response response = client.target(targetURI).request()
                     .header(HttpHeaders.AUTHORIZATION, authHeader)
                     .head();
            if (response.getStatus() == Response.Status.OK.getStatusCode())
            {
               validationMessage = "OpenShift Project '" + project + "' already exists";
            }
            else
            {
               validationMessage = VALIDATION_MESSAGE_OK;
            }
         }
         catch (Exception e)
         {

            String message = e.getMessage();
            Throwable root = e;
            while (root.getCause() != null)
            {
               root = root.getCause();
            }
            if (root instanceof UnknownHostException || root instanceof ConnectException)
            {
               validationMessage = "Mission Control is offline and cannot validate the OpenShift Project Name";
            }
            else
            {
               if (root.getMessage() != null)
               {
                  message = root.getMessage();
               }
               validationMessage = "Error while validating OpenShift Project Name: " + message;
            }
         }
         finally
         {
            if (validationMessage != null)
            {
               attributeMap.put("validate_project_" + project, validationMessage);
            }
            if (client != null)
            {
               client.close();
            }
         }
      }
      if (validationMessage != null && !VALIDATION_MESSAGE_OK.equals(validationMessage))
      {
         context.addValidationError(context.getCurrentInputComponent(), validationMessage);
      }
   }

   @PostConstruct
   void initializeMissionControlServiceURI()
   {
      String host = System.getProperty(LAUNCHPAD_MISSIONCONTROL_SERVICE_HOST,
               System.getenv(LAUNCHPAD_MISSIONCONTROL_SERVICE_HOST));
      if (host == null)
      {
         host = "mission-control";
      }
      String port = System.getProperty(LAUNCHPAD_MISSIONCONTROL_SERVICE_PORT,
               System.getenv(LAUNCHPAD_MISSIONCONTROL_SERVICE_PORT));
      missionControlURI = UriBuilder.fromPath("/api/validate").host(host).scheme("http")
               .port(port != null ? Integer.parseInt(port) : 80).build();
   }

}
