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
package org.thingsboard.server.dao.sql.cf;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.support.TransactionTemplate;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.cf.CalculatedField;
import org.thingsboard.server.common.data.cf.CalculatedFieldLink;
import org.thingsboard.server.common.data.cf.CalculatedFieldLinkConfiguration;
import org.thingsboard.server.common.data.cf.CalculatedFieldType;
import org.thingsboard.server.common.data.cf.configuration.CalculatedFieldConfiguration;
import org.thingsboard.server.common.data.debug.DebugSettings;
import org.thingsboard.server.common.data.id.CalculatedFieldId;
import org.thingsboard.server.common.data.id.CalculatedFieldLinkId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Repository
@Slf4j
public class DefaultNativeCalculatedFieldRepository implements NativeCalculatedFieldRepository {

    private final String CF_COUNT_QUERY = "SELECT count(id) FROM calculated_field;";
    private final String CF_QUERY = "SELECT * FROM calculated_field ORDER BY created_time ASC LIMIT %s OFFSET %s";

    private final String CFL_COUNT_QUERY = "SELECT count(id) FROM calculated_field_link;";
    private final String CFL_QUERY = "SELECT * FROM calculated_field_link ORDER BY created_time ASC LIMIT %s OFFSET %s";

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;

    @Override
    public PageData<CalculatedField> findCalculatedFields(Pageable pageable) {
        return transactionTemplate.execute(status -> {
            long startTs = System.currentTimeMillis();
            int totalElements = jdbcTemplate.queryForObject(CF_COUNT_QUERY, Collections.emptyMap(), Integer.class);
            log.debug("Count query took {} ms", System.currentTimeMillis() - startTs);
            startTs = System.currentTimeMillis();
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(String.format(CF_QUERY, pageable.getPageSize(), pageable.getOffset()), Collections.emptyMap());
            log.debug("Main query took {} ms", System.currentTimeMillis() - startTs);
            int totalPages = pageable.getPageSize() > 0 ? (int) Math.ceil((float) totalElements / pageable.getPageSize()) : 1;
            boolean hasNext = pageable.getPageSize() > 0 && totalElements > pageable.getOffset() + rows.size();
            var data = rows.stream().map(row -> {

                UUID id = (UUID) row.get("id");
                long createdTime = (long) row.get("created_time");
                UUID tenantId = (UUID) row.get("tenant_id");
                EntityType entityType = EntityType.valueOf((String) row.get("entity_type"));
                UUID entityId = (UUID) row.get("entity_id");
                CalculatedFieldType type = CalculatedFieldType.valueOf((String) row.get("type"));
                String name = (String) row.get("name");
                int configurationVersion = (int) row.get("configuration_version");
                JsonNode configuration = JacksonUtil.toJsonNode((String) row.get("configuration"));
                long version = row.get("version") != null ? (long) row.get("version") : 0;
                String debugSettings = (String) row.get("debug_settings");
                Object externalIdObj = row.get("external_id");

                CalculatedField calculatedField = new CalculatedField();
                calculatedField.setId(new CalculatedFieldId(id));
                calculatedField.setCreatedTime(createdTime);
                calculatedField.setTenantId(TenantId.fromUUID(tenantId));
                calculatedField.setEntityId(EntityIdFactory.getByTypeAndUuid(entityType, entityId));
                calculatedField.setType(type);
                calculatedField.setName(name);
                calculatedField.setConfigurationVersion(configurationVersion);
                calculatedField.setConfiguration(JacksonUtil.treeToValue(configuration, CalculatedFieldConfiguration.class));
                calculatedField.setVersion(version);
                calculatedField.setDebugSettings(JacksonUtil.fromString(debugSettings, DebugSettings.class));
                calculatedField.setExternalId(externalIdObj != null ? new CalculatedFieldId(UUID.fromString((String) externalIdObj)) : null);

                return calculatedField;
            }).collect(Collectors.toList());
            return new PageData<>(data, totalPages, totalElements, hasNext);
        });
    }

    @Override
    public PageData<CalculatedFieldLink> findCalculatedFieldLinks(Pageable pageable) {
        return transactionTemplate.execute(status -> {
            long startTs = System.currentTimeMillis();
            int totalElements = jdbcTemplate.queryForObject(CFL_COUNT_QUERY, Collections.emptyMap(), Integer.class);
            log.debug("Count query took {} ms", System.currentTimeMillis() - startTs);
            startTs = System.currentTimeMillis();
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(String.format(CFL_QUERY, pageable.getPageSize(), pageable.getOffset()), Collections.emptyMap());
            log.debug("Main query took {} ms", System.currentTimeMillis() - startTs);
            int totalPages = pageable.getPageSize() > 0 ? (int) Math.ceil((float) totalElements / pageable.getPageSize()) : 1;
            boolean hasNext = pageable.getPageSize() > 0 && totalElements > pageable.getOffset() + rows.size();
            var data = rows.stream().map(row -> {

                UUID id = (UUID) row.get("id");
                long createdTime = (long) row.get("created_time");
                UUID tenantId = (UUID) row.get("tenant_id");
                EntityType entityType = EntityType.valueOf((String) row.get("entity_type"));
                UUID entityId = (UUID) row.get("entity_id");
                UUID calculatedFieldId = (UUID) row.get("calculated_field_id");
                JsonNode configuration = JacksonUtil.toJsonNode((String) row.get("configuration"));

                CalculatedFieldLink calculatedFieldLink = new CalculatedFieldLink();
                calculatedFieldLink.setId(new CalculatedFieldLinkId(id));
                calculatedFieldLink.setCreatedTime(createdTime);
                calculatedFieldLink.setTenantId(new TenantId(tenantId));
                calculatedFieldLink.setEntityId(EntityIdFactory.getByTypeAndUuid(entityType, entityId));
                calculatedFieldLink.setCalculatedFieldId(new CalculatedFieldId(calculatedFieldId));
                calculatedFieldLink.setConfiguration(JacksonUtil.treeToValue(configuration, CalculatedFieldLinkConfiguration.class));

                return calculatedFieldLink;
            }).collect(Collectors.toList());
            return new PageData<>(data, totalPages, totalElements, hasNext);
        });
    }

}
