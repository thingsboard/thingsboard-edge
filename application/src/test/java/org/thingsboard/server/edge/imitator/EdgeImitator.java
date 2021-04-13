/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2021 ThingsBoard, Inc. All Rights Reserved.
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
import com.google.protobuf.AbstractMessage;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.thingsboard.edge.rpc.EdgeGrpcClient;
import org.thingsboard.edge.rpc.EdgeRpcClient;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.gen.edge.AdminSettingsUpdateMsg;
import org.thingsboard.server.gen.edge.AlarmUpdateMsg;
import org.thingsboard.server.gen.edge.AssetUpdateMsg;
import org.thingsboard.server.gen.edge.CustomTranslationProto;
import org.thingsboard.server.gen.edge.CustomerUpdateMsg;
import org.thingsboard.server.gen.edge.DashboardUpdateMsg;
import org.thingsboard.server.gen.edge.DeviceCredentialsRequestMsg;
import org.thingsboard.server.gen.edge.DeviceCredentialsUpdateMsg;
import org.thingsboard.server.gen.edge.DeviceRpcCallMsg;
import org.thingsboard.server.gen.edge.DeviceUpdateMsg;
import org.thingsboard.server.gen.edge.DownlinkMsg;
import org.thingsboard.server.gen.edge.DownlinkResponseMsg;
import org.thingsboard.server.gen.edge.EdgeConfiguration;
import org.thingsboard.server.gen.edge.EntityDataProto;
import org.thingsboard.server.gen.edge.EntityGroupUpdateMsg;
import org.thingsboard.server.gen.edge.EntityViewUpdateMsg;
import org.thingsboard.server.gen.edge.GroupPermissionProto;
import org.thingsboard.server.gen.edge.LoginWhiteLabelingParamsProto;
import org.thingsboard.server.gen.edge.RelationUpdateMsg;
import org.thingsboard.server.gen.edge.RoleProto;
import org.thingsboard.server.gen.edge.RuleChainMetadataUpdateMsg;
import org.thingsboard.server.gen.edge.RuleChainUpdateMsg;
import org.thingsboard.server.gen.edge.SchedulerEventUpdateMsg;
import org.thingsboard.server.gen.edge.UplinkMsg;
import org.thingsboard.server.gen.edge.UplinkResponseMsg;
import org.thingsboard.server.gen.edge.UserCredentialsUpdateMsg;
import org.thingsboard.server.gen.edge.UserUpdateMsg;
import org.thingsboard.server.gen.edge.WhiteLabelingParamsProto;
import org.thingsboard.server.gen.edge.WidgetTypeUpdateMsg;
import org.thingsboard.server.gen.edge.WidgetsBundleUpdateMsg;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
public class EdgeImitator {

    private String routingKey;
    private String routingSecret;

    private EdgeRpcClient edgeRpcClient;

    private final Lock lock = new ReentrantLock();

    private CountDownLatch messagesLatch;
    private CountDownLatch responsesLatch;
    private List<Class<? extends AbstractMessage>> ignoredTypes;

    @Getter
    private EdgeConfiguration configuration;
    @Getter
    private UserId userId;
    @Getter
    private final List<AbstractMessage> downlinkMsgs;

    public EdgeImitator(String host, int port, String routingKey, String routingSecret) throws NoSuchFieldException, IllegalAccessException {
        edgeRpcClient = new EdgeGrpcClient();
        messagesLatch = new CountDownLatch(0);
        responsesLatch = new CountDownLatch(0);
        downlinkMsgs = new ArrayList<>();
        ignoredTypes = new ArrayList<>();
        this.routingKey = routingKey;
        this.routingSecret = routingSecret;
        setEdgeCredentials("rpcHost", host);
        setEdgeCredentials("rpcPort", port);
        setEdgeCredentials("keepAliveTimeSec", 300);
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

    public void sendUplinkMsg(UplinkMsg uplinkMsg) {
        edgeRpcClient.sendUplinkMsg(uplinkMsg);
    }

    private void onUplinkResponse(UplinkResponseMsg msg) {
        log.info("onUplinkResponse: {}", msg);
        responsesLatch.countDown();
    }

    private void onEdgeUpdate(EdgeConfiguration edgeConfiguration) {
        this.configuration = edgeConfiguration;
    }

    private void onUserUpdate(UserUpdateMsg userUpdateMsg) {
        this.userId = new UserId(new UUID(userUpdateMsg.getIdMSB(), userUpdateMsg.getIdLSB()));
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
                result.add(saveDownlinkMsg(deviceUpdateMsg));
            }
        }
        if (downlinkMsg.getDeviceCredentialsUpdateMsgList() != null && !downlinkMsg.getDeviceCredentialsUpdateMsgList().isEmpty()) {
            for (DeviceCredentialsUpdateMsg deviceCredentialsUpdateMsg: downlinkMsg.getDeviceCredentialsUpdateMsgList()) {
                result.add(saveDownlinkMsg(deviceCredentialsUpdateMsg));
            }
        }
        if (downlinkMsg.getAssetUpdateMsgList() != null && !downlinkMsg.getAssetUpdateMsgList().isEmpty()) {
            for (AssetUpdateMsg assetUpdateMsg: downlinkMsg.getAssetUpdateMsgList()) {
                result.add(saveDownlinkMsg(assetUpdateMsg));
            }
        }
        if (downlinkMsg.getRuleChainUpdateMsgList() != null && !downlinkMsg.getRuleChainUpdateMsgList().isEmpty()) {
            for (RuleChainUpdateMsg ruleChainUpdateMsg: downlinkMsg.getRuleChainUpdateMsgList()) {
                result.add(saveDownlinkMsg(ruleChainUpdateMsg));
            }
        }
        if (downlinkMsg.getRuleChainMetadataUpdateMsgList() != null && !downlinkMsg.getRuleChainMetadataUpdateMsgList().isEmpty()) {
            for (RuleChainMetadataUpdateMsg ruleChainMetadataUpdateMsg: downlinkMsg.getRuleChainMetadataUpdateMsgList()) {
                result.add(saveDownlinkMsg(ruleChainMetadataUpdateMsg));
            }
        }
        if (downlinkMsg.getDashboardUpdateMsgList() != null && !downlinkMsg.getDashboardUpdateMsgList().isEmpty()) {
            for (DashboardUpdateMsg dashboardUpdateMsg: downlinkMsg.getDashboardUpdateMsgList()) {
                result.add(saveDownlinkMsg(dashboardUpdateMsg));
            }
        }
        if (downlinkMsg.getRelationUpdateMsgList() != null && !downlinkMsg.getRelationUpdateMsgList().isEmpty()) {
            for (RelationUpdateMsg relationUpdateMsg: downlinkMsg.getRelationUpdateMsgList()) {
                result.add(saveDownlinkMsg(relationUpdateMsg));
            }
        }
        if (downlinkMsg.getAlarmUpdateMsgList() != null && !downlinkMsg.getAlarmUpdateMsgList().isEmpty()) {
            for (AlarmUpdateMsg alarmUpdateMsg: downlinkMsg.getAlarmUpdateMsgList()) {
                result.add(saveDownlinkMsg(alarmUpdateMsg));
            }
        }
        if (downlinkMsg.getEntityDataList() != null && !downlinkMsg.getEntityDataList().isEmpty()) {
            for (EntityDataProto entityData: downlinkMsg.getEntityDataList()) {
                result.add(saveDownlinkMsg(entityData));
            }
        }
        if (downlinkMsg.getEntityViewUpdateMsgList() != null && !downlinkMsg.getEntityViewUpdateMsgList().isEmpty()) {
            for (EntityViewUpdateMsg entityViewUpdateMsg: downlinkMsg.getEntityViewUpdateMsgList()) {
                result.add(saveDownlinkMsg(entityViewUpdateMsg));
            }
        }
        if (downlinkMsg.getCustomerUpdateMsgList() != null && !downlinkMsg.getCustomerUpdateMsgList().isEmpty()) {
            for (CustomerUpdateMsg customerUpdateMsg: downlinkMsg.getCustomerUpdateMsgList()) {
                result.add(saveDownlinkMsg(customerUpdateMsg));
            }
        }
        if (downlinkMsg.getWidgetsBundleUpdateMsgList() != null && !downlinkMsg.getWidgetsBundleUpdateMsgList().isEmpty()) {
            for (WidgetsBundleUpdateMsg widgetsBundleUpdateMsg: downlinkMsg.getWidgetsBundleUpdateMsgList()) {
                result.add(saveDownlinkMsg(widgetsBundleUpdateMsg));
            }
        }
        if (downlinkMsg.getWidgetTypeUpdateMsgList() != null && !downlinkMsg.getWidgetTypeUpdateMsgList().isEmpty()) {
            for (WidgetTypeUpdateMsg widgetTypeUpdateMsg: downlinkMsg.getWidgetTypeUpdateMsgList()) {
                result.add(saveDownlinkMsg(widgetTypeUpdateMsg));
            }
        }
        if (downlinkMsg.getUserUpdateMsgList() != null && !downlinkMsg.getUserUpdateMsgList().isEmpty()) {
            for (UserUpdateMsg userUpdateMsg: downlinkMsg.getUserUpdateMsgList()) {
                onUserUpdate(userUpdateMsg);
                result.add(saveDownlinkMsg(userUpdateMsg));
            }
        }
        if (downlinkMsg.getUserCredentialsUpdateMsgList() != null && !downlinkMsg.getUserCredentialsUpdateMsgList().isEmpty()) {
            for (UserCredentialsUpdateMsg userCredentialsUpdateMsg: downlinkMsg.getUserCredentialsUpdateMsgList()) {
                result.add(saveDownlinkMsg(userCredentialsUpdateMsg));
            }
        }
        if (downlinkMsg.getDeviceRpcCallMsgList() != null && !downlinkMsg.getDeviceRpcCallMsgList().isEmpty()) {
            for (DeviceRpcCallMsg deviceRpcCallMsg: downlinkMsg.getDeviceRpcCallMsgList()) {
                result.add(saveDownlinkMsg(deviceRpcCallMsg));
            }
        }
        if (downlinkMsg.getDeviceCredentialsRequestMsgList() != null && !downlinkMsg.getDeviceCredentialsRequestMsgList().isEmpty()) {
            for (DeviceCredentialsRequestMsg deviceCredentialsRequestMsg: downlinkMsg.getDeviceCredentialsRequestMsgList()) {
                result.add(saveDownlinkMsg(deviceCredentialsRequestMsg));
            }
        }
        if (downlinkMsg.getEntityGroupUpdateMsgList() != null && !downlinkMsg.getEntityGroupUpdateMsgList().isEmpty()) {
            for (EntityGroupUpdateMsg entityGroupUpdateMsg: downlinkMsg.getEntityGroupUpdateMsgList()) {
                result.add(saveDownlinkMsg(entityGroupUpdateMsg));
            }
        }
        if (downlinkMsg.getCustomTranslationMsgCount() > 0) {
            for (CustomTranslationProto customTranslationProto : downlinkMsg.getCustomTranslationMsgList()) {
                result.add(saveDownlinkMsg(customTranslationProto));
            }
        }
        if (downlinkMsg.getWhiteLabelingParamsCount() > 0) {
            for (WhiteLabelingParamsProto whiteLabelingParamsProto : downlinkMsg.getWhiteLabelingParamsList()) {
                result.add(saveDownlinkMsg(whiteLabelingParamsProto));
            }
        }
        if (downlinkMsg.getLoginWhiteLabelingParamsCount() > 0) {
            for (LoginWhiteLabelingParamsProto loginWhiteLabelingParamsProto : downlinkMsg.getLoginWhiteLabelingParamsList()) {
                result.add(saveDownlinkMsg(loginWhiteLabelingParamsProto));
            }
        }
        if (downlinkMsg.getSchedulerEventUpdateMsgCount() > 0) {
            for (SchedulerEventUpdateMsg schedulerEventUpdateMsg : downlinkMsg.getSchedulerEventUpdateMsgList()) {
                result.add(saveDownlinkMsg(schedulerEventUpdateMsg));
            }
        }
        if (downlinkMsg.getAdminSettingsUpdateMsgCount() > 0) {
            for (AdminSettingsUpdateMsg adminSettingsUpdateMsg : downlinkMsg.getAdminSettingsUpdateMsgList()) {
                result.add(saveDownlinkMsg(adminSettingsUpdateMsg));
            }
        }
        if (downlinkMsg.getRoleMsgCount() > 0) {
            for (RoleProto roleProto : downlinkMsg.getRoleMsgList()) {
                result.add(saveDownlinkMsg(roleProto));
            }
        }
        if (downlinkMsg.getGroupPermissionMsgCount() > 0) {
            for (GroupPermissionProto groupPermissionProto : downlinkMsg.getGroupPermissionMsgList()) {
                result.add(saveDownlinkMsg(groupPermissionProto));
            }
        }

        return Futures.allAsList(result);
    }

    private ListenableFuture<Void> saveDownlinkMsg(AbstractMessage message) {
        if (!ignoredTypes.contains(message.getClass())) {
            try {
                lock.lock();
                downlinkMsgs.add(message);
            } finally {
                lock.unlock();
            }
            messagesLatch.countDown();
        }
        return Futures.immediateFuture(null);
    }

    public void waitForMessages() throws InterruptedException {
        waitForMessages(5);
    }

    public void waitForMessages(long timeout) throws InterruptedException {
        messagesLatch.await(timeout, TimeUnit.SECONDS);
    }

    public void expectMessageAmount(int messageAmount) {
        messagesLatch = new CountDownLatch(messageAmount);
    }

    public void waitForResponses() throws InterruptedException { responsesLatch.await(5, TimeUnit.SECONDS); }

    public void expectResponsesAmount(int messageAmount) {
        responsesLatch = new CountDownLatch(messageAmount);
    }

    @SuppressWarnings("unchecked")
    public <T extends AbstractMessage> Optional<T> findMessageByType(Class<T> tClass) {
        Optional<T> result;
        try {
            lock.lock();
            result = (Optional<T>) downlinkMsgs.stream().filter(downlinkMsg -> downlinkMsg.getClass().isAssignableFrom(tClass)).findAny();
        } finally {
            lock.unlock();
        }
        return result;
    }

    public AbstractMessage getLatestMessage() {
        return downlinkMsgs.get(downlinkMsgs.size() - 1);
    }

    public void ignoreType(Class<? extends AbstractMessage> type) {
        ignoredTypes.add(type);
    }

    public void allowIgnoredTypes() {
        ignoredTypes.clear();
    }

}
