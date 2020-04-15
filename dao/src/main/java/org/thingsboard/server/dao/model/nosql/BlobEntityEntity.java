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
package org.thingsboard.server.dao.model.nosql;

import com.datastax.driver.core.utils.UUIDs;
import com.datastax.driver.mapping.annotations.Column;
import com.datastax.driver.mapping.annotations.PartitionKey;
import com.datastax.driver.mapping.annotations.Table;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.thingsboard.server.common.data.blob.BlobEntity;
import org.thingsboard.server.common.data.id.BlobEntityId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.model.SearchTextEntity;
import org.thingsboard.server.dao.model.type.JsonCodec;

import java.nio.ByteBuffer;
import java.util.UUID;

import static org.thingsboard.server.dao.model.ModelConstants.*;

@Table(name = BLOB_ENTITY_COLUMN_FAMILY_NAME)
@EqualsAndHashCode
@ToString
public final class BlobEntityEntity implements SearchTextEntity<BlobEntity> {

    @PartitionKey(value = 0)
    @Column(name = ID_PROPERTY)
    private UUID id;

    @PartitionKey(value = 1)
    @Column(name = BLOB_ENTITY_TENANT_ID_PROPERTY)
    private UUID tenantId;

    @PartitionKey(value = 2)
    @Column(name = BLOB_ENTITY_CUSTOMER_ID_PROPERTY)
    private UUID customerId;

    @PartitionKey(value = 3)
    @Column(name = BLOB_ENTITY_TYPE_PROPERTY)
    private String type;

    @Column(name = BLOB_ENTITY_NAME_PROPERTY)
    private String name;

    @Column(name = BLOB_ENTITY_CONTENT_TYPE_PROPERTY)
    private String contentType;

    @Column(name = SEARCH_TEXT_PROPERTY)
    private String searchText;

    @Column(name = BLOB_ENTITY_DATA_PROPERTY)
    private ByteBuffer data;

    @Column(name = BLOB_ENTITY_ADDITIONAL_INFO_PROPERTY, codec = JsonCodec.class)
    private JsonNode additionalInfo;


    public BlobEntityEntity() {
        super();
    }

    public BlobEntityEntity(BlobEntity blobEntity) {
        if (blobEntity.getId() != null) {
            this.id = blobEntity.getId().getId();
        }
        if (blobEntity.getTenantId() != null) {
            this.tenantId = blobEntity.getTenantId().getId();
        }
        if (blobEntity.getCustomerId() != null) {
            this.customerId = blobEntity.getCustomerId().getId();
        }
        this.name = blobEntity.getName();
        this.type = blobEntity.getType();
        this.contentType = blobEntity.getContentType();
        this.data = blobEntity.getData();
        this.additionalInfo = blobEntity.getAdditionalInfo();
    }

    public UUID getUuid() {
        return id;
    }

    public void setUuid(UUID id) {
        this.id = id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public void setTenantId(UUID tenantId) {
        this.tenantId = tenantId;
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public void setCustomerId(UUID customerId) {
        this.customerId = customerId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public ByteBuffer getData() {
        return data;
    }

    public void setData(ByteBuffer data) {
        this.data = data;
    }

    public JsonNode getAdditionalInfo() {
        return additionalInfo;
    }

    public void setAdditionalInfo(JsonNode additionalInfo) {
        this.additionalInfo = additionalInfo;
    }

    @Override
    public String getSearchTextSource() {
        return getName();
    }

    @Override
    public void setSearchText(String searchText) {
        this.searchText = searchText;
    }

    public String getSearchText() {
        return searchText;
    }

    @Override
    public BlobEntity toData() {
        BlobEntity blobEntity = new BlobEntity(new BlobEntityId(id));
        blobEntity.setCreatedTime(UUIDs.unixTimestamp(id));
        if (tenantId != null) {
            blobEntity.setTenantId(new TenantId(tenantId));
        }
        if (customerId != null) {
            blobEntity.setCustomerId(new CustomerId(customerId));
        }
        blobEntity.setName(name);
        blobEntity.setType(type);
        blobEntity.setContentType(contentType);
        blobEntity.setData(data);
        blobEntity.setAdditionalInfo(additionalInfo);
        return blobEntity;
    }

}