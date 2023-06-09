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
package org.thingsboard.server.dao.model.sqlts.latest;

import lombok.Data;
import org.thingsboard.server.dao.model.sql.AbstractTsKvEntity;
import org.thingsboard.server.dao.sqlts.latest.SearchTsKvLatestRepository;

import javax.persistence.ColumnResult;
import javax.persistence.ConstructorResult;
import javax.persistence.Entity;
import javax.persistence.IdClass;
import javax.persistence.NamedNativeQueries;
import javax.persistence.NamedNativeQuery;
import javax.persistence.SqlResultSetMapping;
import javax.persistence.SqlResultSetMappings;
import javax.persistence.Table;
import java.util.UUID;

@Data
@Entity
@Table(name = "ts_kv_latest")
@IdClass(TsKvLatestCompositeKey.class)
@SqlResultSetMappings({
        @SqlResultSetMapping(
                name = "tsKvLatestFindMapping",
                classes = {
                        @ConstructorResult(
                                targetClass = TsKvLatestEntity.class,
                                columns = {
                                        @ColumnResult(name = "entityId", type = UUID.class),
                                        @ColumnResult(name = "key", type = Integer.class),
                                        @ColumnResult(name = "strKey", type = String.class),
                                        @ColumnResult(name = "strValue", type = String.class),
                                        @ColumnResult(name = "boolValue", type = Boolean.class),
                                        @ColumnResult(name = "longValue", type = Long.class),
                                        @ColumnResult(name = "doubleValue", type = Double.class),
                                        @ColumnResult(name = "jsonValue", type = String.class),
                                        @ColumnResult(name = "ts", type = Long.class),

                                }
                        ),
                })
})
@NamedNativeQueries({
        @NamedNativeQuery(
                name = SearchTsKvLatestRepository.FIND_ALL_BY_ENTITY_ID,
                query = SearchTsKvLatestRepository.FIND_ALL_BY_ENTITY_ID_QUERY,
                resultSetMapping = "tsKvLatestFindMapping",
                resultClass = TsKvLatestEntity.class
        )
})
public final class TsKvLatestEntity extends AbstractTsKvEntity {

    @Override
    public boolean isNotEmpty() {
        return strValue != null || longValue != null || doubleValue != null || booleanValue != null || jsonValue != null;
    }

    public TsKvLatestEntity() {
    }

    public TsKvLatestEntity(UUID entityId, Integer key, String strKey, String strValue, Boolean boolValue, Long longValue, Double doubleValue, String jsonValue, Long ts) {
        this.entityId = entityId;
        this.key = key;
        this.ts = ts;
        this.longValue = longValue;
        this.doubleValue = doubleValue;
        this.strValue = strValue;
        this.booleanValue = boolValue;
        this.jsonValue = jsonValue;
        this.strKey = strKey;
    }
}
