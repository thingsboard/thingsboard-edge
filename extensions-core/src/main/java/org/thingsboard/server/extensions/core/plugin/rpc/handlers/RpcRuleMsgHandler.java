/**
 * Thingsboard OÜ ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2017 Thingsboard OÜ. All Rights Reserved.
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
package org.thingsboard.server.extensions.core.plugin.rpc.handlers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.RuleId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.extensions.api.plugins.PluginCallback;
import org.thingsboard.server.extensions.api.plugins.PluginContext;
import org.thingsboard.server.extensions.api.plugins.handlers.RuleMsgHandler;
import org.thingsboard.server.extensions.api.plugins.msg.RuleToPluginMsg;
import org.thingsboard.server.extensions.api.plugins.msg.ToDeviceRpcRequest;
import org.thingsboard.server.extensions.api.plugins.msg.ToDeviceRpcRequestBody;
import org.thingsboard.server.extensions.api.rules.RuleException;
import org.thingsboard.server.extensions.core.action.rpc.ServerSideRpcCallActionMsg;
import org.thingsboard.server.extensions.core.action.rpc.ServerSideRpcCallRuleToPluginActionMsg;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Created by ashvayka on 14.09.17.
 */
@Slf4j
public class RpcRuleMsgHandler implements RuleMsgHandler {

    @Override
    public void process(PluginContext ctx, TenantId tenantId, RuleId ruleId, RuleToPluginMsg<?> msg) throws RuleException {
        if (msg instanceof ServerSideRpcCallRuleToPluginActionMsg) {
            handle(ctx, tenantId, ruleId, ((ServerSideRpcCallRuleToPluginActionMsg) msg).getPayload());
        } else {
            throw new RuntimeException("Not supported msg: " + msg + "!");
        }
    }

    private void handle(final PluginContext ctx, TenantId tenantId, RuleId ruleId, ServerSideRpcCallActionMsg msg) {
        DeviceId deviceId = new DeviceId(UUID.fromString(msg.getDeviceId()));
        ctx.checkAccess(deviceId, new PluginCallback<Void>() {
            @Override
            public void onSuccess(PluginContext dummy, Void value) {
                try {
                    List<EntityId> deviceIds;
                    if (StringUtils.isEmpty(msg.getFromDeviceRelation()) && StringUtils.isEmpty(msg.getToDeviceRelation())) {
                        deviceIds = Collections.singletonList(deviceId);
                    } else if (!StringUtils.isEmpty(msg.getFromDeviceRelation())) {
                        List<EntityRelation> relations = ctx.findByFromAndType(deviceId, msg.getFromDeviceRelation()).get();
                        deviceIds = relations.stream().map(EntityRelation::getTo).collect(Collectors.toList());
                    } else {
                        List<EntityRelation> relations = ctx.findByToAndType(deviceId, msg.getFromDeviceRelation()).get();
                        deviceIds = relations.stream().map(EntityRelation::getFrom).collect(Collectors.toList());
                    }
                    ToDeviceRpcRequestBody body = new ToDeviceRpcRequestBody(msg.getRpcCallMethod(), msg.getRpcCallBody());
                    long expirationTime = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(msg.getRpcCallTimeoutInSec());
                    for (EntityId address : deviceIds) {
                        DeviceId tmpId = new DeviceId(address.getId());
                        ctx.checkAccess(tmpId, new PluginCallback<Void>() {
                            @Override
                            public void onSuccess(PluginContext ctx, Void value) {
                                ctx.sendRpcRequest(new ToDeviceRpcRequest(UUID.randomUUID(),
                                        null, tenantId, tmpId, true, expirationTime, body)
                                );
                                log.trace("[{}] Sent RPC Call Action msg", tmpId);
                            }

                            @Override
                            public void onFailure(PluginContext ctx, Exception e) {
                                log.info("[{}] Failed to process RPC Call Action msg", tmpId, e);
                            }
                        });
                    }
                } catch (Exception e) {
                    log.info("Failed to process RPC Call Action msg", e);
                }
            }

            @Override
            public void onFailure(PluginContext dummy, Exception e) {
                log.info("[{}] Failed to process RPC Call Action msg", deviceId, e);
            }
        });
    }
}
