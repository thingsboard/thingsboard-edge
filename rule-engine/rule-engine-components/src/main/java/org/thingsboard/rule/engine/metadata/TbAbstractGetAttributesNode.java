/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2019 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.rule.engine.metadata;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.msg.TbMsg;

import java.util.List;

import static org.thingsboard.rule.engine.api.util.DonAsynchron.withCallback;
import static org.thingsboard.rule.engine.api.TbRelationTypes.FAILURE;
import static org.thingsboard.rule.engine.api.TbRelationTypes.SUCCESS;
import static org.thingsboard.server.common.data.DataConstants.CLIENT_SCOPE;
import static org.thingsboard.server.common.data.DataConstants.SERVER_SCOPE;
import static org.thingsboard.server.common.data.DataConstants.SHARED_SCOPE;

public abstract class TbAbstractGetAttributesNode<C extends TbGetAttributesNodeConfiguration, T extends EntityId> implements TbNode {

    protected C config;

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = loadGetAttributesNodeConfig(configuration);
    }

    protected abstract C loadGetAttributesNodeConfig(TbNodeConfiguration configuration) throws TbNodeException;

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) throws TbNodeException {
        try {
            withCallback(
                    findEntityIdAsync(ctx, msg),
                    entityId -> safePutAttributes(ctx, msg, entityId),
                    t -> ctx.tellFailure(msg, t), ctx.getDbCallbackExecutor());
        } catch (Throwable th) {
            ctx.tellFailure(msg, th);
        }
    }

    private void safePutAttributes(TbContext ctx, TbMsg msg, T entityId) {
        if (entityId == null || entityId.isNullUid()) {
            ctx.tellNext(msg, FAILURE);
            return;
        }
        ListenableFuture<List<Void>> allFutures = Futures.allAsList(
                putLatestTelemetry(ctx, entityId, msg, config.getLatestTsKeyNames()),
                putAttrAsync(ctx, entityId, msg, CLIENT_SCOPE, config.getClientAttributeNames(), "cs_"),
                putAttrAsync(ctx, entityId, msg, SHARED_SCOPE, config.getSharedAttributeNames(), "shared_"),
                putAttrAsync(ctx, entityId, msg, SERVER_SCOPE, config.getServerAttributeNames(), "ss_")
        );
        withCallback(allFutures, i -> ctx.tellNext(msg, SUCCESS), t -> ctx.tellFailure(msg, t), ctx.getDbCallbackExecutor());
    }

    private ListenableFuture<Void> putAttrAsync(TbContext ctx, EntityId entityId, TbMsg msg, String scope, List<String> keys, String prefix) {
        if (CollectionUtils.isEmpty(keys)) {
            return Futures.immediateFuture(null);
        }
        ListenableFuture<List<AttributeKvEntry>> latest = ctx.getAttributesService().find(ctx.getTenantId(), entityId, scope, keys);
        return Futures.transform(latest, l -> {
            l.forEach(r -> {
                if (BooleanUtils.toBooleanDefaultIfNull(this.config.isTellFailureIfAbsent(), true)) {
                    if (r.getValue() != null) {
                        msg.getMetaData().putValue(prefix + r.getKey(), r.getValueAsString());
                    } else {
                        throw new RuntimeException("[" + scope + "][" + r.getKey() + "] attribute value is not present in the DB!");
                    }
                } else {
                    if (r.getValue() != null) {
                        msg.getMetaData().putValue(prefix + r.getKey(), r.getValueAsString());
                    }
                }

            });
            return null;
        });
    }

    private ListenableFuture<Void> putLatestTelemetry(TbContext ctx, EntityId entityId, TbMsg msg, List<String> keys) {
        if (CollectionUtils.isEmpty(keys)) {
            return Futures.immediateFuture(null);
        }
        ListenableFuture<List<TsKvEntry>> latest = ctx.getTimeseriesService().findLatest(ctx.getTenantId(), entityId, keys);
        return Futures.transform(latest, l -> {
            l.forEach(r -> {
                if (BooleanUtils.toBooleanDefaultIfNull(this.config.isTellFailureIfAbsent(), true)) {
                    if (r.getValue() != null) {
                        msg.getMetaData().putValue(r.getKey(), r.getValueAsString());
                    } else {
                        throw new RuntimeException("[" + r.getKey() + "] telemetry value is not present in the DB!");
                    }
                } else {
                    if (r.getValue() != null) {
                        msg.getMetaData().putValue(r.getKey(), r.getValueAsString());
                    }
                }
            });
            return null;
        });
    }

    @Override
    public void destroy() {

    }

    protected abstract ListenableFuture<T> findEntityIdAsync(TbContext ctx, TbMsg msg);
}
