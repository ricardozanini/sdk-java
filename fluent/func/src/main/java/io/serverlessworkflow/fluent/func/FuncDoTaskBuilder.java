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
package io.serverlessworkflow.fluent.func;

import io.serverlessworkflow.fluent.func.spi.ConditionalTaskBuilder;
import io.serverlessworkflow.fluent.func.spi.FuncDoFluent;
import io.serverlessworkflow.fluent.func.spi.FuncTransformations;
import io.serverlessworkflow.fluent.spec.BaseDoTaskBuilder;
import java.util.function.Consumer;

public class FuncDoTaskBuilder extends BaseDoTaskBuilder<FuncDoTaskBuilder, FuncTaskItemListBuilder>
    implements FuncTransformations<FuncDoTaskBuilder>,
        ConditionalTaskBuilder<FuncDoTaskBuilder>,
        FuncDoFluent<FuncDoTaskBuilder> {

  public FuncDoTaskBuilder() {
    super(new FuncTaskItemListBuilder());
  }

  @Override
  public FuncDoTaskBuilder self() {
    return this;
  }

  @Override
  public FuncDoTaskBuilder emit(String name, Consumer<FuncEmitTaskBuilder> itemsConfigurer) {
    this.listBuilder().emit(name, itemsConfigurer);
    return this;
  }

  @Override
  public FuncDoTaskBuilder forEach(String name, Consumer<FuncForTaskBuilder> itemsConfigurer) {
    this.listBuilder().forEach(name, itemsConfigurer);
    return this;
  }

  @Override
  public FuncDoTaskBuilder set(String name, Consumer<FuncSetTaskBuilder> itemsConfigurer) {
    this.listBuilder().set(name, itemsConfigurer);
    return this;
  }

  @Override
  public FuncDoTaskBuilder set(String name, String expr) {
    this.listBuilder().set(name, expr);
    return this;
  }

  @Override
  public FuncDoTaskBuilder switchCase(
      String name, Consumer<FuncSwitchTaskBuilder> itemsConfigurer) {
    this.listBuilder().switchCase(name, itemsConfigurer);
    return this;
  }

  @Override
  public FuncDoTaskBuilder callFn(String name, Consumer<FuncCallTaskBuilder> cfg) {
    this.listBuilder().callFn(name, cfg);
    return this;
  }

  @Override
  public FuncDoTaskBuilder fork(String name, Consumer<FuncForkTaskBuilder> itemsConfigurer) {
    this.listBuilder().fork(name, itemsConfigurer);
    return this;
  }
}
