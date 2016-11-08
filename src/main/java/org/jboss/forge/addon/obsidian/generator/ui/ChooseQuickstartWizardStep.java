/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.jboss.forge.addon.obsidian.generator.ui;

import javax.inject.Inject;

import org.jboss.forge.addon.obsidian.generator.model.Quickstart;
import org.jboss.forge.addon.ui.context.UIBuilder;
import org.jboss.forge.addon.ui.context.UIExecutionContext;
import org.jboss.forge.addon.ui.input.UISelectOne;
import org.jboss.forge.addon.ui.metadata.WithAttributes;
import org.jboss.forge.addon.ui.result.Result;
import org.jboss.forge.addon.ui.result.Results;
import org.jboss.forge.addon.ui.wizard.UIWizardStep;

/**
 *
 * @author <a href="mailto:ggastald@redhat.com">George Gastaldi</a>
 */
public class ChooseQuickstartWizardStep implements UIWizardStep
{
   @Inject
   @WithAttributes(label = "Quickstart", required = true)
   private UISelectOne<Quickstart> quickstart;

   @Inject
   private GeneratorModel generatorModel;

   @Override
   public void initializeUI(UIBuilder builder) throws Exception
   {
      quickstart.addValueChangeListener(evt -> generatorModel.setQuickstart((Quickstart) evt.getNewValue()));
      builder.add(quickstart);
   }

   @Override
   public Result execute(UIExecutionContext context) throws Exception
   {
      return Results.success();
   }

}
