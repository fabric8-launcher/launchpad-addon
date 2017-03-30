/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package io.openshift.launchpad.ui.input;

import java.util.regex.Pattern;

import javax.inject.Inject;

import org.hibernate.validator.constraints.Length;
import org.hibernate.validator.valuehandling.UnwrapValidatedValue;
import org.jboss.forge.addon.ui.input.AbstractUIInputDecorator;
import org.jboss.forge.addon.ui.input.UIInput;
import org.jboss.forge.addon.ui.metadata.WithAttributes;

/**
 * The project name input
 * 
 * @author <a href="mailto:ggastald@redhat.com">George Gastaldi</a>
 */
public class ProjectName extends AbstractUIInputDecorator<String>
{
   private static final Pattern SPECIAL_CHARS = Pattern.compile(".*[^-_.a-zA-Z0-9].*");

   @Inject
   @WithAttributes(label = "Project name", required = true, defaultValue = "demo", note = "Downloadable project zip and application jar are based on the project name")
   @UnwrapValidatedValue
   @Length(min = 1, max = 24)
   private UIInput<String> named;

   @Override
   protected UIInput<String> createDelegate()
   {
      named.addValidator(context -> {
         if (named.getValue() != null && SPECIAL_CHARS.matcher(named.getValue()).matches())
            context.addValidationError(named,
                     "Project name must not contain spaces or special characters.");
      }).setDescription("The following characters are accepted: -_.a-zA-Z0-9");
      return named;
   }

}
