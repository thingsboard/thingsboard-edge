/**
 * Thingsboard OÜ ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2018 Thingsboard OÜ. All Rights Reserved.
 *
 * NOTICE: All information contained herein is, and remains
 * the property of Thingsboard OÜ and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Thingsboard OÜ
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
package org.thingsboard.server.common.msg.device;

import lombok.ToString;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.SessionId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.cluster.ServerAddress;
import org.thingsboard.server.common.msg.session.FromDeviceMsg;
import org.thingsboard.server.common.msg.session.SessionType;
import org.thingsboard.server.common.msg.session.ToDeviceActorSessionMsg;

import java.util.Optional;

@ToString
public class BasicToDeviceActorMsg implements ToDeviceActorMsg {

    private static final long serialVersionUID = -1866795134993115408L;

    private final TenantId tenantId;
    private final CustomerId customerId;
    private final DeviceId deviceId;
    private final SessionId sessionId;
    private final SessionType sessionType;
    private final ServerAddress serverAddress;
    private final FromDeviceMsg msg;

    public BasicToDeviceActorMsg(ToDeviceActorMsg other, FromDeviceMsg msg) {
        this(null, other.getTenantId(), other.getCustomerId(), other.getDeviceId(), other.getSessionId(), other.getSessionType(), msg);
    }

    public BasicToDeviceActorMsg(ToDeviceActorSessionMsg msg, SessionType sessionType) {
        this(null, msg.getTenantId(), msg.getCustomerId(), msg.getDeviceId(), msg.getSessionId(), sessionType, msg.getSessionMsg().getMsg());
    }

    private BasicToDeviceActorMsg(ServerAddress serverAddress, TenantId tenantId, CustomerId customerId, DeviceId deviceId, SessionId sessionId, SessionType sessionType,
                                  FromDeviceMsg msg) {
        super();
        this.serverAddress = serverAddress;
        this.tenantId = tenantId;
        this.customerId = customerId;
        this.deviceId = deviceId;
        this.sessionId = sessionId;
        this.sessionType = sessionType;
        this.msg = msg;
    }

    @Override
    public DeviceId getDeviceId() {
        return deviceId;
    }

    @Override
    public CustomerId getCustomerId() {
        return customerId;
    }

    public TenantId getTenantId() {
        return tenantId;
    }

    @Override
    public SessionId getSessionId() {
        return sessionId;
    }

    @Override
    public SessionType getSessionType() {
        return sessionType;
    }

    @Override
    public Optional<ServerAddress> getServerAddress() {
        return Optional.ofNullable(serverAddress);
    }

    @Override
    public FromDeviceMsg getPayload() {
        return msg;
    }

    @Override
    public ToDeviceActorMsg toOtherAddress(ServerAddress otherAddress) {
        return new BasicToDeviceActorMsg(otherAddress, tenantId, customerId, deviceId, sessionId, sessionType, msg);
    }
}
