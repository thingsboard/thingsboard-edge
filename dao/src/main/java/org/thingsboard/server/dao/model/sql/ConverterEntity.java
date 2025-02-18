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
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.converter.Converter;
import org.thingsboard.server.common.data.converter.ConverterType;
import org.thingsboard.server.common.data.debug.DebugSettings;
import org.thingsboard.server.common.data.id.ConverterId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.integration.IntegrationType;
import org.thingsboard.server.dao.model.BaseVersionedEntity;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.util.mapping.JsonConverter;

import java.util.UUID;

import static org.thingsboard.server.dao.model.ModelConstants.CONVERTER_INTEGRATION_TYPE_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.CONVERTER_IS_EDGE_TEMPLATE_MODE_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.CONVERTER_NAME_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.CONVERTER_TABLE_NAME;
import static org.thingsboard.server.dao.model.ModelConstants.CONVERTER_TENANT_ID_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.CONVERTER_TYPE_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.CONVERTER_VERSION_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.EXTERNAL_ID_PROPERTY;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = CONVERTER_TABLE_NAME)
public final class ConverterEntity extends BaseVersionedEntity<Converter> {

    @Column(name = CONVERTER_TENANT_ID_PROPERTY)
    private UUID tenantId;

    @Column(name = CONVERTER_NAME_PROPERTY)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = CONVERTER_TYPE_PROPERTY)
    private ConverterType type;

    @Enumerated(EnumType.STRING)
    @Column(name = CONVERTER_INTEGRATION_TYPE_PROPERTY)
    private IntegrationType integrationType;

    @Column(name = ModelConstants.DEBUG_SETTINGS)
    private String debugSettings;

    @Convert(converter = JsonConverter.class)
    @Column(name = ModelConstants.CONVERTER_CONFIGURATION_PROPERTY)
    private JsonNode configuration;

    @Convert(converter = JsonConverter.class)
    @Column(name = ModelConstants.CONVERTER_ADDITIONAL_INFO_PROPERTY)
    private JsonNode additionalInfo;

    @Column(name = EXTERNAL_ID_PROPERTY)
    private UUID externalId;

    @Column(name = CONVERTER_IS_EDGE_TEMPLATE_MODE_PROPERTY)
    private boolean edgeTemplate;

    @Column(name = CONVERTER_VERSION_PROPERTY)
    private Integer converterVersion;

    public ConverterEntity() {
        super();
    }

    public ConverterEntity(Converter converter) {
        super(converter);
        if (converter.getTenantId() != null) {
            this.tenantId = converter.getTenantId().getId();
        }
        this.name = converter.getName();
        this.type = converter.getType();
        this.integrationType = converter.getIntegrationType();
        this.debugSettings = JacksonUtil.toString(converter.getDebugSettings());
        this.configuration = converter.getConfiguration();
        this.additionalInfo = converter.getAdditionalInfo();
        if (converter.getExternalId() != null) {
            this.externalId = converter.getExternalId().getId();
        }
        this.edgeTemplate = converter.isEdgeTemplate();
        this.converterVersion = converter.getConverterVersion();
    }

    @Override
    public Converter toData() {
        Converter converter = new Converter(new ConverterId(id));
        converter.setCreatedTime(createdTime);
        converter.setVersion(version);
        if (tenantId != null) {
            converter.setTenantId(new TenantId(tenantId));
        }
        converter.setName(name);
        converter.setType(type);
        converter.setIntegrationType(integrationType);
        converter.setDebugSettings(JacksonUtil.fromString(debugSettings, DebugSettings.class));
        converter.setConfiguration(configuration);
        converter.setAdditionalInfo(additionalInfo);
        if (externalId != null) {
            converter.setExternalId(new ConverterId(externalId));
        }
        converter.setEdgeTemplate(edgeTemplate);
        converter.setConverterVersion(converterVersion);
        return converter;
    }

}
