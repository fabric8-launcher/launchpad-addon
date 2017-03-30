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

import freemarker.cache.ClassTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import io.openshift.launchpad.ui.starter.PerformExtraTasksStep;

import org.junit.Test;

/**
 *
 * @author <a href="mailto:ggastald@redhat.com">George Gastaldi</a>
 */
public class TemplateReadmeTest
{
   @Test
   public void testTemplateProcessing() throws Exception
   {
      Configuration config = new Configuration();
      config.setTemplateLoader(new ClassTemplateLoader(PerformExtraTasksStep.class, ""));
      Map<String, Object> vars = new HashMap<>();
      vars.put("name", "demo");
      vars.put("type", "vert.x");
      Template template = config.getTemplate("README.md.ftl");
      template.process(vars, new PrintWriter(System.out));
      vars.put("type", "spring-boot");
      template.process(vars, new PrintWriter(System.out));
   }
}
