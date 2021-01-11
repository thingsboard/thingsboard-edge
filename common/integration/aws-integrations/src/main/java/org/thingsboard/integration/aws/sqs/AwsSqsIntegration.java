/**
 * Copyright © 2016-2021 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.integration.aws.sqs;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;
import org.thingsboard.integration.api.AbstractIntegration;
import org.thingsboard.integration.api.IntegrationContext;
import org.thingsboard.integration.api.TbIntegrationInitParams;
import org.thingsboard.integration.api.data.UplinkData;
import org.thingsboard.integration.api.data.UplinkMetaData;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/*
 * Created by Valerii Sosliuk on 30.05.19
 */
@Slf4j
public class AwsSqsIntegration extends AbstractIntegration<SqsIntegrationMsg> {

    private IntegrationContext context;
    private SqsIntegrationConfiguration sqsConfiguration;
    private AmazonSQS sqs;
    private ScheduledFuture taskFuture;
    private volatile boolean stopped;

    @PostConstruct
    public void init(TbIntegrationInitParams params) throws Exception {
        super.init(params);
        if (!this.configuration.isEnabled()) {
            stopped = true;
            return;
        }
        stopped = false;
        this.context = params.getContext();
        this.sqsConfiguration = mapper.readValue(
                mapper.writeValueAsString(configuration.getConfiguration().get("sqsConfiguration")),
                SqsIntegrationConfiguration.class);
        BasicAWSCredentials awsCreds = new BasicAWSCredentials(sqsConfiguration.getAccessKeyId(), sqsConfiguration.getSecretAccessKey());
        sqs = AmazonSQSClientBuilder.standard().withRegion(sqsConfiguration.getRegion())
                .withCredentials(new AWSStaticCredentialsProvider(awsCreds)).build();
        taskFuture = this.context.getScheduledExecutorService().schedule(this::pollMessages, sqsConfiguration.getPollingPeriodSeconds(), TimeUnit.SECONDS);
    }

    private void pollMessages() {
        if (stopped) {
            return;
        }
        try {
            ReceiveMessageRequest sqsRequest = new ReceiveMessageRequest();
            sqsRequest.setQueueUrl(sqsConfiguration.getQueueUrl());
            sqsRequest.setMaxNumberOfMessages(10);
            List<Message> messages = sqs.receiveMessage(sqsRequest).getMessages();
            if (!CollectionUtils.isEmpty(messages)) {
                for (Message message : messages) {
                    try {
                        SqsIntegrationMsg sqsMessage = toSqsIntegrationMsg(message);
                        process(sqsMessage);
                    } catch (IOException e) {
                        log.error("Failed to process message: " + message + ". Reason: " + e.getMessage(), e);
                    } finally {
                        sqs.deleteMessage(sqsConfiguration.getQueueUrl(), message.getReceiptHandle());
                    }
                }
                this.context.getScheduledExecutorService().submit(this::pollMessages);
            } else {
                taskFuture = this.context.getScheduledExecutorService().schedule(this::pollMessages, sqsConfiguration.getPollingPeriodSeconds(), TimeUnit.SECONDS);
            }
        } catch (Exception e) {
            log.trace(e.getMessage(), e);
            persistDebug(context, "Uplink", getUplinkContentType(), e.getMessage(), "ERROR", e);
            taskFuture = this.context.getScheduledExecutorService().schedule(this::pollMessages, sqsConfiguration.getPollingPeriodSeconds(), TimeUnit.SECONDS);
        }
    }

    private SqsIntegrationMsg toSqsIntegrationMsg(Message message) throws IOException {
        String unescaped = StringEscapeUtils.unescapeJson(message.getBody());
        unescaped = StringUtils.removeStart(unescaped, "\"");
        unescaped = StringUtils.removeEnd(unescaped, "\"");
        JsonNode node = mapper.readTree(unescaped);
        SqsIntegrationMsg sqsMsg = new SqsIntegrationMsg(node, metadataTemplate.getKvMap());
        return sqsMsg;
    }

    @Override
    public void process(SqsIntegrationMsg message) {
        try {
            List<UplinkData> uplinkDataList = convertToUplinkDataList(context, message.getPayload(), new UplinkMetaData(getUplinkContentType(), message.getDeviceMetadata()));
            if (uplinkDataList != null) {
                for (UplinkData data : uplinkDataList) {
                    processUplinkData(context, data);
                    log.debug("[{}] Processing uplink data", data);
                }
            }
            if (configuration.isDebugMode()) {
                persistDebug(context, "Uplink", getUplinkContentType(), mapper.writeValueAsString(message.getJson()), "OK", null);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            persistDebug(context, "Uplink", getUplinkContentType(), e.getMessage(), "ERROR", e);
        }
    }

    @PreDestroy
    public void stop() {
        stopped = true;
        if (sqs != null) {
            sqs.shutdown();
        }
        if (taskFuture != null) {
            taskFuture.cancel(true);
        }
    }
}
