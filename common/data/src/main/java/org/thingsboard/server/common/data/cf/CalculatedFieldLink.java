/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2024 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.common.data.cf;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.BaseData;
import org.thingsboard.server.common.data.id.CalculatedFieldId;
import org.thingsboard.server.common.data.id.CalculatedFieldLinkId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;

@Schema
@Data
@EqualsAndHashCode(callSuper = true)
public class CalculatedFieldLink extends BaseData<CalculatedFieldLinkId> {

    private static final long serialVersionUID = 6492846246722091530L;

    private TenantId tenantId;
    private EntityId entityId;

    @Schema(description = "JSON object with the Calculated Field Id. ", accessMode = Schema.AccessMode.READ_ONLY)
    private CalculatedFieldId calculatedFieldId;
    @Schema
    private transient CalculatedFieldLinkConfiguration configuration;

    public CalculatedFieldLink() {
        super();
    }

    public CalculatedFieldLink(CalculatedFieldLinkId id) {
        super(id);
    }

    public CalculatedFieldLink(TenantId tenantId, EntityId entityId, CalculatedFieldId calculatedFieldId, CalculatedFieldLinkConfiguration configuration) {
        this.tenantId = tenantId;
        this.entityId = entityId;
        this.calculatedFieldId = calculatedFieldId;
        this.configuration = configuration;
    }

    @Override
    public String toString() {
        return new StringBuilder()
                .append("CalculatedFieldLink[")
                .append("tenantId=").append(tenantId)
                .append(", entityId=").append(entityId)
                .append(", calculatedFieldId=").append(calculatedFieldId)
                .append(", configuration=").append(configuration)
                .append(", createdTime=").append(createdTime)
                .append(", id=").append(id).append(']')
                .toString();
    }

}
