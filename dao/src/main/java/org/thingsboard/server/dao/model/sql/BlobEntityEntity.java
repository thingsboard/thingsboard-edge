/**
 * Copyright © 2016-2021 ThingsBoard, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.dao.model.sql;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.springframework.util.Base64Utils;
import org.thingsboard.server.common.data.UUIDConverter;
import org.thingsboard.server.common.data.blob.BlobEntity;
import org.thingsboard.server.common.data.id.BlobEntityId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.model.BaseSqlEntity;
import org.thingsboard.server.dao.model.SearchTextEntity;
import org.thingsboard.server.dao.util.mapping.JsonStringType;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.nio.ByteBuffer;
import java.util.UUID;

import static org.thingsboard.server.dao.model.ModelConstants.*;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@TypeDef(name = "json", typeClass = JsonStringType.class)
@Table(name = BLOB_ENTITY_COLUMN_FAMILY_NAME)
public final class BlobEntityEntity extends BaseSqlEntity<BlobEntity> implements SearchTextEntity<BlobEntity> {

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

    @Column(name = SEARCH_TEXT_PROPERTY)
    private String searchText;

    @Column(name = BLOB_ENTITY_DATA_PROPERTY)
    private String data;

    @Type(type = "json")
    @Column(name = BLOB_ENTITY_ADDITIONAL_INFO_PROPERTY)
    private JsonNode additionalInfo;

    public BlobEntityEntity() {
        super();
    }

    public BlobEntityEntity(BlobEntity blobEntity) {
        this.createdTime = blobEntity.getCreatedTime();
        if (blobEntity.getId() != null) {
            this.setUuid(blobEntity.getId().getId());
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
        this.additionalInfo = blobEntity.getAdditionalInfo();
        this.data = Base64Utils.encodeToString(blobEntity.getData().array());
    }

    @Override
    public String getSearchTextSource() {
        return name;
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
        blobEntity.setCreatedTime(createdTime);
        if (tenantId != null) {
            blobEntity.setTenantId(new TenantId(tenantId));
        }
        if (customerId != null) {
            blobEntity.setCustomerId(new CustomerId(customerId));
        }
        blobEntity.setName(name);
        blobEntity.setType(type);
        blobEntity.setContentType(contentType);
        blobEntity.setAdditionalInfo(additionalInfo);
        blobEntity.setData(ByteBuffer.wrap(Base64Utils.decodeFromString(data)));
        return blobEntity;
    }

}
