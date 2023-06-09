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
package org.thingsboard.server.dao.sql.query;

import org.apache.commons.lang3.math.NumberUtils;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.query.EntityData;
import org.thingsboard.server.common.data.query.EntityDataPageLink;
import org.thingsboard.server.common.data.query.EntityKey;
import org.thingsboard.server.common.data.query.EntityKeyType;
import org.thingsboard.server.common.data.query.TsValue;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class EntityDataAdapter {

    public static PageData<EntityData> createEntityData(EntityDataPageLink pageLink,
                                                        List<EntityKeyMapping> selectionMapping,
                                                        List<Map<String, Object>> rows,
                                                        int totalElements) {
        int totalPages = pageLink.getPageSize() > 0 ? (int) Math.ceil((float) totalElements / pageLink.getPageSize()) : 1;
        int startIndex = pageLink.getPageSize() * pageLink.getPage();
        boolean hasNext = pageLink.getPageSize() > 0 && totalElements > startIndex + rows.size();
        List<EntityData> entitiesData = convertListToEntityData(rows, selectionMapping);
        return new PageData<>(entitiesData, totalPages, totalElements, hasNext);
    }

    private static List<EntityData> convertListToEntityData(List<Map<String, Object>> result, List<EntityKeyMapping> selectionMapping) {
        return result.stream().map(row -> toEntityData(row, selectionMapping)).collect(Collectors.toList());
    }

    private static EntityData toEntityData(Map<String, Object> row, List<EntityKeyMapping> selectionMapping) {
        UUID id = (UUID) row.get("id");
        EntityType entityType = EntityType.valueOf((String) row.get("entity_type"));
        EntityId entityId = EntityIdFactory.getByTypeAndUuid(entityType, id);
        Map<EntityKeyType, Map<String, TsValue>> latest = new HashMap<>();
        //Maybe avoid empty hashmaps?
        EntityData entityData = new EntityData(entityId,
                ((int) row.getOrDefault(DefaultEntityQueryRepository.ATTR_READ_FLAG, 1)) > 0,
                ((int) row.getOrDefault(DefaultEntityQueryRepository.TS_READ_FLAG, 1)) > 0,
                latest, new HashMap<>(), new HashMap<>());
        for (EntityKeyMapping mapping : selectionMapping) {
            if (!mapping.isIgnore()) {
                EntityKey entityKey = mapping.getEntityKey();
                Object value = row.get(mapping.getValueAlias());
                String strValue;
                long ts;
                if (entityKey.getType().equals(EntityKeyType.ENTITY_FIELD)) {
                    strValue = value != null ? value.toString() : "";
                    ts = System.currentTimeMillis();
                } else {
                    strValue = convertValue(value);
                    Object tsObject = row.get(mapping.getTsAlias());
                    ts = tsObject != null ? Long.parseLong(tsObject.toString()) : 0;
                }
                TsValue tsValue = new TsValue(ts, strValue);
                latest.computeIfAbsent(entityKey.getType(), entityKeyType -> new HashMap<>()).put(entityKey.getKey(), tsValue);
            }
        }
        return entityData;
    }

    static String convertValue(Object value) {
        if (value != null) {
            String strVal = value.toString();
            // check number
            if (NumberUtils.isParsable(strVal)) {
                if (strVal.startsWith("0") && !strVal.startsWith("0.")) {
                    return strVal;
                }
                try {
                    BigInteger longVal = new BigInteger(strVal);
                    return longVal.toString();
                } catch (NumberFormatException ignored) {
                }
                try {
                    double dblVal = Double.parseDouble(strVal);
                    String doubleAsString = Double.toString(dblVal);
                    if (!Double.isInfinite(dblVal) && isSimpleDouble(doubleAsString)) {
                        return doubleAsString;
                    }
                } catch (NumberFormatException ignored) {
                }
            }
            return strVal;
        } else {
            return "";
        }
    }

    private static boolean isSimpleDouble(String valueAsString) {
        return valueAsString.contains(".") && !valueAsString.contains("E") && !valueAsString.contains("e");
    }


}
