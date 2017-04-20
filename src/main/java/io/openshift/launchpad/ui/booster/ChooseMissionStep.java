/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package io.openshift.launchpad.ui.booster;

import java.util.Iterator;
import java.util.Map;

import javax.inject.Inject;

import org.jboss.forge.addon.ui.context.UIBuilder;
import org.jboss.forge.addon.ui.context.UIContext;
import org.jboss.forge.addon.ui.context.UIExecutionContext;
import org.jboss.forge.addon.ui.context.UINavigationContext;
import org.jboss.forge.addon.ui.input.UISelectOne;
import org.jboss.forge.addon.ui.metadata.UICommandMetadata;
import org.jboss.forge.addon.ui.metadata.WithAttributes;
import org.jboss.forge.addon.ui.result.NavigationResult;
import org.jboss.forge.addon.ui.result.Result;
import org.jboss.forge.addon.ui.result.Results;
import org.jboss.forge.addon.ui.result.navigation.NavigationResultBuilder;
import org.jboss.forge.addon.ui.util.Categories;
import org.jboss.forge.addon.ui.util.Metadata;
import org.jboss.forge.addon.ui.wizard.UIWizardStep;

import io.openshift.launchpad.catalog.BoosterCatalogService;
import io.openshift.launchpad.catalog.Mission;

/**
 *
 * @author <a href="mailto:ggastald@redhat.com">George Gastaldi</a>
 */
public class ChooseMissionStep implements UIWizardStep
{
   @Inject
   @WithAttributes(label = "Mission", required = true)
   private UISelectOne<Mission> mission;

   @Inject
   private BoosterCatalogService catalogService;

   @Override
   public void initializeUI(UIBuilder builder) throws Exception
   {
      if (builder.getUIContext().getProvider().isGUI())
      {
         mission.setItemLabelConverter(Mission::getName);
      }
      else
      {
         mission.setItemLabelConverter(Mission::getId);
      }
      mission.setValueChoices(catalogService.getMissions());
      mission.setDefaultValue(() -> {
         Iterator<Mission> iterator = mission.getValueChoices().iterator();
         if (iterator.hasNext())
         {
            return iterator.next();
         }
         else
         {
            return null;
         }
      });
      builder.add(mission);
   }

   @Override
   public NavigationResult next(UINavigationContext context) throws Exception
   {
      if (mission.getValue() != null)
      {
         Map<Object, Object> attributeMap = context.getUIContext().getAttributeMap();
         attributeMap.put(Mission.class, mission.getValue());
      }
      NavigationResultBuilder builder = NavigationResultBuilder.create();
      builder.add(ChooseRuntimeStep.class);
      return builder.build();
   }

   @Override
   public UICommandMetadata getMetadata(UIContext context)
   {
      return Metadata.forCommand(getClass()).name("Mission")
               .description("Choose the Mission")
               .category(Categories.create("Openshift.io"));
   }

   @Override
   public Result execute(UIExecutionContext arg0) throws Exception
   {
      return Results.success();
   }

}
