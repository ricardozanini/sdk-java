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
package io.serverless.workflow.impl.executors.func;

import static io.serverlessworkflow.fluent.func.dsl.FuncDSL.consume;
import static io.serverlessworkflow.fluent.func.dsl.FuncDSL.consumed;
import static io.serverlessworkflow.fluent.func.dsl.FuncDSL.emitJson;
import static io.serverlessworkflow.fluent.func.dsl.FuncDSL.function;
import static io.serverlessworkflow.fluent.func.dsl.FuncDSL.listen;
import static io.serverlessworkflow.fluent.func.dsl.FuncDSL.switchWhenOrElse;
import static io.serverlessworkflow.fluent.func.dsl.FuncDSL.to;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.cloudevents.CloudEvent;
import io.cloudevents.CloudEventData;
import io.cloudevents.core.builder.CloudEventBuilder;
import io.serverlessworkflow.api.types.Workflow;
import io.serverlessworkflow.fluent.func.FuncWorkflowBuilder;
import io.serverlessworkflow.impl.WorkflowApplication;
import io.serverlessworkflow.impl.WorkflowDefinition;
import io.serverlessworkflow.impl.WorkflowInstance;
import io.serverlessworkflow.impl.WorkflowModel;
import io.serverlessworkflow.impl.WorkflowStatus;
import io.serverlessworkflow.impl.events.EventPublisher;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;

public class EventFilteringTest {

  // --- Mock Domain Models ---
  public record NewsletterRequest(String topic) {}

  public record NewsletterDraft(String title, String body) {}

  public record HumanReview(String status, String notes) {
    public static final String NEEDS_REVISION = "NEEDS_REVISION";
    public static final String APPROVED = "APPROVED";
  }

  private static final ObjectMapper MAPPER = new ObjectMapper();

  // --- Mock Service Methods (replacing Quarkus Agents) ---
  public NewsletterDraft writeDraft(NewsletterRequest req) {
    return new NewsletterDraft("Draft: " + req.topic(), "Initial body...");
  }

  public NewsletterDraft editDraft(HumanReview review) {
    return new NewsletterDraft("Edited Draft", "Fixed based on: " + review.notes());
  }

  public void sendEmail(NewsletterDraft draft) {
    // Simulates MailService.send
  }

  @Test
  public void testIntelligentNewsletterApprovalPath() throws Exception {
    try (WorkflowApplication app =
        WorkflowApplication.builder().withListener(new TraceExecutionListener()).build()) {

      // 1. Build the workflow using your exact DSL
      Workflow workflow =
          FuncWorkflowBuilder.workflow("intelligent-newsletter")
              .tasks(
                  function("draftAgent", this::writeDraft).exportAsTaskOutput(),
                  emitJson("draftReady", "org.acme.email.review.required", NewsletterDraft.class),
                  listen(
                          "waitHumanReview",
                          to().one(
                                  consumed("org.acme.newsletter.review.done")
                                      .extensionByInstanceId("instanceid")))
                      .outputAs(
                          (ArrayList events) -> {
                            try {
                              return MAPPER.readValue(
                                  ((CloudEventData) events.iterator().next()).toBytes(),
                                  HumanReview.class);
                            } catch (Exception e) {
                              throw new RuntimeException("Failed to deserialize HumanReview", e);
                            }
                          }),
                  switchWhenOrElse(
                      h -> HumanReview.NEEDS_REVISION.equals(h.status()),
                      "humanEditorAgent",
                      "sendNewsletter",
                      HumanReview.class),
                  function("humanEditorAgent", this::editDraft)
                      .exportAsTaskOutput()
                      .then("draftReady"),
                  consume("sendNewsletter", this::sendEmail)
                      .inputFrom(
                          (payload, wfc, tfc) ->
                              payload instanceof HumanReview ? wfc.context() : payload))
              .build();

      WorkflowDefinition definition = app.workflowDefinition(workflow);
      WorkflowInstance instance = definition.instance(new NewsletterRequest("Tech Stocks"));
      CompletableFuture<WorkflowModel> future = instance.start();

      // Wait for it to hit the listen state
      Thread.sleep(250);
      assertThat(instance.status()).isEqualTo(WorkflowStatus.WAITING);

      EventPublisher publisher = app.eventPublishers().iterator().next();

      // --- THE NEGATIVE TEST: Fire an event with the WRONG instance id ---
      CloudEvent maliciousEvent =
          CloudEventBuilder.v1()
              .withId("event-wrong")
              .withSource(URI.create("test:/human-editor"))
              .withType("org.acme.newsletter.review.done")
              .withExtension("instanceid", "SOME-OTHER-ID-12345") // Does not match instance.id()
              .withData(
                  "application/json",
                  "{\"status\":\"APPROVED\", \"notes\":\"Malicious approval\"}"
                      .getBytes(StandardCharsets.UTF_8))
              .build();

      publisher.publish(maliciousEvent).toCompletableFuture().join();

      // Give the engine a moment to process and discard the event
      Thread.sleep(250);

      // Assert that the workflow completely ignored it and is STILL waiting
      assertThat(future.isDone()).isFalse();
      assertThat(instance.status()).isEqualTo(WorkflowStatus.WAITING);

      // --- THE POSITIVE TEST: Fire the CORRECT event ---
      CloudEvent humanReviewEvent =
          CloudEventBuilder.v1()
              .withId("event-123")
              .withSource(URI.create("test:/human-editor"))
              .withType("org.acme.newsletter.review.done")
              .withExtension("instanceid", instance.id()) // Matches exactly
              .withData(
                  "application/json",
                  "{\"status\":\"APPROVED\", \"notes\":\"Looks good\"}"
                      .getBytes(StandardCharsets.UTF_8))
              .build();

      publisher.publish(humanReviewEvent).toCompletableFuture().join();

      // Wait for the workflow to resume and finish
      future.join();

      // Assert successful completion
      assertThat(instance.status()).isEqualTo(WorkflowStatus.COMPLETED);
    }
  }
}
