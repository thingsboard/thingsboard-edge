/**
 * Thingsboard OÜ ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2018 Thingsboard OÜ. All Rights Reserved.
 *
 * NOTICE: All information contained herein is, and remains
 * the property of Thingsboard OÜ and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Thingsboard OÜ
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

import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.server.common.data.HasTenantId;
import org.thingsboard.server.common.data.alarm.AlarmId;
import org.thingsboard.server.common.data.id.*;

public class EntitiesTenantIdAsyncLoader {

    public static ListenableFuture<TenantId> findEntityIdAsync(TbContext ctx, EntityId original) {

        switch (original.getEntityType()) {
            case TENANT:
                return Futures.immediateFuture((TenantId) original);
            case CUSTOMER:
                return getTenantAsync(ctx.getCustomerService().findCustomerByIdAsync((CustomerId) original));
            case USER:
                return getTenantAsync(ctx.getUserService().findUserByIdAsync((UserId) original));
            case ASSET:
                return getTenantAsync(ctx.getAssetService().findAssetByIdAsync((AssetId) original));
            case DEVICE:
                return getTenantAsync(ctx.getDeviceService().findDeviceByIdAsync((DeviceId) original));
            case ALARM:
                return getTenantAsync(ctx.getAlarmService().findAlarmByIdAsync((AlarmId) original));
            case RULE_CHAIN:
                return getTenantAsync(ctx.getRuleChainService().findRuleChainByIdAsync((RuleChainId) original));
            default:
                return Futures.immediateFailedFuture(new TbNodeException("Unexpected original EntityType " + original));
        }
    }

    private static <T extends HasTenantId> ListenableFuture<TenantId> getTenantAsync(ListenableFuture<T> future) {
        return Futures.transformAsync(future, in -> {
            return in != null ? Futures.immediateFuture(in.getTenantId())
                    : Futures.immediateFuture(null);});
    }
}
