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
package org.thingsboard.server.dao.model.sql;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.cf.CalculatedFieldLink;
import org.thingsboard.server.common.data.cf.CalculatedFieldLinkConfiguration;
import org.thingsboard.server.common.data.id.CalculatedFieldId;
import org.thingsboard.server.common.data.id.CalculatedFieldLinkId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.model.BaseEntity;
import org.thingsboard.server.dao.model.BaseSqlEntity;
import org.thingsboard.server.dao.util.mapping.JsonConverter;

import java.util.UUID;

import static org.thingsboard.server.dao.model.ModelConstants.CALCULATED_FIELD_LINK_CALCULATED_FIELD_ID;
import static org.thingsboard.server.dao.model.ModelConstants.CALCULATED_FIELD_LINK_CONFIGURATION;
import static org.thingsboard.server.dao.model.ModelConstants.CALCULATED_FIELD_LINK_ENTITY_ID;
import static org.thingsboard.server.dao.model.ModelConstants.CALCULATED_FIELD_LINK_ENTITY_TYPE;
import static org.thingsboard.server.dao.model.ModelConstants.CALCULATED_FIELD_LINK_TABLE_NAME;
import static org.thingsboard.server.dao.model.ModelConstants.CALCULATED_FIELD_LINK_TENANT_ID_COLUMN;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = CALCULATED_FIELD_LINK_TABLE_NAME)
public class CalculatedFieldLinkEntity extends BaseSqlEntity<CalculatedFieldLink> implements BaseEntity<CalculatedFieldLink> {

    @Column(name = CALCULATED_FIELD_LINK_TENANT_ID_COLUMN)
    private UUID tenantId;

    @Column(name = CALCULATED_FIELD_LINK_ENTITY_TYPE)
    private String entityType;

    @Column(name = CALCULATED_FIELD_LINK_ENTITY_ID)
    private UUID entityId;

    @Column(name = CALCULATED_FIELD_LINK_CALCULATED_FIELD_ID)
    private UUID calculatedFieldId;

    @Convert(converter = JsonConverter.class)
    @Column(name = CALCULATED_FIELD_LINK_CONFIGURATION)
    private JsonNode configuration;

    public CalculatedFieldLinkEntity() {
        super();
    }

    public CalculatedFieldLinkEntity(CalculatedFieldLink calculatedFieldLink) {
        this.setUuid(calculatedFieldLink.getUuidId());
        this.createdTime = calculatedFieldLink.getCreatedTime();
        this.tenantId = calculatedFieldLink.getTenantId().getId();
        this.entityType = calculatedFieldLink.getEntityId().getEntityType().name();
        this.entityId = calculatedFieldLink.getEntityId().getId();
        this.calculatedFieldId = calculatedFieldLink.getCalculatedFieldId().getId();
        this.configuration = JacksonUtil.valueToTree(calculatedFieldLink.getConfiguration());
    }

    @Override
    public CalculatedFieldLink toData() {
        CalculatedFieldLink calculatedFieldLink = new CalculatedFieldLink(new CalculatedFieldLinkId(id));
        calculatedFieldLink.setCreatedTime(createdTime);
        calculatedFieldLink.setTenantId(TenantId.fromUUID(tenantId));
        calculatedFieldLink.setEntityId(EntityIdFactory.getByTypeAndUuid(entityType, entityId));
        calculatedFieldLink.setCalculatedFieldId(new CalculatedFieldId(calculatedFieldId));
        calculatedFieldLink.setConfiguration(JacksonUtil.treeToValue(configuration, CalculatedFieldLinkConfiguration.class));
        return calculatedFieldLink;
    }

}
