/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2025 ThingsBoard, Inc. All Rights Reserved.
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
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.translation.CustomTranslation;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.model.ToData;
import org.thingsboard.server.dao.util.mapping.JsonConverter;

import java.io.Serializable;
import java.util.UUID;

import static org.thingsboard.server.dao.model.ModelConstants.CUSTOMER_ID_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.CUSTOM_TRANSLATION_LOCALE_CODE;
import static org.thingsboard.server.dao.model.ModelConstants.TENANT_ID_COLUMN;

@Data
@NoArgsConstructor
@Entity
@Table(name = ModelConstants.CUSTOM_TRANSLATION_TABLE_NAME)
@IdClass(CustomTranslationCompositeKey.class)
public class CustomTranslationEntity implements ToData<CustomTranslation>, Serializable {

    @Id
    @Column(name = TENANT_ID_COLUMN, columnDefinition = "uuid")
    private UUID tenantId;

    @Id
    @Column(name = CUSTOMER_ID_PROPERTY, columnDefinition = "uuid")
    private UUID customerId;

    @Id
    @Column(name = CUSTOM_TRANSLATION_LOCALE_CODE)
    private String localeCode;

    @Convert(converter = JsonConverter.class)
    @Column(name = ModelConstants.CUSTOM_TRANSLATION_VALUE)
    private JsonNode value;

    public CustomTranslationEntity(CustomTranslation customTranslation) {
        this.tenantId = customTranslation.getTenantId().getId();
        if (customTranslation.getCustomerId() != null) {
            this.customerId = customTranslation.getCustomerId().getId();
        } else {
            this.customerId = EntityId.NULL_UUID;
        }
        this.localeCode = customTranslation.getLocaleCode();
        if (customTranslation.getValue() != null) {
            this.value = customTranslation.getValue();
        }
    }

    @Override
    public CustomTranslation toData() {
        CustomTranslation customTranslation = new CustomTranslation();
        customTranslation.setTenantId(TenantId.fromUUID(tenantId));
        if (!EntityId.NULL_UUID.equals(customerId)) {
            customTranslation.setCustomerId(new CustomerId(customerId));
        }
        customTranslation.setLocaleCode(localeCode);
        if (value != null) {
            customTranslation.setValue(value);
        }
        return customTranslation;
    }
}
