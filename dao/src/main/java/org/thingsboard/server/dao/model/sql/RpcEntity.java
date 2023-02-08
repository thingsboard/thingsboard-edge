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
package org.thingsboard.server.dao.model.sql;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.RpcId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.rpc.Rpc;
import org.thingsboard.server.common.data.rpc.RpcStatus;
import org.thingsboard.server.dao.model.BaseEntity;
import org.thingsboard.server.dao.model.BaseSqlEntity;
import org.thingsboard.server.dao.util.mapping.JsonStringType;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Table;
import java.util.UUID;

import static org.thingsboard.server.dao.model.ModelConstants.RPC_ADDITIONAL_INFO;
import static org.thingsboard.server.dao.model.ModelConstants.RPC_DEVICE_ID;
import static org.thingsboard.server.dao.model.ModelConstants.RPC_EXPIRATION_TIME;
import static org.thingsboard.server.dao.model.ModelConstants.RPC_REQUEST;
import static org.thingsboard.server.dao.model.ModelConstants.RPC_RESPONSE;
import static org.thingsboard.server.dao.model.ModelConstants.RPC_STATUS;
import static org.thingsboard.server.dao.model.ModelConstants.RPC_TABLE_NAME;
import static org.thingsboard.server.dao.model.ModelConstants.RPC_TENANT_ID_COLUMN;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@TypeDef(name = "json", typeClass = JsonStringType.class)
@Table(name = RPC_TABLE_NAME)
public class RpcEntity extends BaseSqlEntity<Rpc> implements BaseEntity<Rpc> {

    @Column(name = RPC_TENANT_ID_COLUMN)
    private UUID tenantId;

    @Column(name = RPC_DEVICE_ID)
    private UUID deviceId;

    @Column(name = RPC_EXPIRATION_TIME)
    private long expirationTime;

    @Type(type = "json")
    @Column(name = RPC_REQUEST)
    private JsonNode request;

    @Type(type = "json")
    @Column(name = RPC_RESPONSE)
    private JsonNode response;

    @Enumerated(EnumType.STRING)
    @Column(name = RPC_STATUS)
    private RpcStatus status;

    @Type(type = "json")
    @Column(name = RPC_ADDITIONAL_INFO)
    private JsonNode additionalInfo;

    public RpcEntity() {
        super();
    }

    public RpcEntity(Rpc rpc) {
        this.setUuid(rpc.getUuidId());
        this.createdTime = rpc.getCreatedTime();
        this.tenantId = rpc.getTenantId().getId();
        this.deviceId = rpc.getDeviceId().getId();
        this.expirationTime = rpc.getExpirationTime();
        this.request = rpc.getRequest();
        this.response = rpc.getResponse();
        this.status = rpc.getStatus();
        this.additionalInfo = rpc.getAdditionalInfo();
    }

    @Override
    public Rpc toData() {
        Rpc rpc = new Rpc(new RpcId(id));
        rpc.setCreatedTime(createdTime);
        rpc.setTenantId(TenantId.fromUUID(tenantId));
        rpc.setDeviceId(new DeviceId(deviceId));
        rpc.setExpirationTime(expirationTime);
        rpc.setRequest(request);
        rpc.setResponse(response);
        rpc.setStatus(status);
        rpc.setAdditionalInfo(additionalInfo);
        return rpc;
    }
}
