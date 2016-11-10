/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.jboss.forge.addon.obsidian.generator.ui;

import javax.inject.Inject;

import org.jboss.forge.addon.obsidian.generator.model.LanguageRuntime;
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
import org.jboss.forge.addon.ui.wizard.UIWizard;

/**
 *
 * @author <a href="mailto:ggastald@redhat.com">George Gastaldi</a>
 */
public class ChooseLanguageRuntimeWizard implements UIWizard
{
   @Inject
   @WithAttributes(label = "Language Runtime", required = true)
   private UISelectOne<LanguageRuntime> languageRuntime;

   @Inject
   private GeneratorModel generatorModel;

   @Override
   public void initializeUI(UIBuilder builder) throws Exception
   {
      languageRuntime
               .addValueChangeListener((evt) -> generatorModel.setLanguageRuntime((LanguageRuntime) evt.getNewValue()));
      builder.add(languageRuntime);
   }

   @Override
   public UICommandMetadata getMetadata(UIContext context)
   {
      return Metadata.forCommand(getClass()).name("Obsidian: New Project")
               .description("Choose your language runtime")
               .category(Categories.create("Obsidian"));
   }

   @Override
   public NavigationResult next(UINavigationContext context) throws Exception
   {
      return NavigationResultBuilder.create().add(ChooseFeaturesWizardStep.class).build();
   }

   @Override
   public Result execute(UIExecutionContext context) throws Exception
   {
      return Results.success();
   }

}
