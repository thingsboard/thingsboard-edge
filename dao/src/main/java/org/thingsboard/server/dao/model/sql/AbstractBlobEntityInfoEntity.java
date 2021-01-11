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
import org.thingsboard.server.common.data.UUIDConverter;
import org.thingsboard.server.common.data.blob.BlobEntityInfo;
import org.thingsboard.server.common.data.id.BlobEntityId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.model.BaseSqlEntity;
import org.thingsboard.server.dao.model.SearchTextEntity;
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
import static org.thingsboard.server.dao.model.ModelConstants.SEARCH_TEXT_PROPERTY;

@Data
@EqualsAndHashCode(callSuper = true)
@TypeDef(name = "json", typeClass = JsonStringType.class)
@MappedSuperclass
public abstract class AbstractBlobEntityInfoEntity<T extends BlobEntityInfo> extends BaseSqlEntity<T> implements SearchTextEntity<T> {

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
        this.searchText = blobEntityInfoEntity.getSearchText();
        this.additionalInfo = blobEntityInfoEntity.getAdditionalInfo();
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
