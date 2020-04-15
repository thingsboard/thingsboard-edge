/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2020 ThingsBoard, Inc. All Rights Reserved.
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
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.CreateQueueRequest;
import com.amazonaws.services.sqs.model.QueueAttributeName;
import org.thingsboard.server.queue.TbQueueAdmin;

import java.util.HashMap;
import java.util.Map;

public class TbAwsSqsAdmin implements TbQueueAdmin {

    private final TbAwsSqsSettings sqsSettings;
    private final Map<String, String> attributes = new HashMap<>();
    private final AWSStaticCredentialsProvider credProvider;

    public TbAwsSqsAdmin(TbAwsSqsSettings sqsSettings) {
        this.sqsSettings = sqsSettings;

        AWSCredentials awsCredentials = new BasicAWSCredentials(sqsSettings.getAccessKeyId(), sqsSettings.getSecretAccessKey());
        this.credProvider = new AWSStaticCredentialsProvider(awsCredentials);

        attributes.put("FifoQueue", "true");
        attributes.put("ContentBasedDeduplication", "true");
        attributes.put(QueueAttributeName.VisibilityTimeout.toString(), sqsSettings.getVisibilityTimeout());
    }

    @Override
    public void createTopicIfNotExists(String topic) {
        AmazonSQS sqsClient = AmazonSQSClientBuilder.standard()
                .withCredentials(credProvider)
                .withRegion(sqsSettings.getRegion())
                .build();

        final CreateQueueRequest createQueueRequest =
                new CreateQueueRequest(topic.replaceAll("\\.", "_") + ".fifo")
                        .withAttributes(attributes);
        try {
            sqsClient.createQueue(createQueueRequest);
        } finally {
            if (sqsClient != null) {
                sqsClient.shutdown();
            }
        }
    }
}
