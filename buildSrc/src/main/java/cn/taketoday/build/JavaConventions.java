/*
 * Copyright 2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cn.taketoday.build;

import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.plugins.JavaBasePlugin;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.jvm.toolchain.JavaLanguageVersion;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import cn.taketoday.build.optional.OptionalDependenciesPlugin;

public class JavaConventions {

  private static final List<String> COMPILER_ARGS;

  private static final List<String> TEST_COMPILER_ARGS;

  static {
    List<String> commonCompilerArgs = List.of(
            /*"-Xlint:serial",*/ "-Xlint:cast", "-Xlint:classfile",/* "-Xlint:dep-ann",*/
            "-Xlint:divzero", "-Xlint:empty", "-Xlint:finally", "-Xlint:overrides",
            "-Xlint:path", "-Xlint:-processing", /* "-Xlint:static", "-Xlint:try",*/ "-Xlint:-options",
            "-parameters"
    );
    COMPILER_ARGS = new ArrayList<>();
    COMPILER_ARGS.addAll(commonCompilerArgs);
    COMPILER_ARGS.addAll(Arrays.asList(
            /* "-Xlint:varargs",*/ "-Xlint:fallthrough" // , "-Xlint:rawtypes" // "-Xlint:deprecation",
            // "-Xlint:unchecked"/*, "-Werror"*/
    ));
    TEST_COMPILER_ARGS = new ArrayList<>();
    TEST_COMPILER_ARGS.addAll(commonCompilerArgs);
    TEST_COMPILER_ARGS.addAll(Arrays.asList("-Xlint:-varargs", "-Xlint:-fallthrough", "-Xlint:-rawtypes",
            "-Xlint:-deprecation", "-Xlint:-unchecked"));
  }

  public void apply(Project project) {
    project.getPlugins().withType(JavaBasePlugin.class, javaPlugin -> {
      applyJavaCompileConventions(project);
      configureDependencyManagement(project);
    });
  }

  /**
   * Applies the common Java compiler options for main sources, test fixture sources, and
   * test sources.
   *
   * @param project the current project
   */
  private void applyJavaCompileConventions(Project project) {
    project.getExtensions().getByType(JavaPluginExtension.class)
            .getToolchain().getLanguageVersion().set(JavaLanguageVersion.of(17));
    project.getTasks().withType(JavaCompile.class)
            .matching(compileTask -> compileTask.getName().equals(JavaPlugin.COMPILE_JAVA_TASK_NAME))
            .forEach(compileTask -> {
              compileTask.getOptions().setCompilerArgs(COMPILER_ARGS);
              compileTask.getOptions().setEncoding("UTF-8");
            });
    project.getTasks().withType(JavaCompile.class)
            .matching(compileTask -> compileTask.getName().equals(JavaPlugin.COMPILE_TEST_JAVA_TASK_NAME)
                    || compileTask.getName().equals("compileTestFixturesJava"))
            .forEach(compileTask -> {
              compileTask.getOptions().setCompilerArgs(TEST_COMPILER_ARGS);
              compileTask.getOptions().setEncoding("UTF-8");
            });
  }

  private void configureDependencyManagement(Project project) {
    ConfigurationContainer configurations = project.getConfigurations();
    Configuration dependencyManagement = configurations.create("dependencyManagement", (configuration) -> {
      configuration.setVisible(false);
      configuration.setCanBeConsumed(false);
      configuration.setCanBeResolved(false);
    });

    configurations.matching(configuration -> {
              String name = configuration.getName();
              return name.endsWith("Classpath")
                      || JavaPlugin.ANNOTATION_PROCESSOR_CONFIGURATION_NAME.equals(name)
                      || JavaPlugin.TEST_ANNOTATION_PROCESSOR_CONFIGURATION_NAME.equals(name);
            })
            .forEach(configuration -> configuration.extendsFrom(dependencyManagement));

    project.getPlugins().withType(OptionalDependenciesPlugin.class,
            optionalDependencies -> configurations.getByName(OptionalDependenciesPlugin.OPTIONAL_CONFIGURATION_NAME)
                    .extendsFrom(dependencyManagement));
  }

}
