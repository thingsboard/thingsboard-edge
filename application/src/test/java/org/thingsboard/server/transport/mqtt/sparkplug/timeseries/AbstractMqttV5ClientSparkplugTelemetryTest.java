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
package org.thingsboard.server.transport.mqtt.sparkplug.timeseries;

import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.gen.transport.mqtt.SparkplugBProto;
import org.thingsboard.server.transport.mqtt.sparkplug.AbstractMqttV5ClientSparkplugTest;
import org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugMessageType;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.awaitility.Awaitility.await;
import static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugTopicUtil.NAMESPACE;

/**
 * Created by nickAS21 on 12.01.23
 */
@Slf4j
public abstract class AbstractMqttV5ClientSparkplugTelemetryTest extends AbstractMqttV5ClientSparkplugTest {

    protected void processClientWithCorrectAccessTokenPublishNBIRTH() throws Exception {
        clientWithCorrectNodeAccessTokenWithNDEATH();
        List<String> listKeys = connectionWithNBirth(metricBirthDataType_Int32, metricBirthName_Int32, nextInt32());
        Assert.assertTrue("Connection node is failed", client.isConnected());
        AtomicReference<ListenableFuture<List<TsKvEntry>>> finalFuture = new AtomicReference<>();
        await(alias + SparkplugMessageType.NBIRTH.name())
                .atMost(40, TimeUnit.SECONDS)
                .until(() -> {
                    finalFuture.set(tsService.findLatest(tenantId, savedGateway.getId(), listKeys));
                    return !finalFuture.get().get().isEmpty();
                });
        Assert.assertEquals(listKeys.size(), finalFuture.get().get().size());
    }

    protected void processClientWithCorrectAccessTokenPushNodeMetricBuildPrimitiveSimple() throws Exception {
        List<String> listKeys = new ArrayList<>();
        clientWithCorrectNodeAccessTokenWithNDEATH();

        String messageTypeName = SparkplugMessageType.NDATA.name();

        List<TsKvEntry> listTsKvEntry = new ArrayList<>();

        SparkplugBProto.Payload.Builder ndataPayload = SparkplugBProto.Payload.newBuilder()
                .setTimestamp(calendar.getTimeInMillis())
                .setSeq(getSeqNum());
        long ts = calendar.getTimeInMillis() - PUBLISH_TS_DELTA_MS;

        createdAddMetricValuePrimitiveTsKv(listTsKvEntry, listKeys, ndataPayload, ts);

        if (client.isConnected()) {
            client.publish(NAMESPACE + "/" + groupId + "/" + messageTypeName + "/" + edgeNode,
                    ndataPayload.build().toByteArray(), 0, false);
        }

        AtomicReference<ListenableFuture<List<TsKvEntry>>> finalFuture = new AtomicReference<>();
        await(alias + SparkplugMessageType.NDATA.name())
                .atMost(40, TimeUnit.SECONDS)
                .until(() -> {
                    finalFuture.set(tsService.findAllLatest(tenantId, savedGateway.getId()));
                    return finalFuture.get().get().size() == (listTsKvEntry.size() + 1);
                });
        Assert.assertTrue("Actual tsKvEntrys is not containsAll Expected tsKvEntrys", finalFuture.get().get().containsAll(listTsKvEntry));
    }

    protected void processClientWithCorrectAccessTokenPushNodeMetricBuildArraysPrimitiveSimple() throws Exception {
        clientWithCorrectNodeAccessTokenWithNDEATH();

        String messageTypeName = SparkplugMessageType.NDATA.name();
        List<String> listKeys = new ArrayList<>();
        List<TsKvEntry> listTsKvEntry = new ArrayList<>();

        SparkplugBProto.Payload.Builder ndataPayload = SparkplugBProto.Payload.newBuilder()
                .setTimestamp(calendar.getTimeInMillis())
                .setSeq(getSeqNum());
        long ts = calendar.getTimeInMillis() - PUBLISH_TS_DELTA_MS;

        createdAddMetricValueArraysPrimitiveTsKv(listTsKvEntry, listKeys, ndataPayload, ts);

        if (client.isConnected()) {
            client.publish(NAMESPACE + "/" + groupId + "/" + messageTypeName + "/" + edgeNode,
                    ndataPayload.build().toByteArray(), 0, false);
        }

        AtomicReference<ListenableFuture<List<TsKvEntry>>> finalFuture = new AtomicReference<>();
        await(alias + SparkplugMessageType.NDATA.name())
                .atMost(40, TimeUnit.SECONDS)
                .until(() -> {
                    finalFuture.set(tsService.findAllLatest(tenantId, savedGateway.getId()));
                    return finalFuture.get().get().size() == (listTsKvEntry.size() + 1);
                });
        Assert.assertTrue("Actual tsKvEntrys is not containsAll Expected tsKvEntrys", finalFuture.get().get().containsAll(listTsKvEntry));
    }

}
