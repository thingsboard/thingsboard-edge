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
package org.thingsboard.server.common.data.rpc;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.BaseData;
import org.thingsboard.server.common.data.HasTenantId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.RpcId;
import org.thingsboard.server.common.data.id.TenantId;

@Schema
@Data
@EqualsAndHashCode(callSuper = true)
public class Rpc extends BaseData<RpcId> implements HasTenantId {

    @Schema(description = "JSON object with Tenant Id.", accessMode = Schema.AccessMode.READ_ONLY)
    private TenantId tenantId;
    @Schema(description = "JSON object with Device Id.", accessMode = Schema.AccessMode.READ_ONLY)
    private DeviceId deviceId;
    @Schema(description = "Expiration time of the request.", accessMode = Schema.AccessMode.READ_ONLY)
    private long expirationTime;
    @Schema(description = "The request body that will be used to send message to device.", accessMode = Schema.AccessMode.READ_ONLY)
    private JsonNode request;
    @Schema(description = "The response from the device.", accessMode = Schema.AccessMode.READ_ONLY)
    private JsonNode response;
    @Schema(description = "The current status of the RPC call.", accessMode = Schema.AccessMode.READ_ONLY)
    private RpcStatus status;
    @Schema(description = "Additional info used in the rule engine to process the updates to the RPC state.", accessMode = Schema.AccessMode.READ_ONLY)
    private JsonNode additionalInfo;

    public Rpc() {
        super();
    }

    public Rpc(RpcId id) {
        super(id);
    }

    public Rpc(Rpc rpc) {
        super(rpc);
        this.tenantId = rpc.getTenantId();
        this.deviceId = rpc.getDeviceId();
        this.expirationTime = rpc.getExpirationTime();
        this.request = rpc.getRequest();
        this.response = rpc.getResponse();
        this.status = rpc.getStatus();
        this.additionalInfo = rpc.getAdditionalInfo();
    }

    @Schema(description = "JSON object with the rpc Id. Referencing non-existing rpc Id will cause error.")
    @Override
    public RpcId getId() {
        return super.getId();
    }

    @Schema(description = "Timestamp of the rpc creation, in milliseconds", example = "1609459200000", accessMode = Schema.AccessMode.READ_ONLY)
    @Override
    public long getCreatedTime() {
        return super.getCreatedTime();
    }

}
