/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.obsidiantoaster.generator.ui.input;

import javax.inject.Inject;

import org.jboss.forge.addon.ui.input.AbstractUIInputDecorator;
import org.jboss.forge.addon.ui.input.UIInput;
import org.jboss.forge.addon.ui.metadata.WithAttributes;

/**
 *
 * @author <a href="mailto:ggastald@redhat.com">George Gastaldi</a>
 */
public class Version extends AbstractUIInputDecorator<String>
{
   @Inject
   @WithAttributes(label = "Project version", required = true, defaultValue = "1.0.0-SNAPSHOT")
   private UIInput<String> version;

   @Override
   protected UIInput<String> createDelegate()
   {
      return version;
   }
}