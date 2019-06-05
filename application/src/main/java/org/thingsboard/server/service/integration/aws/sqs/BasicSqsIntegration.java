/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 * <p>
 * Copyright © 2016-2019 ThingsBoard, Inc. All Rights Reserved.
 * <p>
 * NOTICE: All information contained herein is, and remains
 * the property of ThingsBoard, Inc. and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to ThingsBoard, Inc.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * <p>
 * Dissemination of this information or reproduction of this material is strictly forbidden
 * unless prior written permission is obtained from COMPANY.
 * <p>
 * Access to the source code contained herein is hereby forbidden to anyone except current COMPANY employees,
 * managers or contractors who have executed Confidentiality and Non-disclosure agreements
 * explicitly covering such access.
 * <p>
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
package org.thingsboard.server.service.integration.aws.sqs;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.thingsboard.server.service.converter.UplinkData;
import org.thingsboard.server.service.converter.UplinkMetaData;
import org.thingsboard.server.service.integration.AbstractIntegration;
import org.thingsboard.server.service.integration.IntegrationContext;
import org.thingsboard.server.service.integration.TbIntegrationInitParams;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/*
 * Created by Valerii Sosliuk on 30.05.19
 */
@Slf4j
public class BasicSqsIntegration extends AbstractIntegration<SqsIntegrationMsg> {

    private SqsIntegrationConfiguration sqsConfiguration;
    private AmazonSQS sqs;
    private ScheduledExecutorService executor;

    private IntegrationContext context;

    private static final ObjectMapper mapper = new ObjectMapper();

    @Override
    public void init(TbIntegrationInitParams params) throws Exception {
        super.init(params);
        this.context = params.getContext();
        this.sqsConfiguration = mapper.readValue(
                mapper.writeValueAsString(configuration.getConfiguration().get("sqsConfiguration")),
                SqsIntegrationConfiguration.class);
        executor = Executors.newSingleThreadScheduledExecutor();
        BasicAWSCredentials awsCreds = new BasicAWSCredentials(sqsConfiguration.getAccessKeyId(), sqsConfiguration.getSecretAccessKey());

        sqs = AmazonSQSClientBuilder.standard().withRegion(sqsConfiguration.getRegion())
                .withCredentials(new AWSStaticCredentialsProvider(awsCreds)).build();
        executor.schedule(this::pollMessages, sqsConfiguration.getPollingPeriodSeconds(), TimeUnit.SECONDS);
    }

    private void pollMessages() {
        try {
            ReceiveMessageRequest sqsRequest = new ReceiveMessageRequest();
            sqsRequest.setQueueUrl(sqsConfiguration.getQueueUrl());
            sqsRequest.setMaxNumberOfMessages(10);
            List<Message> messages = sqs.receiveMessage(sqsRequest).getMessages();
            for (Message message : messages) {
                try {
                    SqsIntegrationMsg sqsMsg = toSqsIntegrationMsg(message);
                    process(context, sqsMsg);
                } finally {
                    sqs.deleteMessage(sqsConfiguration.getQueueUrl(), message.getReceiptHandle());
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            persistDebug(context, "Uplink", getUplinkContentType(), e.getMessage(), "ERROR", e);
        }
        executor.schedule(this::pollMessages, sqsConfiguration.getPollingPeriodSeconds(), TimeUnit.SECONDS);
    }


    private SqsIntegrationMsg toSqsIntegrationMsg(Message message) throws IOException {
        String unescaped = StringEscapeUtils.unescapeJson(message.getBody());
        unescaped = StringUtils.removeStart(unescaped, "\"");
        unescaped = StringUtils.removeEnd(unescaped, "\"");
        JsonNode node = mapper.readTree(unescaped);
        // TODO: vsosliuk add metadata
        SqsIntegrationMsg sqsMsg = new SqsIntegrationMsg(node, metadataTemplate.getKvMap());
        return sqsMsg;
    }

    @Override
    public void process(IntegrationContext context, SqsIntegrationMsg message) {
        try {
            List<UplinkData> uplinkDataList = convertToUplinkDataList(context, message.getPayload(), new UplinkMetaData(getUplinkContentType(), message.getDeviceMetadata()));
            if (uplinkDataList != null) {
                for (UplinkData data : uplinkDataList) {
                    processUplinkData(context, data);
                    log.info("[{}] Processing uplink data", data);
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
}
