/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2022 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.queue.sqs;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.CreateQueueRequest;
import org.thingsboard.server.queue.TbQueueAdmin;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class TbAwsSqsAdmin implements TbQueueAdmin {

    private final Map<String, String> attributes;
    private final AmazonSQS sqsClient;
    private final Set<String> queues;

    public TbAwsSqsAdmin(TbAwsSqsSettings sqsSettings, Map<String, String> attributes) {
        this.attributes = attributes;

        AWSCredentialsProvider credentialsProvider;
        if (sqsSettings.getUseDefaultCredentialProviderChain()) {
            credentialsProvider = new DefaultAWSCredentialsProviderChain();
        } else {
            AWSCredentials awsCredentials = new BasicAWSCredentials(sqsSettings.getAccessKeyId(), sqsSettings.getSecretAccessKey());
            credentialsProvider = new AWSStaticCredentialsProvider(awsCredentials);
        }

        sqsClient = AmazonSQSClientBuilder.standard()
                .withCredentials(credentialsProvider)
                .withRegion(sqsSettings.getRegion())
                .build();

        queues = sqsClient
                .listQueues()
                .getQueueUrls()
                .stream()
                .map(this::getQueueNameFromUrl)
                .collect(Collectors.toCollection(ConcurrentHashMap::newKeySet));
    }

    @Override
    public void createTopicIfNotExists(String topic) {
        String queueName = topic.replaceAll("\\.", "_") + ".fifo";
        if (queues.contains(queueName)) {
            return;
        }
        final CreateQueueRequest createQueueRequest = new CreateQueueRequest(queueName).withAttributes(attributes);
        String queueUrl = sqsClient.createQueue(createQueueRequest).getQueueUrl();
        queues.add(getQueueNameFromUrl(queueUrl));
    }

    private String getQueueNameFromUrl(String queueUrl) {
        int delimiterIndex = queueUrl.lastIndexOf("/");
        return queueUrl.substring(delimiterIndex + 1);
    }

    @Override
    public void destroy() {
        if (sqsClient != null) {
            sqsClient.shutdown();
        }
    }
}
