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
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.wl.WhiteLabeling;
import org.thingsboard.server.common.data.wl.WhiteLabelingType;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.model.ToData;
import org.thingsboard.server.dao.util.mapping.JsonStringType;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.Table;
import java.io.Serializable;
import java.util.UUID;

import static org.thingsboard.server.dao.model.ModelConstants.CUSTOMER_ID_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.ENTITY_ID_COLUMN;
import static org.thingsboard.server.dao.model.ModelConstants.ENTITY_TYPE_COLUMN;
import static org.thingsboard.server.dao.model.ModelConstants.TENANT_ID_COLUMN;
import static org.thingsboard.server.dao.model.ModelConstants.WHITE_LABELING_SETTINGS_TYPE;

@Data
@NoArgsConstructor
@TypeDef(name = "json", typeClass = JsonStringType.class)
@Entity
@Table(name = ModelConstants.WHITE_LABELING_TABLE_NAME)
@IdClass(WhiteLabelingCompositeKey.class)
public class WhiteLabelingEntity implements ToData<WhiteLabeling>, Serializable {

    @Id
    @Column(name = TENANT_ID_COLUMN, columnDefinition = "uuid")
    private UUID tenantId;

    @Id
    @Column(name = CUSTOMER_ID_PROPERTY, columnDefinition = "uuid")
    private UUID customerId;

    @Id
    @Enumerated(EnumType.STRING)
    @Column(name = WHITE_LABELING_SETTINGS_TYPE)
    private WhiteLabelingType type;

    @Type(type = "json")
    @Column(name = ModelConstants.WHITE_LABELING_SETTINGS)
    private JsonNode settings;

    @Column(name = ModelConstants.WHITE_LABELING_DOMAIN)
    private String domain;

    public WhiteLabelingEntity(WhiteLabeling whiteLabeling) {
        this.tenantId = whiteLabeling.getTenantId().getId();
        if (whiteLabeling.getCustomerId() != null) {
            this.customerId = whiteLabeling.getCustomerId().getId();
        } else {
            this.customerId = EntityId.NULL_UUID;
        }
        this.type = whiteLabeling.getType();
        if (whiteLabeling.getSettings() != null) {
            this.settings = whiteLabeling.getSettings();
        }
        this.domain = whiteLabeling.getDomain();
    }

    @Override
    public WhiteLabeling toData() {
        WhiteLabeling whiteLabeling = new WhiteLabeling();
        whiteLabeling.setTenantId(TenantId.fromUUID(tenantId));
        if (!EntityId.NULL_UUID.equals(customerId)) {
            whiteLabeling.setCustomerId(new CustomerId(customerId));
        }
        whiteLabeling.setType(type);
        if (settings != null) {
            whiteLabeling.setSettings(settings);
        }
        whiteLabeling.setDomain(domain);
        return whiteLabeling;
    }
}
