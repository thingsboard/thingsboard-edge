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
package org.thingsboard.rule.engine.action;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.thingsboard.server.common.data.util.CollectionsUtil;
import org.thingsboard.common.util.DonAsynchron;
import org.thingsboard.rule.engine.api.EmptyNodeConfiguration;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.objects.AttributesEntityView;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.session.SessionMsgType;
import org.thingsboard.server.common.adaptor.JsonConverter;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.thingsboard.rule.engine.api.TbRelationTypes.SUCCESS;

@Slf4j
@RuleNode(
        type = ComponentType.ACTION,
        name = "copy to view",
        configClazz = EmptyNodeConfiguration.class,
        nodeDescription = "Copy attributes from asset/device to entity view and changes message originator to related entity view",
        nodeDetails = "Copy attributes from asset/device to related entity view according to entity view configuration. \n " +
                "Copy will be done only for attributes that are between start and end dates and according to attribute keys configuration. \n" +
                "Changes message originator to related entity view and produces new messages according to count of updated entity views",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbNodeEmptyConfig",
        icon = "content_copy"
)
public class TbCopyAttributesToEntityViewNode implements TbNode {

    EmptyNodeConfiguration config;

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, EmptyNodeConfiguration.class);
    }

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) {
        if (DataConstants.ATTRIBUTES_UPDATED.equals(msg.getType()) ||
                DataConstants.ATTRIBUTES_DELETED.equals(msg.getType()) ||
                DataConstants.ACTIVITY_EVENT.equals(msg.getType()) ||
                DataConstants.INACTIVITY_EVENT.equals(msg.getType()) ||
                SessionMsgType.POST_ATTRIBUTES_REQUEST.name().equals(msg.getType())) {
            if (!msg.getMetaData().getData().isEmpty()) {
                long now = System.currentTimeMillis();
                String scope = msg.getType().equals(SessionMsgType.POST_ATTRIBUTES_REQUEST.name()) ?
                        DataConstants.CLIENT_SCOPE : msg.getMetaData().getValue(DataConstants.SCOPE);

                ListenableFuture<List<EntityView>> entityViewsFuture =
                        ctx.getEntityViewService().findEntityViewsByTenantIdAndEntityIdAsync(ctx.getTenantId(), msg.getOriginator());

                DonAsynchron.withCallback(entityViewsFuture,
                        entityViews -> {
                            for (EntityView entityView : entityViews) {
                                long startTime = entityView.getStartTimeMs();
                                long endTime = entityView.getEndTimeMs();
                                if ((endTime != 0 && endTime > now && startTime < now) || (endTime == 0 && startTime < now)) {
                                    if (DataConstants.ATTRIBUTES_DELETED.equals(msg.getType())) {
                                        List<String> attributes = new ArrayList<>();
                                        for (JsonElement element : new JsonParser().parse(msg.getData()).getAsJsonObject().get("attributes").getAsJsonArray()) {
                                            if (element.isJsonPrimitive()) {
                                                JsonPrimitive value = element.getAsJsonPrimitive();
                                                if (value.isString()) {
                                                    attributes.add(value.getAsString());
                                                }
                                            }
                                        }
                                        List<String> filteredAttributes =
                                                attributes.stream().filter(attr -> attributeContainsInEntityView(scope, attr, entityView)).collect(Collectors.toList());
                                        if (!filteredAttributes.isEmpty()) {
                                            ctx.getTelemetryService().deleteAndNotify(ctx.getTenantId(), entityView.getId(), scope, filteredAttributes,
                                                    getFutureCallback(ctx, msg, entityView));
                                        }
                                    } else {
                                        Set<AttributeKvEntry> attributes = JsonConverter.convertToAttributes(new JsonParser().parse(msg.getData()));
                                        List<AttributeKvEntry> filteredAttributes =
                                                attributes.stream().filter(attr -> attributeContainsInEntityView(scope, attr.getKey(), entityView)).collect(Collectors.toList());
                                        ctx.getTelemetryService().saveAndNotify(ctx.getTenantId(), entityView.getId(), scope, filteredAttributes,
                                                getFutureCallback(ctx, msg, entityView));
                                    }
                                }
                            }
                            ctx.ack(msg);
                        },
                        t -> ctx.tellFailure(msg, t));
            } else {
                ctx.tellFailure(msg, new IllegalArgumentException("Message metadata is empty"));
            }
        } else {
            ctx.tellFailure(msg, new IllegalArgumentException("Unsupported msg type [" + msg.getType() + "]"));
        }
    }

    @NotNull
    private FutureCallback<Void> getFutureCallback(TbContext ctx, TbMsg msg, EntityView entityView) {
        return new FutureCallback<Void>() {
            @Override
            public void onSuccess(@Nullable Void result) {
                transformAndTellNext(ctx, msg, entityView);
            }

            @Override
            public void onFailure(Throwable t) {
                ctx.tellFailure(msg, t);
            }
        };
    }

    private void transformAndTellNext(TbContext ctx, TbMsg msg, EntityView entityView) {
        ctx.enqueueForTellNext(ctx.newMsg(msg.getQueueName(), msg.getType(), entityView.getId(), msg.getCustomerId(), msg.getMetaData(), msg.getData()), SUCCESS);
    }

    private boolean attributeContainsInEntityView(String scope, String attrKey, EntityView entityView) {
        AttributesEntityView attributesEntityView = entityView.getKeys().getAttributes();
        List<String> keys = null;
        switch (scope) {
            case DataConstants.CLIENT_SCOPE:
                keys = attributesEntityView.getCs();
                break;
            case DataConstants.SERVER_SCOPE:
                keys = attributesEntityView.getSs();
                break;
            case DataConstants.SHARED_SCOPE:
                keys = attributesEntityView.getSh();
                break;
        }
        return CollectionsUtil.contains(keys, attrKey);
    }

}
