/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.jboss.forge.addon.obsidian.generator.ui;

import javax.inject.Inject;

import org.jboss.forge.addon.obsidian.generator.model.Feature;
import org.jboss.forge.addon.ui.context.UIBuilder;
import org.jboss.forge.addon.ui.context.UIExecutionContext;
import org.jboss.forge.addon.ui.input.UISelectMany;
import org.jboss.forge.addon.ui.metadata.WithAttributes;
import org.jboss.forge.addon.ui.result.Result;
import org.jboss.forge.addon.ui.result.Results;
import org.jboss.forge.addon.ui.wizard.UIWizardStep;
import org.jboss.forge.furnace.util.Lists;

/**
 *
 * @author <a href="mailto:ggastald@redhat.com">George Gastaldi</a>
 */
public class ChooseFeaturesWizardStep implements UIWizardStep
{
   @Inject
   @WithAttributes(label = "Features", required = true)
   private UISelectMany<Feature> features;

   @Inject
   private GeneratorModel generatorModel;

   @SuppressWarnings("unchecked")
   @Override
   public void initializeUI(UIBuilder builder) throws Exception
   {
      features.addValueChangeListener(
               evt -> generatorModel.setFeatures(Lists.toList((Iterable<Feature>) evt.getNewValue())));
      builder.add(features);

   }

   @Override
   public Result execute(UIExecutionContext context) throws Exception
   {
      return Results.success();
   }

}
