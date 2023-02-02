/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2023 ThingsBoard, Inc. All Rights Reserved.
 *
 * NOTICE: All information contained herein is, and remains
 * the property of ThingsBoard, Inc. and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to ThingsBoard, Inc.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 *
 * Dissemination of this information or reproduction of this material is strictly forbidden
 * unless prior written permission is obtained from COMPANY.
 *
 * Access to the source code contained herein is hereby forbidden to anyone except current COMPANY employees,
 * managers or contractors who have executed Confidentiality and Non-disclosure agreements
 * explicitly covering such access.
 *
 * The copyright notice above does not evidence any actual or intended publication
 * or disclosure  of  this source code, which includes
 * information that is confidential and/or proprietary, and is a trade secret, of  COMPANY.
 * ANY REPRODUCTION, MODIFICATION, DISTRIBUTION, PUBLIC  PERFORMANCE,
 * OR PUBLIC DISPLAY OF OR THROUGH USE  OF THIS  SOURCE CODE  WITHOUT
 * THE EXPRESS WRITTEN CONSENT OF COMPANY IS STRICTLY PROHIBITED,
 * AND IN VIOLATION OF APPLICABLE LAWS AND INTERNATIONAL TREATIES.
 * THE RECEIPT OR POSSESSION OF THIS SOURCE CODE AND/OR RELATED INFORMATION
 * DOES NOT CONVEY OR IMPLY ANY RIGHTS TO REPRODUCE, DISCLOSE OR DISTRIBUTE ITS CONTENTS,
 * OR TO MANUFACTURE, USE, OR SELL ANYTHING THAT IT  MAY DESCRIBE, IN WHOLE OR IN PART.
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
import org.springframework.util.CollectionUtils;
import org.thingsboard.integration.api.AbstractIntegration;
import org.thingsboard.integration.api.IntegrationContext;
import org.thingsboard.integration.api.TbIntegrationInitParams;
import org.thingsboard.integration.api.data.UplinkData;
import org.thingsboard.integration.api.data.UplinkMetaData;
import org.thingsboard.server.common.data.StringUtils;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/*
 * Created by Valerii Sosliuk on 30.05.19
 */
@Slf4j
public class AwsSqsIntegration extends AbstractIntegration<SqsIntegrationMsg> {

    private IntegrationContext context;
    private SqsIntegrationConfiguration sqsConfiguration;
    private AmazonSQS sqs;
    private ScheduledFuture<?> taskFuture;
    private volatile boolean stopped;
    private final Lock pollLock = new ReentrantLock();

    @Override
    public void init(TbIntegrationInitParams params) throws Exception {
        super.init(params);
        stopped = false;
        this.context = params.getContext();
        this.sqsConfiguration = mapper.readValue(
                mapper.writeValueAsString(configuration.getConfiguration().get("sqsConfiguration")),
                SqsIntegrationConfiguration.class);
        BasicAWSCredentials awsCreds = new BasicAWSCredentials(sqsConfiguration.getAccessKeyId(), sqsConfiguration.getSecretAccessKey());
        sqs = AmazonSQSClientBuilder.standard().withRegion(sqsConfiguration.getRegion())
                .withCredentials(new AWSStaticCredentialsProvider(awsCreds)).build();
        schedulePoll();
    }

    private void schedulePoll() {
        taskFuture = this.context.getScheduledExecutorService().schedule(this::submitPoll, sqsConfiguration.getPollingPeriodSeconds(), TimeUnit.SECONDS);
    }

    private void submitPoll() {
        context.getExecutorService().execute(this::pollMessages);
    }

    private void pollMessages() {
        pollLock.lock();
        try {
            if (stopped) {
                return;
            }
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
                if (!stopped) {
                    submitPoll();
                }
            } else {
                schedulePoll();
            }
        } catch (Exception e) {
            log.trace(e.getMessage(), e);
            persistDebug(context, "Uplink", getDefaultUplinkContentType(), e.getMessage(), "ERROR", e);
            schedulePoll();
        } finally {
            pollLock.unlock();
        }
    }

    private SqsIntegrationMsg toSqsIntegrationMsg(Message message) throws IOException {
        String unescaped = StringEscapeUtils.unescapeJson(message.getBody());
        unescaped = StringUtils.removeStart(unescaped, "\"");
        unescaped = StringUtils.removeEnd(unescaped, "\"");
        JsonNode node = mapper.readTree(unescaped);
        return new SqsIntegrationMsg(node, metadataTemplate.getKvMap());
    }

    @Override
    public void process(SqsIntegrationMsg message) {
        try {
            List<UplinkData> uplinkDataList = convertToUplinkDataList(context, message.getPayload(), new UplinkMetaData(getDefaultUplinkContentType(), message.getDeviceMetadata()));
            if (uplinkDataList != null) {
                for (UplinkData data : uplinkDataList) {
                    processUplinkData(context, data);
                    log.trace("[{}] Processing uplink data", data);
                }
            }
            integrationStatistics.incMessagesProcessed();
            if (configuration.isDebugMode()) {
                persistDebug(context, "Uplink", getDefaultUplinkContentType(), mapper.writeValueAsString(message.getJson()), "OK", null);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            integrationStatistics.incErrorsOccurred();
            persistDebug(context, "Uplink", getDefaultUplinkContentType(), e.getMessage(), "ERROR", e);
        }
    }

    @Override
    public void destroy() {
        stopped = true;
        pollLock.lock();
        try {
            if (sqs != null) {
                sqs.shutdown();
            }
            if (taskFuture != null) {
                taskFuture.cancel(true);
            }
        } finally {
            pollLock.unlock();
        }
    }
}
