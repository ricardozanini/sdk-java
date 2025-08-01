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
package io.serverlessworkflow.generator;

import com.fasterxml.jackson.databind.JsonNode;
import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JFieldVar;
import io.serverlessworkflow.annotations.AdditionalProperties;
import jakarta.validation.constraints.Pattern;
import org.jsonschema2pojo.AbstractAnnotator;
import org.jsonschema2pojo.GenerationConfig;

public class CustomAnnotator extends AbstractAnnotator {

  private static final String CONST = "const";

  public CustomAnnotator(GenerationConfig generationConfig) {
    super(generationConfig);
  }

  @Override
  public void additionalPropertiesField(JFieldVar field, JDefinedClass clazz, String propertyName) {
    clazz.annotate(AdditionalProperties.class);
  }

  @Override
  public void propertyField(
      JFieldVar field, JDefinedClass clazz, String propertyName, JsonNode propertyNode) {
    if (propertyNode.has(CONST)) {
      field.annotate(Pattern.class).param("regexp", propertyNode.get(CONST).asText());
    }
  }
}
