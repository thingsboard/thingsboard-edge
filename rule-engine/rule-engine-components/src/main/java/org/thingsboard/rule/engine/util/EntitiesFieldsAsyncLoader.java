/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2022 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.rule.engine.util;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.server.common.data.BaseData;
import org.thingsboard.server.common.data.EntityFieldsData;
import org.thingsboard.server.common.data.id.AlarmCommentId;
import org.thingsboard.server.common.data.id.AlarmId;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityViewId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;

import java.util.function.Function;

public class EntitiesFieldsAsyncLoader {

    public static ListenableFuture<EntityFieldsData> findAsync(TbContext ctx, EntityId original) {
        switch (original.getEntityType()) {
            case TENANT:
                return getAsync(ctx.getTenantService().findTenantByIdAsync(ctx.getTenantId(), (TenantId) original),
                        EntityFieldsData::new);
            case CUSTOMER:
                return getAsync(ctx.getCustomerService().findCustomerByIdAsync(ctx.getTenantId(), (CustomerId) original),
                        EntityFieldsData::new);
            case USER:
                return getAsync(ctx.getUserService().findUserByIdAsync(ctx.getTenantId(), (UserId) original),
                        EntityFieldsData::new);
            case ASSET:
                return getAsync(ctx.getAssetService().findAssetByIdAsync(ctx.getTenantId(), (AssetId) original),
                        EntityFieldsData::new);
            case DEVICE:
                return getAsync(ctx.getDeviceService().findDeviceByIdAsync(ctx.getTenantId(), (DeviceId) original),
                        EntityFieldsData::new);
            case ALARM:
                return getAsync(ctx.getAlarmService().findAlarmByIdAsync(ctx.getTenantId(), (AlarmId) original),
                        EntityFieldsData::new);
            case ALARM_COMMENT:
                return getAsync(ctx.getAlarmCommentService().findAlarmCommentByIdAsync(ctx.getTenantId(), (AlarmCommentId) original),
                        EntityFieldsData::new);
            case RULE_CHAIN:
                return getAsync(ctx.getRuleChainService().findRuleChainByIdAsync(ctx.getTenantId(), (RuleChainId) original),
                        EntityFieldsData::new);
            case ENTITY_VIEW:
                return getAsync(ctx.getEntityViewService().findEntityViewByIdAsync(ctx.getTenantId(), (EntityViewId) original),
                        EntityFieldsData::new);
            default:
                return Futures.immediateFailedFuture(new TbNodeException("Unexpected original EntityType " + original.getEntityType()));
        }
    }

    private static <T extends BaseData> ListenableFuture<EntityFieldsData> getAsync(
            ListenableFuture<T> future, Function<T, EntityFieldsData> converter) {
        return Futures.transformAsync(future, in -> in != null ?
                Futures.immediateFuture(converter.apply(in))
                : Futures.immediateFailedFuture(new RuntimeException("Entity not found!")), MoreExecutors.directExecutor());
    }
}
