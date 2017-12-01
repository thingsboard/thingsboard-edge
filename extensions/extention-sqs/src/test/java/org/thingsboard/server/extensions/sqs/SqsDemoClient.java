/**
 * The Thingsboard Authors ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2017 The Thingsboard Authors. All Rights Reserved.
 *
 * NOTICE: All information contained herein is, and remains
 * the property of The Thingsboard Authors and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to The Thingsboard Authors
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
package org.thingsboard.server.extensions.sqs;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.DeleteMessageRequest;
import com.amazonaws.services.sqs.model.Message;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * Created by Valerii Sosliuk on 11/10/2017.
 */
@Slf4j
public class SqsDemoClient {

    private static final String ACCESS_KEY_ID = "$ACCES_KEY_ID";
    private static final String SECRET_ACCESS_KEY = "$SECRET_ACCESS_KEY";

    private static final String QUEUE_URL = "$QUEUE_URL";
    private static final String REGION = "us-east-1";

    public static void main(String[] args) {
        log.info("Starting SQS Demo Clinent...");
        AWSCredentials awsCredentials = new BasicAWSCredentials(ACCESS_KEY_ID, SECRET_ACCESS_KEY);
        AmazonSQS sqs = AmazonSQSClientBuilder.standard().withCredentials(new AWSStaticCredentialsProvider(awsCredentials))
                .withRegion(Regions.fromName(REGION)).build();
        SqsDemoClient client = new SqsDemoClient();
        client.pollMessages(sqs);
    }

    private void pollMessages(AmazonSQS sqs) {
        log.info("Polling messages");
        while (true) {
            List<Message> messages = sqs.receiveMessage(QUEUE_URL).getMessages();
            messages.forEach(m -> {
                log.info("Message Received: " + m.getBody());
                System.out.println(m.getBody());
                DeleteMessageRequest deleteMessageRequest = new DeleteMessageRequest(QUEUE_URL, m.getReceiptHandle());
                sqs.deleteMessage(deleteMessageRequest);
            });
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                e.printStackTrace();
            }
        }
    }
}
