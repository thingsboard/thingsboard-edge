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
package org.thingsboard.server.edqs.query.processor;

import org.thingsboard.server.common.data.permission.QueryContext;
import org.thingsboard.server.edqs.query.EdqsQuery;
import org.thingsboard.server.edqs.repo.TenantRepo;

public class EntityQueryProcessorFactory {

    public static EntityQueryProcessor create(TenantRepo repo, QueryContext ctx, EdqsQuery query) {
        return switch (query.getEntityFilter().getType()) {
            case SINGLE_ENTITY -> new SingleEntityQueryProcessor(repo, ctx, query);
            case ENTITY_LIST -> new EntityListQueryProcessor(repo, ctx, query);
            case ENTITY_NAME -> new EntityNameQueryProcessor(repo, ctx, query);
            case ENTITY_TYPE -> new EntityTypeQueryProcessor(repo, ctx, query);
            case DEVICE_TYPE -> new DeviceTypeQueryProcessor(repo, ctx, query);
            case ASSET_TYPE -> new AssetTypeQueryProcessor(repo, ctx, query);
            case ENTITY_VIEW_TYPE -> new EntityViewTypeQueryProcessor(repo, ctx, query);
            case EDGE_TYPE -> new EdgeTypeQueryProcessor(repo, ctx, query);
            case RELATIONS_QUERY -> new RelationQueryProcessor(repo, ctx, query);
            case ENTITY_GROUP -> new EntitiesByGroupQueryProcessor(repo, ctx, query);
            case ENTITY_GROUP_LIST -> new EntityGroupListQueryProcessor(repo, ctx, query);
            case ENTITY_GROUP_NAME -> new EntityGroupNameQueryProcessor(repo, ctx, query);
            case ENTITIES_BY_GROUP_NAME -> new EntitiesByGroupNameQueryProcessor(repo, ctx, query);
            case STATE_ENTITY_OWNER -> new StateEntityOwnerQueryProcessor(repo, ctx, query);
            case API_USAGE_STATE -> new ApiUsageStateQueryProcessor(repo, ctx, query);
            case ASSET_SEARCH_QUERY -> new AssetSearchQueryProcessor(repo, ctx, query);
            case DEVICE_SEARCH_QUERY -> new DeviceSearchQueryProcessor(repo, ctx, query);
            case ENTITY_VIEW_SEARCH_QUERY -> new EntityViewSearchQueryProcessor(repo, ctx, query);
            case EDGE_SEARCH_QUERY -> new EdgeTypeSearchQueryProcessor(repo, ctx, query);
            case SCHEDULER_EVENT -> new SchedulerEventQueryProcessor(repo, ctx, query);
            default -> throw new RuntimeException("Not Implemented!");
        };
    }

}
