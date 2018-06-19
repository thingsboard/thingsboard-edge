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
package org.thingsboard.server.dao.model.sql;

import com.datastax.driver.core.utils.UUIDs;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.thingsboard.server.common.data.UUIDConverter;
import org.thingsboard.server.common.data.blob.BlobEntityInfo;
import org.thingsboard.server.common.data.id.BlobEntityId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.SchedulerEventId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.scheduler.SchedulerEventInfo;
import org.thingsboard.server.dao.model.BaseSqlEntity;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.model.SearchTextEntity;
import org.thingsboard.server.dao.util.mapping.JsonStringType;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

import static org.thingsboard.server.dao.model.ModelConstants.*;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@TypeDef(name = "json", typeClass = JsonStringType.class)
@Table(name = BLOB_ENTITY_COLUMN_FAMILY_NAME)
public final class BlobEntityInfoEntity extends BaseSqlEntity<BlobEntityInfo> implements SearchTextEntity<BlobEntityInfo> {

    @Column(name = BLOB_ENTITY_TENANT_ID_PROPERTY)
    private String tenantId;

    @Column(name = BLOB_ENTITY_CUSTOMER_ID_PROPERTY)
    private String customerId;

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

    public BlobEntityInfoEntity() {
        super();
    }

    public BlobEntityInfoEntity(BlobEntityInfo blobEntityInfo) {
        if (blobEntityInfo.getId() != null) {
            this.setId(blobEntityInfo.getId().getId());
        }
        if (blobEntityInfo.getTenantId() != null) {
            this.tenantId = UUIDConverter.fromTimeUUID(blobEntityInfo.getTenantId().getId());
        }
        if (blobEntityInfo.getCustomerId() != null) {
            this.customerId = UUIDConverter.fromTimeUUID(blobEntityInfo.getCustomerId().getId());
        }
        this.name = blobEntityInfo.getName();
        this.type = blobEntityInfo.getType();
        this.contentType = blobEntityInfo.getContentType();
        this.additionalInfo = blobEntityInfo.getAdditionalInfo();
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
    public BlobEntityInfo toData() {
        BlobEntityInfo blobEntityInfo = new BlobEntityInfo(new BlobEntityId(UUIDConverter.fromString(id)));
        blobEntityInfo.setCreatedTime(UUIDs.unixTimestamp(UUIDConverter.fromString(id)));
        if (tenantId != null) {
            blobEntityInfo.setTenantId(new TenantId(UUIDConverter.fromString(tenantId)));
        }
        if (customerId != null) {
            blobEntityInfo.setCustomerId(new CustomerId(UUIDConverter.fromString(customerId)));
        }
        blobEntityInfo.setName(name);
        blobEntityInfo.setType(type);
        blobEntityInfo.setContentType(contentType);
        blobEntityInfo.setAdditionalInfo(additionalInfo);
        return blobEntityInfo;
    }

}