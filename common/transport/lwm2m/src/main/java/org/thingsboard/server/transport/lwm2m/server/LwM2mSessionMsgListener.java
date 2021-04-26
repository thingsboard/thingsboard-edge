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
package org.thingsboard.server.transport.lwm2m.server;

import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.ResourceType;
import org.thingsboard.server.common.transport.SessionMsgListener;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.gen.transport.TransportProtos.AttributeUpdateNotificationMsg;
import org.thingsboard.server.gen.transport.TransportProtos.GetAttributeResponseMsg;
import org.thingsboard.server.gen.transport.TransportProtos.SessionCloseNotificationProto;
import org.thingsboard.server.gen.transport.TransportProtos.ToDeviceRpcRequestMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToServerRpcResponseMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToTransportUpdateCredentialsProto;

import java.util.Optional;

@Slf4j
public class LwM2mSessionMsgListener implements GenericFutureListener<Future<? super Void>>, SessionMsgListener {
    private LwM2mTransportServiceImpl service;
    private TransportProtos.SessionInfoProto sessionInfo;

    public LwM2mSessionMsgListener(LwM2mTransportServiceImpl service, TransportProtos.SessionInfoProto sessionInfo) {
        this.service = service;
        this.sessionInfo = sessionInfo;
    }

    @Override
    public void onGetAttributesResponse(GetAttributeResponseMsg getAttributesResponse) {
        this.service.onGetAttributesResponse(getAttributesResponse, this.sessionInfo);
    }

    @Override
    public void onAttributeUpdate(AttributeUpdateNotificationMsg attributeUpdateNotification) {
        this.service.onAttributeUpdate(attributeUpdateNotification, this.sessionInfo);
     }

    @Override
    public void onRemoteSessionCloseCommand(SessionCloseNotificationProto sessionCloseNotification) {
        log.info("[{}] sessionCloseNotification", sessionCloseNotification);
    }

    @Override
    public void onToTransportUpdateCredentials(ToTransportUpdateCredentialsProto updateCredentials) {
        this.service.onToTransportUpdateCredentials(updateCredentials);
    }

    @Override
    public void onDeviceProfileUpdate(TransportProtos.SessionInfoProto sessionInfo, DeviceProfile deviceProfile) {
        this.service.onDeviceProfileUpdate(sessionInfo, deviceProfile);
    }

    @Override
    public void onDeviceUpdate(TransportProtos.SessionInfoProto sessionInfo, Device device, Optional<DeviceProfile> deviceProfileOpt) {
        this.service.onDeviceUpdate(sessionInfo, device, deviceProfileOpt);
    }

    @Override
    public void onToDeviceRpcRequest(ToDeviceRpcRequestMsg toDeviceRequest) {
        this.service.onToDeviceRpcRequest(toDeviceRequest,this.sessionInfo);
    }

    @Override
    public void onToServerRpcResponse(ToServerRpcResponseMsg toServerResponse) {
        this.service.onToServerRpcResponse(toServerResponse);
    }

    @Override
    public void operationComplete(Future<? super Void> future) throws Exception {
        log.info("[{}]  operationComplete", future);
    }

    @Override
    public void onResourceUpdate(@NotNull Optional<TransportProtos.ResourceUpdateMsg> resourceUpdateMsgOpt) {
        if (ResourceType.LWM2M_MODEL.name().equals(resourceUpdateMsgOpt.get().getResourceType())) {
            this.service.onResourceUpdate(resourceUpdateMsgOpt);
        }
    }

    @Override
    public void onResourceDelete(@NotNull Optional<TransportProtos.ResourceDeleteMsg> resourceDeleteMsgOpt) {
        if (ResourceType.LWM2M_MODEL.name().equals(resourceDeleteMsgOpt.get().getResourceType())) {
            this.service.onResourceDelete(resourceDeleteMsgOpt);
        }
    }
}
