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
package io.serverlessworkflow.fluent.agentic;

import static dev.langchain4j.agentic.internal.AgentUtil.agentsToExecutors;

import dev.langchain4j.agentic.cognisphere.Cognisphere;
import dev.langchain4j.agentic.internal.AgentExecutor;
import dev.langchain4j.agentic.internal.AgentInstance;
import io.serverlessworkflow.impl.expressions.LoopPredicateIndex;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

public final class AgentAdapters {

  private AgentAdapters() {}

  public static List<AgentExecutor> toExecutors(Object... agents) {
    return agentsToExecutors(Stream.of(agents).map(AgentInstance.class::cast).toArray());
  }

  public static Function<Cognisphere, Object> toFunction(AgentExecutor exec) {
    return exec::invoke;
  }

  public static LoopPredicateIndex<Object, Object> toWhile(Predicate<Cognisphere> exit) {
    return (model, item, idx) -> !exit.test((Cognisphere) model);
  }
}
