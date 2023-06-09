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
import org.thingsboard.server.common.data.blob.BlobEntityInfo;
import org.thingsboard.server.common.data.id.BlobEntityId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.model.BaseEntity;
import org.thingsboard.server.dao.model.BaseSqlEntity;
import org.thingsboard.server.dao.util.mapping.JsonStringType;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;
import java.util.UUID;

import static org.thingsboard.server.dao.model.ModelConstants.BLOB_ENTITY_ADDITIONAL_INFO_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.BLOB_ENTITY_CONTENT_TYPE_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.BLOB_ENTITY_CUSTOMER_ID_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.BLOB_ENTITY_NAME_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.BLOB_ENTITY_TENANT_ID_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.BLOB_ENTITY_TYPE_PROPERTY;

@Data
@EqualsAndHashCode(callSuper = true)
@TypeDef(name = "json", typeClass = JsonStringType.class)
@MappedSuperclass
public abstract class AbstractBlobEntityInfoEntity<T extends BlobEntityInfo> extends BaseSqlEntity<T> implements BaseEntity<T> {

    @Column(name = BLOB_ENTITY_TENANT_ID_PROPERTY)
    private UUID tenantId;

    @Column(name = BLOB_ENTITY_CUSTOMER_ID_PROPERTY)
    private UUID customerId;

    @Column(name = BLOB_ENTITY_NAME_PROPERTY)
    private String name;

    @Column(name = BLOB_ENTITY_TYPE_PROPERTY)
    private String type;

    @Column(name = BLOB_ENTITY_CONTENT_TYPE_PROPERTY)
    private String contentType;

    @Type(type = "json")
    @Column(name = BLOB_ENTITY_ADDITIONAL_INFO_PROPERTY)
    private JsonNode additionalInfo;

    public AbstractBlobEntityInfoEntity() {
        super();
    }

    public AbstractBlobEntityInfoEntity(BlobEntityInfo blobEntityInfo) {
        this.createdTime = blobEntityInfo.getCreatedTime();
        if (blobEntityInfo.getId() != null) {
            this.setUuid(blobEntityInfo.getId().getId());
        }
        if (blobEntityInfo.getTenantId() != null) {
            this.tenantId = blobEntityInfo.getTenantId().getId();
        }
        if (blobEntityInfo.getCustomerId() != null) {
            this.customerId = blobEntityInfo.getCustomerId().getId();
        }
        this.name = blobEntityInfo.getName();
        this.type = blobEntityInfo.getType();
        this.contentType = blobEntityInfo.getContentType();
        this.additionalInfo = blobEntityInfo.getAdditionalInfo();
    }

    public AbstractBlobEntityInfoEntity(BlobEntityInfoEntity blobEntityInfoEntity) {
        this.setId(blobEntityInfoEntity.getId());
        this.setCreatedTime(blobEntityInfoEntity.getCreatedTime());
        this.tenantId = blobEntityInfoEntity.getTenantId();
        this.customerId = blobEntityInfoEntity.getCustomerId();
        this.type = blobEntityInfoEntity.getType();
        this.name = blobEntityInfoEntity.getName();
        this.contentType = blobEntityInfoEntity.getContentType();
        this.additionalInfo = blobEntityInfoEntity.getAdditionalInfo();
    }

    protected BlobEntityInfo toBlobEntityInfo() {
        BlobEntityInfo blobEntityInfo = new BlobEntityInfo(new BlobEntityId(id));
        blobEntityInfo.setCreatedTime(createdTime);
        if (tenantId != null) {
            blobEntityInfo.setTenantId(new TenantId(tenantId));
        }
        if (customerId != null) {
            blobEntityInfo.setCustomerId(new CustomerId(customerId));
        }
        blobEntityInfo.setName(name);
        blobEntityInfo.setType(type);
        blobEntityInfo.setContentType(contentType);
        blobEntityInfo.setAdditionalInfo(additionalInfo);
        return blobEntityInfo;
    }
}
