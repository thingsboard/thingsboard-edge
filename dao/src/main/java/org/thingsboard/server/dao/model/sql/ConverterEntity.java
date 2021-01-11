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
import org.thingsboard.server.common.data.converter.Converter;
import org.thingsboard.server.common.data.converter.ConverterType;
import org.thingsboard.server.common.data.id.ConverterId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.model.BaseSqlEntity;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.model.SearchTextEntity;
import org.thingsboard.server.dao.util.mapping.JsonStringType;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Table;

import java.util.UUID;

import static org.thingsboard.server.dao.model.ModelConstants.CONVERTER_COLUMN_FAMILY_NAME;
import static org.thingsboard.server.dao.model.ModelConstants.CONVERTER_DEBUG_MODE_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.CONVERTER_NAME_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.CONVERTER_TENANT_ID_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.CONVERTER_TYPE_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.SEARCH_TEXT_PROPERTY;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@TypeDef(name = "json", typeClass = JsonStringType.class)
@Table(name = CONVERTER_COLUMN_FAMILY_NAME)
public final class ConverterEntity extends BaseSqlEntity<Converter> implements SearchTextEntity<Converter> {

    @Column(name = CONVERTER_TENANT_ID_PROPERTY)
    private UUID tenantId;

    @Column(name = CONVERTER_NAME_PROPERTY)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = CONVERTER_TYPE_PROPERTY)
    private ConverterType type;

    @Column(name = CONVERTER_DEBUG_MODE_PROPERTY)
    private boolean debugMode;

    @Column(name = SEARCH_TEXT_PROPERTY)
    private String searchText;

    @Type(type = "json")
    @Column(name = ModelConstants.CONVERTER_CONFIGURATION_PROPERTY)
    private JsonNode configuration;

    @Type(type = "json")
    @Column(name = ModelConstants.CONVERTER_ADDITIONAL_INFO_PROPERTY)
    private JsonNode additionalInfo;

    public ConverterEntity() {
        super();
    }

    public ConverterEntity(Converter converter) {
        this.createdTime = converter.getCreatedTime();
        if (converter.getId() != null) {
            this.setUuid(converter.getId().getId());
        }
        if (converter.getTenantId() != null) {
            this.tenantId = converter.getTenantId().getId();
        }
        this.name = converter.getName();
        this.type = converter.getType();
        this.debugMode = converter.isDebugMode();
        this.configuration = converter.getConfiguration();
        this.additionalInfo = converter.getAdditionalInfo();
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
    public Converter toData() {
        Converter converter = new Converter(new ConverterId(id));
        converter.setCreatedTime(createdTime);
        if (tenantId != null) {
            converter.setTenantId(new TenantId(tenantId));
        }
        converter.setName(name);
        converter.setType(type);
        converter.setDebugMode(debugMode);
        converter.setConfiguration(configuration);
        converter.setAdditionalInfo(additionalInfo);
        return converter;
    }
}
