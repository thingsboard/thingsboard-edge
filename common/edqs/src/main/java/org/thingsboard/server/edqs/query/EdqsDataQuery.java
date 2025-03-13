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
package org.thingsboard.server.edqs.query;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.query.EntityDataSortOrder;
import org.thingsboard.server.common.data.query.EntityFilter;
import org.thingsboard.server.common.data.query.EntityKeyType;
import org.thingsboard.server.common.data.util.CollectionsUtil;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Getter
public class EdqsDataQuery extends EdqsQuery {

    private final int pageSize;
    private final int page;
    private final boolean hasTextSearch;
    private final String textSearch;
    private final boolean defaultSort;
    private final DataKey sortKey;
    private final EntityDataSortOrder.Direction sortDirection;
    private final List<DataKey> entityFields;
    private final List<DataKey> latestValues;

    @Builder
    public EdqsDataQuery(EntityFilter entityFilter, List<EdqsFilter> keyFilters,
                         int pageSize, int page, String textSearch, DataKey sortKey, EntityDataSortOrder.Direction sortDirection,
                         List<DataKey> entityFields, List<DataKey> latestValues) {
        super(entityFilter, CollectionsUtil.isNotEmpty(keyFilters), keyFilters);
        this.pageSize = pageSize;
        this.page = page;
        this.hasTextSearch = StringUtils.isNotBlank(textSearch);
        this.textSearch = textSearch;
        this.defaultSort = EntityKeyType.ENTITY_FIELD.equals(sortKey.type()) && "createdTime".equals(sortKey.key()) && EntityDataSortOrder.Direction.DESC.equals(sortDirection);
        this.sortKey = sortKey;
        this.sortDirection = sortDirection;
        this.entityFields = entityFields;
        this.latestValues = latestValues;
    }

}
