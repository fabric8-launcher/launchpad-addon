/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.jboss.forge.addon.obsidian.generator.api;

import org.jboss.forge.addon.obsidian.generator.ui.GeneratorModel;
import org.jboss.shrinkwrap.api.Archive;

/**
 *
 * @author <a href="mailto:ggastald@redhat.com">George Gastaldi</a>
 */
public interface ProjectGenerator
{
   Archive<?> generate(GeneratorModel model);
}
