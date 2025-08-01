/*
 * Copyright 2020-Present The Serverless Workflow Specification Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.serverless.workflow.impl;

import static org.assertj.core.api.Assertions.assertThat;

import io.serverlessworkflow.api.types.Document;
import io.serverlessworkflow.api.types.FlowDirective;
import io.serverlessworkflow.api.types.FlowDirectiveEnum;
import io.serverlessworkflow.api.types.ForTaskConfiguration;
import io.serverlessworkflow.api.types.SwitchItem;
import io.serverlessworkflow.api.types.SwitchTask;
import io.serverlessworkflow.api.types.Task;
import io.serverlessworkflow.api.types.TaskItem;
import io.serverlessworkflow.api.types.TaskMetadata;
import io.serverlessworkflow.api.types.Workflow;
import io.serverlessworkflow.api.types.func.CallJava;
import io.serverlessworkflow.api.types.func.CallTaskJava;
import io.serverlessworkflow.api.types.func.ForTaskFunction;
import io.serverlessworkflow.api.types.func.SwitchCaseFunction;
import io.serverlessworkflow.impl.WorkflowApplication;
import io.serverlessworkflow.impl.WorkflowDefinition;
import io.serverlessworkflow.impl.expressions.TaskMetadataKeys;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.function.Predicate;
import org.junit.jupiter.api.Test;

class CallTest {

  @Test
  void testJavaFunction() throws InterruptedException, ExecutionException {
    try (WorkflowApplication app = WorkflowApplication.builder().build()) {
      Workflow workflow =
          new Workflow()
              .withDocument(
                  new Document().withNamespace("test").withName("testJavaCall").withVersion("1.0"))
              .withDo(
                  List.of(
                      new TaskItem(
                          "javaCall",
                          new Task()
                              .withCallTask(
                                  new CallTaskJava(CallJava.function(JavaFunctions::getName))))));

      assertThat(
              app.workflowDefinition(workflow)
                  .instance(new Person("Francisco", 33))
                  .start()
                  .get()
                  .asText()
                  .orElseThrow())
          .isEqualTo("Francisco Javierito");
    }
  }

  @Test
  void testForLoop() throws InterruptedException, ExecutionException {
    try (WorkflowApplication app = WorkflowApplication.builder().build()) {
      ForTaskConfiguration forConfig = new ForTaskConfiguration();
      Workflow workflow =
          new Workflow()
              .withDocument(
                  new Document().withNamespace("test").withName("testLoop").withVersion("1.0"))
              .withDo(
                  List.of(
                      new TaskItem(
                          "forLoop",
                          new Task()
                              .withForTask(
                                  new ForTaskFunction()
                                      .withWhile(CallTest::isEven)
                                      .withCollection(v -> (Collection) v)
                                      .withFor(forConfig)
                                      .withDo(
                                          List.of(
                                              new TaskItem(
                                                  "javaCall",
                                                  new Task()
                                                      .withCallTask(
                                                          new CallTaskJava(
                                                              CallJava.loopFunction(
                                                                  CallTest::sum,
                                                                  forConfig.getEach()))))))))));

      assertThat(
              app.workflowDefinition(workflow)
                  .instance(List.of(2, 4, 6, 7))
                  .start()
                  .get()
                  .asNumber()
                  .orElseThrow())
          .isEqualTo(12);
    }
  }

  @Test
  void testSwitch() throws InterruptedException, ExecutionException {
    try (WorkflowApplication app = WorkflowApplication.builder().build()) {
      Workflow workflow =
          new Workflow()
              .withDocument(
                  new Document().withNamespace("test").withName("testSwith").withVersion("1.0"))
              .withDo(
                  List.of(
                      new TaskItem(
                          "switch",
                          new Task()
                              .withSwitchTask(
                                  new SwitchTask()
                                      .withSwitch(
                                          List.of(
                                              new SwitchItem(
                                                  "odd",
                                                  new SwitchCaseFunction()
                                                      .withPredicate(CallTest::isOdd)
                                                      .withThen(
                                                          new FlowDirective()
                                                              .withFlowDirectiveEnum(
                                                                  FlowDirectiveEnum.END))))))),
                      new TaskItem(
                          "java",
                          new Task()
                              .withCallTask(new CallTaskJava(CallJava.function(CallTest::zero))))));

      WorkflowDefinition definition = app.workflowDefinition(workflow);
      assertThat(definition.instance(3).start().get().asNumber().orElseThrow()).isEqualTo(3);
      assertThat(definition.instance(4).start().get().asNumber().orElseThrow()).isEqualTo(0);
    }
  }

  @Test
  void testIf() throws InterruptedException, ExecutionException {
    try (WorkflowApplication app = WorkflowApplication.builder().build()) {
      Workflow workflow =
          new Workflow()
              .withDocument(
                  new Document().withNamespace("test").withName("testIf").withVersion("1.0"))
              .withDo(
                  List.of(
                      new TaskItem(
                          "java",
                          new Task()
                              .withCallTask(
                                  new CallTaskJava(
                                      withPredicate(
                                          CallJava.function(CallTest::zero), CallTest::isOdd))))));
      WorkflowDefinition definition = app.workflowDefinition(workflow);
      assertThat(definition.instance(3).start().get().asNumber().orElseThrow()).isEqualTo(0);
      assertThat(definition.instance(4).start().get().asNumber().orElseThrow()).isEqualTo(4);
    }
  }

  private <T> CallJava withPredicate(CallJava call, Predicate<T> pred) {
    return (CallJava)
        call.withMetadata(
            new TaskMetadata().withAdditionalProperty(TaskMetadataKeys.IF_PREDICATE, pred));
  }

  public static boolean isEven(Object model, Integer number) {
    return !isOdd(number);
  }

  public static boolean isOdd(Integer number) {
    return number % 2 != 0;
  }

  public static int zero(Integer value) {
    return 0;
  }

  public static Integer sum(Object model, Integer item) {
    return model instanceof Collection ? item : (Integer) model + item;
  }
}
