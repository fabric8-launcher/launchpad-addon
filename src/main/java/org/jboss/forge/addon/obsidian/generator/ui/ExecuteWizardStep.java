/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.jboss.forge.addon.obsidian.generator.ui;

import javax.inject.Inject;

import org.jboss.forge.addon.ui.context.UIExecutionContext;
import org.jboss.forge.addon.ui.result.Result;
import org.jboss.forge.addon.ui.result.Results;
import org.jboss.forge.addon.ui.wizard.UIWizardStep;

/**
 * Perform the execution
 * 
 * @author <a href="mailto:ggastald@redhat.com">George Gastaldi</a>
 */
public class ExecuteWizardStep implements UIWizardStep
{
   @Inject
   private GeneratorModel generatorModel;

   @Override
   public Result execute(UIExecutionContext context) throws Exception
   {
      System.out.println(generatorModel);
      return Results.success();
   }
}
