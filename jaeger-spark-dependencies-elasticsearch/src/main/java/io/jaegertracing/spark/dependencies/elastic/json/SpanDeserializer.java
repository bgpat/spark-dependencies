/**
 * Copyright 2017 The Jaeger Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package io.jaegertracing.spark.dependencies.elastic.json;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import io.jaegertracing.spark.dependencies.model.KeyValue;
import io.jaegertracing.spark.dependencies.model.Process;
import io.jaegertracing.spark.dependencies.model.Span;
import java.io.IOException;
import java.math.BigInteger;
import java.util.Arrays;

/**
 * @author Pavol Loffay
 */
public class SpanDeserializer extends StdDeserializer<Span> {

  // TODO Spark incorrectly serializes object mapper, therefore reinitializing here
  private ObjectMapper objectMapper = JsonHelper.configure(new ObjectMapper());

  public SpanDeserializer() {
    super(Span.class);
  }

  @Override
  public Span deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {
    JsonNode node = objectMapper.getFactory().setCodec(objectMapper).getCodec().readTree(jp);

    if (node == null || node.get("spanID") == null || node.get("parentSpanID") == null ||
            node.get("traceID") == null || node.get("startTime") == null ||
            node.get("process") == null || node.get("tags") == null) {
        return new Span();
    }

    String spanIdHex = node.get("spanID").asText();
    String parentIdHex = node.get("parentSpanID").asText();
    String traceIdHex = node.get("traceID").asText();
    String startTimeStr = node.get("startTime").asText();

    BigInteger spanId = new BigInteger(spanIdHex, 16);
    BigInteger parentId = new BigInteger(parentIdHex, 16);

    JsonNode processNode = node.get("process");
    Process process = objectMapper.treeToValue(processNode, Process.class);

    JsonNode tagsNode = node.get("tags");
    KeyValue[] tags = objectMapper.treeToValue(tagsNode, KeyValue[].class);

    Span span = new Span();
    span.setSpanId(spanId.longValue());
    span.setParentId(parentId.longValue());
    span.setTraceId(traceIdHex);
    span.setStartTime(startTimeStr != null ? Long.parseLong(startTimeStr) : null);
    span.setProcess(process);
    span.setTags(Arrays.asList(tags));
    return span;
  }
}
