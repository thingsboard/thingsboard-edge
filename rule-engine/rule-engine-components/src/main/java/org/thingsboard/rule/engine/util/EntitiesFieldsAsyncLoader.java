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
package org.thingsboard.rule.engine.util;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.server.common.data.BaseData;
import org.thingsboard.server.common.data.EntityFieldsData;
import org.thingsboard.server.common.data.id.AlarmId;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityViewId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UUIDBased;
import org.thingsboard.server.common.data.id.UserId;

import java.util.NoSuchElementException;
import java.util.function.Function;

public class EntitiesFieldsAsyncLoader {

    public static ListenableFuture<EntityFieldsData> findAsync(TbContext ctx, EntityId originatorId) {
        switch (originatorId.getEntityType()) {  // TODO: use EntityServiceRegistry
            case TENANT:
                return toEntityFieldsDataAsync(ctx.getTenantService().findTenantByIdAsync(ctx.getTenantId(), (TenantId) originatorId),
                        EntityFieldsData::new, ctx);
            case CUSTOMER:
                return toEntityFieldsDataAsync(ctx.getCustomerService().findCustomerByIdAsync(ctx.getTenantId(), (CustomerId) originatorId),
                        EntityFieldsData::new, ctx);
            case USER:
                return toEntityFieldsDataAsync(ctx.getUserService().findUserByIdAsync(ctx.getTenantId(), (UserId) originatorId),
                        EntityFieldsData::new, ctx);
            case ASSET:
                return toEntityFieldsDataAsync(ctx.getAssetService().findAssetByIdAsync(ctx.getTenantId(), (AssetId) originatorId),
                        EntityFieldsData::new, ctx);
            case DEVICE:
                return toEntityFieldsDataAsync(Futures.immediateFuture(ctx.getDeviceService().findDeviceById(ctx.getTenantId(), (DeviceId) originatorId)),
                        EntityFieldsData::new, ctx);
            case ALARM:
                return toEntityFieldsDataAsync(ctx.getAlarmService().findAlarmByIdAsync(ctx.getTenantId(), (AlarmId) originatorId),
                        EntityFieldsData::new, ctx);
            case RULE_CHAIN:
                return toEntityFieldsDataAsync(ctx.getRuleChainService().findRuleChainByIdAsync(ctx.getTenantId(), (RuleChainId) originatorId),
                        EntityFieldsData::new, ctx);
            case ENTITY_VIEW:
                return toEntityFieldsDataAsync(ctx.getEntityViewService().findEntityViewByIdAsync(ctx.getTenantId(), (EntityViewId) originatorId),
                        EntityFieldsData::new, ctx);
            default:
                return Futures.immediateFailedFuture(new TbNodeException("Unexpected originator EntityType: " + originatorId.getEntityType()));
        }
    }

    private static <T extends BaseData<? extends UUIDBased>> ListenableFuture<EntityFieldsData> toEntityFieldsDataAsync(
            ListenableFuture<T> future,
            Function<T, EntityFieldsData> converter,
            TbContext ctx
    ) {
        return Futures.transformAsync(future, in -> in != null ?
                Futures.immediateFuture(converter.apply(in))
                : Futures.immediateFailedFuture(new NoSuchElementException("Entity not found!")), ctx.getDbCallbackExecutor());
    }

}
