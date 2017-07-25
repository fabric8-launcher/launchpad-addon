/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package io.openshift.launchpad;

import java.net.ConnectException;
import java.net.URI;
import java.net.UnknownHostException;

import javax.inject.Singleton;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

/**
 * Facade for the Mission Control component
 * 
 * @author <a href="mailto:ggastald@redhat.com">George Gastaldi</a>
 */
@Singleton
public class MissionControl
{
   private static final String LAUNCHPAD_MISSIONCONTROL_SERVICE_HOST = "LAUNCHPAD_MISSIONCONTROL_SERVICE_HOST";
   private static final String LAUNCHPAD_MISSIONCONTROL_SERVICE_PORT = "LAUNCHPAD_MISSIONCONTROL_SERVICE_PORT";

   public static final String VALIDATION_MESSAGE_OK = "OK";

   private final URI missionControlValidationURI;

   public MissionControl()
   {
      String host = System.getProperty(LAUNCHPAD_MISSIONCONTROL_SERVICE_HOST,
               System.getenv(LAUNCHPAD_MISSIONCONTROL_SERVICE_HOST));
      if (host == null)
      {
         host = "mission-control";
      }
      String port = System.getProperty(LAUNCHPAD_MISSIONCONTROL_SERVICE_PORT,
               System.getenv(LAUNCHPAD_MISSIONCONTROL_SERVICE_PORT));
      missionControlValidationURI = UriBuilder.fromPath("/api/validate").host(host).scheme("http")
               .port(port != null ? Integer.parseInt(port) : 80).build();
   }

   /**
    * Validates if the OpenShift project exists
    * 
    * @param authHeader
    * @param project
    * @return a validation message, returns {@link #VALIDATION_MESSAGE_OK} if the project does not exist
    */
   public String validateOpenShiftProjectExists(String authHeader, String project)
   {
      String validationMessage;
      try
      {
         URI targetURI = UriBuilder.fromUri(missionControlValidationURI).path("/project/" + project).build();
         if (head(targetURI, authHeader) == Response.Status.OK.getStatusCode())
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
         Throwable root = rootException(e);
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
      return validationMessage;
   }

   public String validateGitHubRepositoryExists(String authHeader, String repository)
   {
      String validationMessage;
      try
      {
         URI targetURI = UriBuilder.fromUri(missionControlValidationURI).path("/repository/" + repository).build();
         if (head(targetURI, authHeader) == Response.Status.OK.getStatusCode())
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
         Throwable root = rootException(e);
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
      return validationMessage;
   }

   public String validateOpenShiftTokenExists(String authHeader)
   {
      String validationMessage;
      try
      {
         URI targetURI = UriBuilder.fromUri(missionControlValidationURI).path("/token/openshift").build();
         if (head(targetURI, authHeader) == Response.Status.OK.getStatusCode())
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
         Throwable root = rootException(e);
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
      return validationMessage;
   }

   public String validateGitHubTokenExists(String authHeader)
   {
      String validationMessage;
      try
      {
         URI targetURI = UriBuilder.fromUri(missionControlValidationURI).path("/token/github").build();
         if (head(targetURI, authHeader) == Response.Status.OK.getStatusCode())
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
         Throwable root = rootException(e);
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
      return validationMessage;
   }

   private Throwable rootException(Exception e)
   {
      Throwable root = e;
      while (root.getCause() != null)
      {
         root = root.getCause();
      }
      return root;
   }

   private int head(URI targetURI, String authHeader) throws ProcessingException
   {
      Client client = null;
      try
      {
         client = ClientBuilder.newClient();
         Response response = client.target(targetURI).request()
                  .header(HttpHeaders.AUTHORIZATION, authHeader)
                  .head();
         return response.getStatus();
      }
      finally
      {
         if (client != null)
         {
            client.close();
         }
      }
   }
}