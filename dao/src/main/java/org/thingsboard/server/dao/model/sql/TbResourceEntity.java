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
import org.thingsboard.server.common.data.ResourceType;
import org.thingsboard.server.common.data.TbResource;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TbResourceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.model.BaseSqlEntity;
import org.thingsboard.server.dao.util.mapping.JsonStringType;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.util.UUID;

import static org.thingsboard.server.dao.model.ModelConstants.CUSTOMER_ID_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.EXTERNAL_ID_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.RESOURCE_CUSTOMER_ID_COLUMN;
import static org.thingsboard.server.dao.model.ModelConstants.RESOURCE_DATA_COLUMN;
import static org.thingsboard.server.dao.model.ModelConstants.RESOURCE_DESCRIPTOR_COLUMN;
import static org.thingsboard.server.dao.model.ModelConstants.RESOURCE_ETAG_COLUMN;
import static org.thingsboard.server.dao.model.ModelConstants.RESOURCE_FILE_NAME_COLUMN;
import static org.thingsboard.server.dao.model.ModelConstants.RESOURCE_KEY_COLUMN;
import static org.thingsboard.server.dao.model.ModelConstants.RESOURCE_PREVIEW_COLUMN;
import static org.thingsboard.server.dao.model.ModelConstants.RESOURCE_TABLE_NAME;
import static org.thingsboard.server.dao.model.ModelConstants.RESOURCE_TENANT_ID_COLUMN;
import static org.thingsboard.server.dao.model.ModelConstants.RESOURCE_TITLE_COLUMN;
import static org.thingsboard.server.dao.model.ModelConstants.RESOURCE_TYPE_COLUMN;
import static org.thingsboard.server.dao.model.ModelConstants.SEARCH_TEXT_PROPERTY;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@TypeDef(name = "json", typeClass = JsonStringType.class)
@Table(name = RESOURCE_TABLE_NAME)
public class TbResourceEntity extends BaseSqlEntity<TbResource> {

    @Column(name = RESOURCE_TENANT_ID_COLUMN, columnDefinition = "uuid")
    private UUID tenantId;

    @Column(name = RESOURCE_CUSTOMER_ID_COLUMN, columnDefinition = "uuid")
    private UUID customerId;

    @Column(name = RESOURCE_TITLE_COLUMN)
    private String title;

    @Column(name = RESOURCE_TYPE_COLUMN)
    private String resourceType;

    @Column(name = RESOURCE_KEY_COLUMN)
    private String resourceKey;

    @Column(name = SEARCH_TEXT_PROPERTY)
    private String searchText;

    @Column(name = RESOURCE_FILE_NAME_COLUMN)
    private String fileName;

    @Column(name = RESOURCE_DATA_COLUMN)
    private byte[] data;

    @Column(name = RESOURCE_ETAG_COLUMN)
    private String etag;

    @Type(type = "json")
    @Column(name = RESOURCE_DESCRIPTOR_COLUMN)
    private JsonNode descriptor;

    @Column(name = RESOURCE_PREVIEW_COLUMN)
    private byte[] preview;

    @Column(name = EXTERNAL_ID_PROPERTY)
    private UUID externalId;

    public TbResourceEntity() {
    }

    public TbResourceEntity(TbResource resource) {
        if (resource.getId() != null) {
            this.id = resource.getId().getId();
        }
        this.createdTime = resource.getCreatedTime();
        if (resource.getTenantId() != null) {
            this.tenantId = resource.getTenantId().getId();
        }
        if (resource.getCustomerId() != null) {
            this.customerId = resource.getCustomerId().getId();
        }
        this.title = resource.getTitle();
        this.resourceType = resource.getResourceType().name();
        this.resourceKey = resource.getResourceKey();
        this.searchText = resource.getSearchText();
        this.fileName = resource.getFileName();
        this.data = resource.getData();
        this.etag = resource.getEtag();
        this.descriptor = resource.getDescriptor();
        this.preview = resource.getPreview();
        this.externalId = getUuid(resource.getExternalId());
    }

    @Override
    public TbResource toData() {
        TbResource resource = new TbResource(new TbResourceId(id));
        resource.setCreatedTime(createdTime);
        resource.setTenantId(TenantId.fromUUID(tenantId));
        if (customerId != null) {
            resource.setCustomerId(new CustomerId(customerId));
        }
        resource.setTitle(title);
        resource.setResourceType(ResourceType.valueOf(resourceType));
        resource.setResourceKey(resourceKey);
        resource.setSearchText(searchText);
        resource.setFileName(fileName);
        resource.setData(data);
        resource.setEtag(etag);
        resource.setDescriptor(descriptor);
        resource.setPreview(preview);
        resource.setExternalId(getEntityId(externalId, TbResourceId::new));
        return resource;
    }

}
