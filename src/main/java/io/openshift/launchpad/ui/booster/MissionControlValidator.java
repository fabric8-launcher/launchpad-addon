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

   private URI missionControlURI;

   public void validateGitHubRepositoryExists(UIValidationContext context, String repository)
   {
      Map<Object, Object> attributeMap = context.getUIContext().getAttributeMap();
      Boolean result = (Boolean) attributeMap.get("validate_repo_" + repository);
      if (result == null)
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
            result = response.getStatus() == Response.Status.OK.getStatusCode();
            if (result)
            {
               context.addValidationError(context.getCurrentInputComponent(),
                        "GitHub Repository '" + repository + "' already exists");
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
               context.addValidationError(context.getCurrentInputComponent(),
                        "Mission Control is offline and cannot validate the GitHub Repository Name");
            }
            else
            {
               if (root.getMessage() != null)
               {
                  message = root.getMessage();
               }
               context.addValidationError(context.getCurrentInputComponent(),
                        "Error while validating GitHub Repository Name: " + message);
            }
         }
         finally
         {
            if (result != null)
            {
               attributeMap.put("validate_repo_" + repository, result);
            }
            if (client != null)
            {
               client.close();
            }
         }
      }
   }

   public void validateOpenShiftProjectExists(UIValidationContext context, String project)
   {
      Map<Object, Object> attributeMap = context.getUIContext().getAttributeMap();
      Boolean result = (Boolean) attributeMap.get("validate_project_" + project);
      if (result == null)
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
            result = response.getStatus() == Response.Status.OK.getStatusCode();
            if (result)
            {
               context.addValidationError(context.getCurrentInputComponent(),
                        "OpenShift Project '" + project + "' already exists");
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
               context.addValidationError(context.getCurrentInputComponent(),
                        "Mission Control is offline and cannot validate the OpenShift Project Name");
               result = false;
            }
            else
            {
               if (root.getMessage() != null)
               {
                  message = root.getMessage();
               }
               context.addValidationError(context.getCurrentInputComponent(),
                        "Error while validating OpenShift Project Name: " + message);
            }
         }
         finally
         {
            if (result != null)
            {
               attributeMap.put("validate_project_" + project, result);
            }
            if (client != null)
            {
               client.close();
            }
         }
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
