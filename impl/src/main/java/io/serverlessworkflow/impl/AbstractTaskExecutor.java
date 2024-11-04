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
package io.serverlessworkflow.impl;

import com.fasterxml.jackson.databind.JsonNode;
import io.serverlessworkflow.api.types.TaskBase;

public abstract class AbstractTaskExecutor<T extends TaskBase> implements TaskExecutor<T> {

  protected final T task;
  protected final ExpressionFactory exprFactory;

  protected AbstractTaskExecutor(T task, ExpressionFactory exprFactory) {
    this.task = task;
    this.exprFactory = exprFactory;
  }

  @Override
  public JsonNode apply(JsonNode node) {

    // do input filtering
    return internalExecute(node);
    // do output filtering

  }

  protected abstract JsonNode internalExecute(JsonNode node);
}
