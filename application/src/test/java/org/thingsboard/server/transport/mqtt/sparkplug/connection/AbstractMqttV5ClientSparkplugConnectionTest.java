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
package org.thingsboard.server.transport.mqtt.sparkplug.connection;

import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.junit.Assert;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.kv.BasicTsKvEntry;
import org.thingsboard.server.common.data.kv.LongDataEntry;
import org.thingsboard.server.common.data.kv.StringDataEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.gen.transport.mqtt.SparkplugBProto;
import org.thingsboard.server.transport.mqtt.mqttv5.MqttV5TestClient;
import org.thingsboard.server.transport.mqtt.sparkplug.AbstractMqttV5ClientSparkplugTest;
import org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugMessageType;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.awaitility.Awaitility.await;
import static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugConnectionState.OFFLINE;
import static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugConnectionState.ONLINE;
import static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugMessageType.DBIRTH;
import static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugMessageType.STATE;
import static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugMessageType.messageName;
import static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugTopicUtil.NAMESPACE;

/**
 * Created by nickAS21 on 12.01.23
 */
@Slf4j
public abstract class AbstractMqttV5ClientSparkplugConnectionTest extends AbstractMqttV5ClientSparkplugTest {

    protected void processClientWithCorrectNodeAccessTokenWithNDEATH_Test() throws Exception {
        long ts = calendar.getTimeInMillis() - PUBLISH_TS_DELTA_MS;
        long value = bdSeq = 0;
        clientWithCorrectNodeAccessTokenWithNDEATH(ts, value);

        String keys = SparkplugMessageType.NDEATH.name() + " " + keysBdSeq;
        TsKvEntry expectedTsKvEntry = new BasicTsKvEntry(ts, new LongDataEntry(keys, value));
        AtomicReference<ListenableFuture<Optional<TsKvEntry>>> finalFuture = new AtomicReference<>();
        await(alias + SparkplugMessageType.NDEATH.name())
                .atMost(40, TimeUnit.SECONDS)
                .until(() -> {
                    finalFuture.set(tsService.findLatest(tenantId, savedGateway.getId(), keys));
                    return finalFuture.get().get().isPresent();
                });
        TsKvEntry actualTsKvEntry = finalFuture.get().get().get();
        Assert.assertEquals(expectedTsKvEntry, actualTsKvEntry);
    }

    protected void processClientWithCorrectNodeAccessTokenWithoutNDEATH_Test() throws Exception {
        this.client = new MqttV5TestClient();
        MqttException actualException = Assert.assertThrows(MqttException.class, () -> client.connectAndWait(gatewayAccessToken));
        String expectedMessage = "Server unavailable.";
        int expectedReasonCode = 136;
        Assert.assertEquals(expectedMessage, actualException.getMessage());
        Assert.assertEquals(expectedReasonCode, actualException.getReasonCode());
    }

    protected void processClientWithCorrectNodeAccessTokenNameSpaceInvalid_Test() throws Exception {
        long ts = calendar.getTimeInMillis() - PUBLISH_TS_DELTA_MS;
        long value = bdSeq = 0;
        MqttException actualException = Assert.assertThrows(MqttException.class, () -> clientConnectWithNDEATH(ts, value, "spBv1.2"));
        String expectedMessage = "Server unavailable.";
        int expectedReasonCode = 136;
        Assert.assertEquals(expectedMessage, actualException.getMessage());
        Assert.assertEquals(expectedReasonCode, actualException.getReasonCode());
    }

    protected void processClientWithCorrectAccessTokenWithNDEATHCreatedDevices(int cntDevices) throws Exception {
        long ts = calendar.getTimeInMillis();
        connectClientWithCorrectAccessTokenWithNDEATHCreatedDevices(cntDevices, ts);
    }

    protected void processClientWithCorrectAccessTokenWithNDEATHCreatedDeviceAutogeneratedGroupForDevice() throws Exception {
        long ts = calendar.getTimeInMillis();
        List<Device> devices = connectClientWithCorrectAccessTokenWithNDEATHCreatedDevices(1, ts);
        String descriptionExpect = "Autogenerated Device";
        AtomicReference<EntityGroup> entityGroup = new AtomicReference<>();

        await(alias + messageName(DBIRTH) + ", device: " + devices.get(0))
                .atMost(40, TimeUnit.SECONDS)
                .until(() -> {
                    DaoUtil.processInBatches(pageLink ->
                            entityGroupService.findEntityGroupsByTypeAndPageLink(tenantId, EntityType.DEVICE, pageLink), 1024, entity -> {
                        if (entity.getName().equals(groupId)) {
                            entityGroup.set(entity);
                        }
                    });
                    return entityGroup.get() != null;
                });

        Assert.assertEquals(descriptionExpect, entityGroup.get().getAdditionalInfo().get("description").asText());
    }

    protected void processConnectClientWithCorrectAccessTokenWithNDEATH_State_ONLINE_ALL(int cntDevices) throws Exception {
        long ts = calendar.getTimeInMillis();
        List<Device> devices = connectClientWithCorrectAccessTokenWithNDEATHCreatedDevices(cntDevices, ts);

        TsKvEntry tsKvEntry = new BasicTsKvEntry(ts, new StringDataEntry(messageName(STATE), ONLINE.name()));
        AtomicReference<ListenableFuture<List<TsKvEntry>>> finalFuture = new AtomicReference<>();
        await(alias + messageName(STATE) + ", device: " + savedGateway.getName())
                .atMost(40, TimeUnit.SECONDS)
                .until(() -> {
                    finalFuture.set(tsService.findAllLatest(tenantId, savedGateway.getId()));
                    return finalFuture.get().get().contains(tsKvEntry);
                });

        for (Device device : devices) {
            await(alias + messageName(STATE) + ", device: " + device.getName())
                    .atMost(40, TimeUnit.SECONDS)
                    .until(() -> {
                        finalFuture.set(tsService.findAllLatest(tenantId, device.getId()));
                        return finalFuture.get().get().contains(tsKvEntry);
                    });
        }
    }

    protected void processConnectClientWithCorrectAccessTokenWithNDEATH_State_ONLINE_All_Then_OneDeviceOFFLINE(int cntDevices, int indexDeviceDisconnect) throws Exception {
        long ts = calendar.getTimeInMillis();
        List<Device> devices = connectClientWithCorrectAccessTokenWithNDEATHCreatedDevices(cntDevices, ts);

        TsKvEntry tsKvEntry = new BasicTsKvEntry(ts, new StringDataEntry(messageName(STATE), OFFLINE.name()));
        AtomicReference<ListenableFuture<List<TsKvEntry>>> finalFuture = new AtomicReference<>();

        SparkplugBProto.Payload.Builder payloadDeathDevice = SparkplugBProto.Payload.newBuilder()
                .setTimestamp(ts)
                .setSeq(getSeqNum());
        if (client.isConnected()) {
            List<Device> devicesList = new ArrayList<>(devices);
            Device device =  devicesList.get(indexDeviceDisconnect);
            client.publish(NAMESPACE + "/" + groupId + "/" + SparkplugMessageType.DDEATH.name() + "/" + edgeNode + "/" + device.getName(),
                    payloadDeathDevice.build().toByteArray(), 0, false);
            await(alias + messageName(STATE) + ", device: " + device.getName())
                    .atMost(40, TimeUnit.SECONDS)
                    .until(() -> {
                        finalFuture.set(tsService.findAllLatest(tenantId, device.getId()));
                        return findEqualsKeyValueInKvEntrys(finalFuture.get().get(), tsKvEntry);
                    });
        }
    }

    protected void processConnectClientWithCorrectAccessTokenWithNDEATH_State_ONLINE_All_Then_OFFLINE_All(int cntDevices) throws Exception {
        long ts = calendar.getTimeInMillis();
        List<Device> devices = connectClientWithCorrectAccessTokenWithNDEATHCreatedDevices(cntDevices, ts);

        TsKvEntry tsKvEntry = new BasicTsKvEntry(ts, new StringDataEntry(messageName(STATE), OFFLINE.name()));
        AtomicReference<ListenableFuture<List<TsKvEntry>>> finalFuture = new AtomicReference<>();

        if (client.isConnected()) {
            client.disconnect();

            await(alias + messageName(STATE) + ", device: " + savedGateway.getName())
                    .atMost(40, TimeUnit.SECONDS)
                    .until(() -> {
                        finalFuture.set(tsService.findAllLatest(tenantId, savedGateway.getId()));
                        return findEqualsKeyValueInKvEntrys(finalFuture.get().get(), tsKvEntry);
                    });

            List<Device> devicesList = new ArrayList<>(devices);
            for (Device device : devicesList) {
                await(alias + messageName(STATE) + ", device: " + device.getName())
                        .atMost(40, TimeUnit.SECONDS)
                        .until(() -> {
                            finalFuture.set(tsService.findAllLatest(tenantId, device.getId()));
                            return findEqualsKeyValueInKvEntrys(finalFuture.get().get(), tsKvEntry);
                        });
            }
        }
    }

    private boolean findEqualsKeyValueInKvEntrys(List<TsKvEntry> finalFuture, TsKvEntry tsKvEntry) {
        for (TsKvEntry kvEntry : finalFuture) {
            if (kvEntry.getKey().equals(tsKvEntry.getKey()) && kvEntry.getValue().equals(tsKvEntry.getValue())) {
                return true;
            }
        }
        return false;
    }
}

