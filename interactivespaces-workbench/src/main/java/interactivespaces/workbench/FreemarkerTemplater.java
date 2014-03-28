/*
 * Copyright (C) 2012 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package interactivespaces.workbench;

import com.google.common.collect.Lists;
import com.google.common.io.Closeables;
import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.Template;
import interactivespaces.InteractiveSpacesException;
import interactivespaces.SimpleInteractiveSpacesException;
import interactivespaces.service.template.Templater;
import interactivespaces.util.io.FileSupport;
import interactivespaces.util.io.FileSupportImpl;
import interactivespaces.util.resource.ManagedResource;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.List;
import java.util.Map;

/**
 * A templater using Freemarker.
 *
 * <p>
 * This implementation supports the concpet of multiple evaluation passes. This is
 * useful when cascading defintitions need to be resolved in the output.  Say, for a project defintiion, there is
 * a concept of {@code packageName=${directoryName}.${className}} and then the template itself outputs
 * {@code <packageName>${packageName}</packageName>}, then the first pass will resolve packageName, and the second
 * pass will resolve {$directoryName} and ${className}. Since this is a templating language, evaluations are not
 * recursive, and so this is necessary to properly handle the output.
 *
 * @author Keith M. Hughes
 */
public class FreemarkerTemplater implements ManagedResource {

  /**
   * Base directory where templates are kept.
   */
  public static final File TEMPLATE_LOCATION = new File("templates");

  /**
   * File support instance to use.
   */
  private FileSupport fileSupport = FileSupportImpl.INSTANCE;

  /**
   * The configuration used by Freemarker.
   */
  private Configuration freemarkerConfig;

  /**
   * Number of evaluation passes to apply to template output. See class documentation for an explanation of what
   * this field is fore.
   */
  private int evaluationPasses = 1;

  @Override
  public void startup() {
    try {
      freemarkerConfig = new Configuration();
      freemarkerConfig.setDirectoryForTemplateLoading(TEMPLATE_LOCATION);
      // Specify how templates will see the data-model. This is an
      // advanced topic... but just use this:
      freemarkerConfig.setObjectWrapper(new DefaultObjectWrapper());
    } catch (IOException e) {
      throw new InteractiveSpacesException("Cannot initialize activity project creator", e);
    }
  }

  @Override
  public void shutdown() {
    // Nothing to be done on shutdown.
  }

  /**
   * Set the number of evaluation passes to apply. See class documentation for details about this feature.
   *
   * @param evaluationPasses
   *          number of passes to apply
   */
  public void setEvaluationPasses(int evaluationPasses) {
    this.evaluationPasses = evaluationPasses;
  }

  /**
   * Process a string template.
   *
   *
   * @param data
   *          data for template
   * @param templateContent
   *          string template to process
   * @param defineResult
   *          target value to define with new value, or {@code null} if no definition should take place
   *
   * @return processed template
   */
  public String processStringTemplate(Map<String, Object> data, String templateContent, String defineResult) {
    for (int passesRemaining = evaluationPasses; passesRemaining > 0; passesRemaining--) {
      templateContent = processStringTemplateCore(data, templateContent);
      if (defineResult != null) {
        data.put(defineResult, templateContent);
      }
    }
    return templateContent;
  }

  /**
   * Process a string template.
   *
   *
   * @param data
   *          data for template
   * @param templateContent
   *          string template to process
   *
   * @return processed template
   */
  public String processStringTemplateCore(Map<String, Object> data, String templateContent) {
    try {
      Template temp = new Template("generator for " + templateContent,
          new StringReader(templateContent), freemarkerConfig);
      StringWriter stringWriter = new StringWriter();
      temp.process(data, stringWriter);
      return stringWriter.toString();
    } catch (Exception e) {
      throw new InteractiveSpacesException(
          String.format("Could not instantiate string template %s", templateContent), e);
    }
  }

  /**
   * Write out the template.
   *
   * @param data
   *          data for the template
   * @param outputFile
   *          file where the template will be written
   * @param template
   *          which template to use
   */
  public void writeTemplate(Map<String, Object> data, File outputFile, String template) {
    fileSupport.directoryExists(outputFile.getParentFile());

    List<File> deleteList = Lists.newArrayList();
    File tempFile = new File(String.format("%s.%d", outputFile.getAbsolutePath(), evaluationPasses));
    deleteList.add(tempFile);
    File inputFile = new File(template);
    if (inputFile.isAbsolute()) {
      FileSupportImpl.INSTANCE.copyFile(inputFile, tempFile);
    }
    for (int passesRemaining = evaluationPasses; passesRemaining > 0; passesRemaining--) {
      tempFile = new File(String.format("%s.%d", outputFile.getAbsolutePath(), passesRemaining - 1));
      deleteList.add(tempFile);
      if (passesRemaining == 1) {
        writeTemplateCore(data, outputFile, template);
      } else {
        writeTemplateCore(data, tempFile, template);
        template = tempFile.getAbsolutePath();
      }
    }

    // By design. if there are any errors processing the templates, these files will remain.
    for (File toDelete : deleteList) {
      FileSupportImpl.INSTANCE.delete(toDelete);
    }
  }

  /**
   * Write out the template.
   *
   * @param data
   *          data for the template
   * @param outputFile
   *          file where the template will be written
   * @param template
   *          which template to use
   */
  private void writeTemplateCore(Map<String, Object> data, File outputFile, String template) {
    Writer out = null;
    Reader in = null;
    try {
      Template temp;
      if (template.startsWith("/")) {
        in = new FileReader(template);
        temp = new Template(template, in, freemarkerConfig);
      } else {
        temp = freemarkerConfig.getTemplate(template);
      }

      out = new FileWriter(outputFile);
      temp.process(data, out);
      out.close();
    } catch (Exception e) {
      throw new SimpleInteractiveSpacesException(String.format("Could not instantiate template %s to %s",
          template, outputFile.getAbsolutePath()), e);
    } finally {
      Closeables.closeQuietly(out);
      Closeables.closeQuietly(in);
    }
  }
}
