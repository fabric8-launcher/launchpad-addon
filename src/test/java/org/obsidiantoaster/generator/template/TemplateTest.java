/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.obsidiantoaster.generator.template;

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.obsidiantoaster.generator.ui.starter.PerformExtraTasksStep;

import freemarker.cache.ClassTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;

/**
 *
 * @author <a href="mailto:ggastald@redhat.com">George Gastaldi</a>
 */
public class TemplateTest
{
   @Test
   public void testTemplateProcessing() throws Exception
   {
      Configuration config = new Configuration();
      config.setTemplateLoader(new ClassTemplateLoader(PerformExtraTasksStep.class, ""));
      Map<String, Object> vars = new HashMap<>();
      vars.put("name", "demo");
      vars.put("type", "vert.x");
      Template template = config.getTemplate("route.yml.ftl");
      template.process(vars, new PrintWriter(System.out));
   }
}
