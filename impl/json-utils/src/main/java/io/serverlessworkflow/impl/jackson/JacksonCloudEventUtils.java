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
package io.serverlessworkflow.impl.jackson;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
import io.cloudevents.CloudEvent;
import io.cloudevents.CloudEventData;
import io.cloudevents.core.provider.EventFormatProvider;
import io.cloudevents.jackson.JsonCloudEventData;
import io.cloudevents.jackson.JsonFormat;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Date;

public class JacksonCloudEventUtils {

  public static JsonNode toJsonNode(CloudEvent event) {
    if (event == null) {
      return NullNode.instance;
    }
    // Delegate entirely to the official CloudEvents SDK
    byte[] serialized =
        EventFormatProvider.getInstance().resolveFormat(JsonFormat.CONTENT_TYPE).serialize(event);
    try {
      return JsonUtils.mapper().readTree(serialized);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  public static OffsetDateTime toOffset(Date date) {
    return date.toInstant().atOffset(ZoneOffset.UTC);
  }

  public static JsonNode toJsonNode(CloudEventData data) {
    if (data == null) {
      return NullNode.instance;
    }
    try {
      return data instanceof JsonCloudEventData
          ? ((JsonCloudEventData) data).getNode()
          : JsonUtils.mapper().readTree(data.toBytes());
    } catch (IOException io) {
      throw new UncheckedIOException(io);
    }
  }

  public static CloudEvent toCloudEvent(JsonNode node) {
    if (node == null || node.isNull()) {
      return null;
    }
    try {
      byte[] ceBytes = JsonUtils.mapper().writeValueAsBytes(node);
      return EventFormatProvider.getInstance()
          .resolveFormat(JsonFormat.CONTENT_TYPE)
          .deserialize(ceBytes);
    } catch (JsonProcessingException e) {
      throw new IllegalArgumentException("Failed to deserialize JsonNode to CloudEvent", e);
    }
  }

  public static CloudEventData toCloudEventData(JsonNode node) {
    if (node == null || node.isNull()) {
      return null;
    }
    return JsonCloudEventData.wrap(node);
  }

  private JacksonCloudEventUtils() {}
}
