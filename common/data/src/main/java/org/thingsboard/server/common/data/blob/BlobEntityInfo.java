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
package org.thingsboard.server.common.data.blob;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.BaseDataWithAdditionalInfo;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.HasCustomerId;
import org.thingsboard.server.common.data.HasName;
import org.thingsboard.server.common.data.HasOwnerId;
import org.thingsboard.server.common.data.TenantEntity;
import org.thingsboard.server.common.data.id.BlobEntityId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.validation.Length;

@Data
@EqualsAndHashCode(callSuper = true)
public class BlobEntityInfo extends BaseDataWithAdditionalInfo<BlobEntityId> implements HasName, TenantEntity, HasCustomerId, HasOwnerId {

    private static final long serialVersionUID = 2807223040519549363L;

    @ApiModelProperty(position = 3, value = "JSON object with Tenant Id", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    private TenantId tenantId;
    @ApiModelProperty(position = 4, value = "JSON object with Customer Id", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    private CustomerId customerId;
    @Length(fieldName = "name")
    @ApiModelProperty(position = 6, value = "blob entity name", accessMode = ApiModelProperty.AccessMode.READ_ONLY, example = "report-2021-10-29_14:00:00.pdf")
    private String name;
    @Length(fieldName = "type")
    @ApiModelProperty(position = 7, value = "blob entity type", accessMode = ApiModelProperty.AccessMode.READ_ONLY, example = "report")
    private String type;
    @Length(fieldName = "contentType")
    @ApiModelProperty(position = 8, value = "blob content type", accessMode = ApiModelProperty.AccessMode.READ_ONLY, example = "application/pdf", allowableValues = "application/pdf, image/jpeg, image/png")
    private String contentType;

    public BlobEntityInfo() {
        super();
    }

    public BlobEntityInfo(BlobEntityId id) {
        super(id);
    }

    public BlobEntityInfo(BlobEntityInfo blobEntityInfo) {
        super(blobEntityInfo);
        this.tenantId = blobEntityInfo.getTenantId();
        this.customerId = blobEntityInfo.getCustomerId();
        this.name = blobEntityInfo.getName();
        this.type = blobEntityInfo.getType();
        this.contentType = blobEntityInfo.getContentType();
    }

    @ApiModelProperty(position = 1, value = "JSON object with the blob entity Id. " +
            "Referencing non-existing blob entity Id will cause error")
    @Override
    public BlobEntityId getId() {
        return super.getId();
    }

    @ApiModelProperty(position = 2, value = "Timestamp of the blob entity creation, in milliseconds", example = "1609459200000", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    @Override
    public long getCreatedTime() {
        return super.getCreatedTime();
    }

    @Override
    public String getName() {
        return name;
    }

    @ApiModelProperty(position = 9, value = "Additional parameters of the blob entity", dataType = "com.fasterxml.jackson.databind.JsonNode")
    @Override
    public JsonNode getAdditionalInfo() {
        return super.getAdditionalInfo();
    }


    @Override
    @JsonIgnore
    public EntityType getEntityType() {
        return EntityType.BLOB_ENTITY;
    }

    @ApiModelProperty(position = 5, value = "JSON object with Customer or Tenant Id", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    @Override
    public EntityId getOwnerId() {
        return customerId != null && !customerId.isNullUid() ? customerId : tenantId;
    }

    @Override
    public void setOwnerId(EntityId entityId) {
        if (EntityType.CUSTOMER.equals(entityId.getEntityType())) {
            this.customerId = new CustomerId(entityId.getId());
        } else {
            this.customerId = new CustomerId(CustomerId.NULL_UUID);
        }
    }
}
