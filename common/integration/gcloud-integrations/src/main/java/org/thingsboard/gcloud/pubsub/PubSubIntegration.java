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
package org.thingsboard.gcloud.pubsub;

import com.google.api.gax.core.CredentialsProvider;
import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.pubsub.v1.Subscriber;
import com.google.pubsub.v1.ProjectSubscriptionName;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.integration.api.AbstractIntegration;
import org.thingsboard.integration.api.IntegrationContext;
import org.thingsboard.integration.api.TbIntegrationInitParams;
import org.thingsboard.integration.api.data.UplinkData;
import org.thingsboard.integration.api.data.UplinkMetaData;
import org.thingsboard.integration.api.util.ConvertUtil;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class PubSubIntegration extends AbstractIntegration<PubSubIntegrationMsg> {

    private IntegrationContext context;
    private PubSubIntegrationConfiguration pubSubConfiguration;
    private Subscriber subscriber;
    private volatile boolean stopped;

    @Override
    public void init(TbIntegrationInitParams params) throws Exception {
        super.init(params);
        stopped = false;
        this.context = params.getContext();
        this.pubSubConfiguration = getClientConfiguration(configuration, PubSubIntegrationConfiguration.class);
        ServiceAccountCredentials credentials =
                ServiceAccountCredentials.fromStream(new ByteArrayInputStream(pubSubConfiguration.getServiceAccountKey().getBytes()));
        CredentialsProvider credProvider = FixedCredentialsProvider.create(credentials);
        ProjectSubscriptionName subscriptionName = ProjectSubscriptionName.of(
                pubSubConfiguration.getProjectId(), pubSubConfiguration.getSubscriptionId());
        subscriber = Subscriber.newBuilder(subscriptionName, (pubsubMessage, ackReplyConsumer) -> {
            Map<String, String> metadata = new HashMap<>(metadataTemplate.getKvMap());
            metadata.putAll(pubsubMessage.getAttributesMap());
            metadata.put("pubSubMsgId", pubsubMessage.getMessageId());
            process(new PubSubIntegrationMsg(pubsubMessage.getData().toByteArray(), metadata));
            ackReplyConsumer.ack();
        }).setCredentialsProvider(credProvider).build();
        subscriber.startAsync().awaitRunning();
    }

    @Override
    public void process(PubSubIntegrationMsg msg) {
        if (stopped) {
            return;
        }
        try {
            List<UplinkData> uplinkDataList = convertToUplinkDataList(context, msg.getPayload(), new UplinkMetaData(getDefaultUplinkContentType(), msg.getDeviceMetadata()));
            if (uplinkDataList != null) {
                for (UplinkData data : uplinkDataList) {
                    processUplinkData(context, data);
                    log.trace("[{}] Processing uplink data", data);
                }
            }
            integrationStatistics.incMessagesProcessed();
            if (configuration.isDebugMode()) {
                persistDebug(context, "Uplink", getDefaultUplinkContentType(),
                        ConvertUtil.toDebugMessage(getDefaultUplinkContentType(), msg.getPayload()), "OK", null);
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
        if (subscriber != null) {
            subscriber.stopAsync();
        }
    }
}
