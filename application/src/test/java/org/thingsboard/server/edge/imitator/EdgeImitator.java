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
package org.thingsboard.server.edge.imitator;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.thingsboard.edge.rpc.EdgeGrpcClient;
import org.thingsboard.edge.rpc.EdgeRpcClient;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.gen.edge.AlarmUpdateMsg;
import org.thingsboard.server.gen.edge.AssetUpdateMsg;
import org.thingsboard.server.gen.edge.DashboardUpdateMsg;
import org.thingsboard.server.gen.edge.DeviceUpdateMsg;
import org.thingsboard.server.gen.edge.DownlinkMsg;
import org.thingsboard.server.gen.edge.DownlinkResponseMsg;
import org.thingsboard.server.gen.edge.EdgeConfiguration;
import org.thingsboard.server.gen.edge.RelationUpdateMsg;
import org.thingsboard.server.gen.edge.RuleChainUpdateMsg;
import org.thingsboard.server.gen.edge.UplinkResponseMsg;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
public class EdgeImitator {

    private String routingKey;
    private String routingSecret;

    private EdgeRpcClient edgeRpcClient;

    @Getter
    private EdgeStorage storage;


    public EdgeImitator(String host, int port, String routingKey, String routingSecret) throws NoSuchFieldException, IllegalAccessException {
        edgeRpcClient = new EdgeGrpcClient();
        storage = new EdgeStorage();
        this.routingKey = routingKey;
        this.routingSecret = routingSecret;
        setEdgeCredentials("rpcHost", host);
        setEdgeCredentials("rpcPort", port);
    }

    private void setEdgeCredentials(String fieldName, Object value) throws NoSuchFieldException, IllegalAccessException {
        Field fieldToSet = edgeRpcClient.getClass().getDeclaredField(fieldName);
        fieldToSet.setAccessible(true);
        fieldToSet.set(edgeRpcClient, value);
        fieldToSet.setAccessible(false);
    }

    public void connect() {
        edgeRpcClient.connect(routingKey, routingSecret,
                this::onUplinkResponse,
                this::onEdgeUpdate,
                this::onDownlink,
                this::onClose);

        edgeRpcClient.sendSyncRequestMsg();
    }

    public void disconnect() throws InterruptedException {
        edgeRpcClient.disconnect(false);
    }

    private void onUplinkResponse(UplinkResponseMsg msg) {
        log.info("onUplinkResponse: {}", msg);
    }

    private void onEdgeUpdate(EdgeConfiguration edgeConfiguration) {
        storage.setConfiguration(edgeConfiguration);
    }

    private void onDownlink(DownlinkMsg downlinkMsg) {
        ListenableFuture<List<Void>> future = processDownlinkMsg(downlinkMsg);
        Futures.addCallback(future, new FutureCallback<List<Void>>() {
            @Override
            public void onSuccess(@Nullable List<Void> result) {
                DownlinkResponseMsg downlinkResponseMsg = DownlinkResponseMsg.newBuilder().setSuccess(true).build();
                edgeRpcClient.sendDownlinkResponseMsg(downlinkResponseMsg);
            }

            @Override
            public void onFailure(Throwable t) {
                DownlinkResponseMsg downlinkResponseMsg = DownlinkResponseMsg.newBuilder().setSuccess(false).setErrorMsg(t.getMessage()).build();
                edgeRpcClient.sendDownlinkResponseMsg(downlinkResponseMsg);
            }
        }, MoreExecutors.directExecutor());
    }

    private void onClose(Exception e) {
        log.info("onClose: {}", e.getMessage());
    }

    private ListenableFuture<List<Void>> processDownlinkMsg(DownlinkMsg downlinkMsg) {
        List<ListenableFuture<Void>> result = new ArrayList<>();
        if (downlinkMsg.getDeviceUpdateMsgList() != null && !downlinkMsg.getDeviceUpdateMsgList().isEmpty()) {
            for (DeviceUpdateMsg deviceUpdateMsg: downlinkMsg.getDeviceUpdateMsgList()) {
                result.add(storage.processEntity(deviceUpdateMsg.getMsgType(), EntityType.DEVICE, new UUID(deviceUpdateMsg.getIdMSB(), deviceUpdateMsg.getIdLSB())));
            }
        }
        if (downlinkMsg.getAssetUpdateMsgList() != null && !downlinkMsg.getAssetUpdateMsgList().isEmpty()) {
            for (AssetUpdateMsg assetUpdateMsg: downlinkMsg.getAssetUpdateMsgList()) {
                result.add(storage.processEntity(assetUpdateMsg.getMsgType(), EntityType.ASSET, new UUID(assetUpdateMsg.getIdMSB(), assetUpdateMsg.getIdLSB())));
            }
        }
        if (downlinkMsg.getRuleChainUpdateMsgList() != null && !downlinkMsg.getRuleChainUpdateMsgList().isEmpty()) {
            for (RuleChainUpdateMsg ruleChainUpdateMsg: downlinkMsg.getRuleChainUpdateMsgList()) {
                result.add(storage.processEntity(ruleChainUpdateMsg.getMsgType(), EntityType.RULE_CHAIN, new UUID(ruleChainUpdateMsg.getIdMSB(), ruleChainUpdateMsg.getIdLSB())));
            }
        }
        if (downlinkMsg.getDashboardUpdateMsgList() != null && !downlinkMsg.getDashboardUpdateMsgList().isEmpty()) {
            for (DashboardUpdateMsg dashboardUpdateMsg: downlinkMsg.getDashboardUpdateMsgList()) {
                result.add(storage.processEntity(dashboardUpdateMsg.getMsgType(), EntityType.DASHBOARD, new UUID(dashboardUpdateMsg.getIdMSB(), dashboardUpdateMsg.getIdLSB())));
            }
        }
        if (downlinkMsg.getRelationUpdateMsgList() != null && !downlinkMsg.getRelationUpdateMsgList().isEmpty()) {
            for (RelationUpdateMsg relationUpdateMsg: downlinkMsg.getRelationUpdateMsgList()) {
                result.add(storage.processRelation(relationUpdateMsg));
            }
        }
        if (downlinkMsg.getAlarmUpdateMsgList() != null && !downlinkMsg.getAlarmUpdateMsgList().isEmpty()) {
            for (AlarmUpdateMsg alarmUpdateMsg: downlinkMsg.getAlarmUpdateMsgList()) {
                result.add(storage.processAlarm(alarmUpdateMsg));
            }
        }
        return Futures.allAsList(result);
    }

}
