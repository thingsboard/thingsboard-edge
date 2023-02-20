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
package org.thingsboard.rule.engine.rpc;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.rule.engine.api.RuleEngineDeviceRpcRequest;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.TbRelationTypes;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.msg.TbMsg;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@RuleNode(
        type = ComponentType.ACTION,
        name = "rpc call request",
        configClazz = TbSendRpcRequestNodeConfiguration.class,
        nodeDescription = "Sends RPC call to device",
        nodeDetails = "Expects messages with \"method\" and \"params\". Will forward response from device to next nodes." +
                "If the RPC call request is originated by REST API call from user, will forward the response to user immediately.",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbActionNodeRpcRequestConfig",
        icon = "call_made"
)
public class TbSendRPCRequestNode implements TbNode {

    private Random random = new Random();
    private Gson gson = new Gson();
    private JsonParser jsonParser = new JsonParser();
    private TbSendRpcRequestNodeConfiguration config;

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, TbSendRpcRequestNodeConfiguration.class);
    }

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) {
        JsonObject json = jsonParser.parse(msg.getData()).getAsJsonObject();
        String tmp;
        if (msg.getOriginator().getEntityType() != EntityType.DEVICE) {
            ctx.tellFailure(msg, new RuntimeException("Message originator is not a device entity!"));
        } else if (!json.has("method")) {
            ctx.tellFailure(msg, new RuntimeException("Method is not present in the message!"));
        } else if (!json.has("params")) {
            ctx.tellFailure(msg, new RuntimeException("Params are not present in the message!"));
        } else {
            int requestId = json.has("requestId") ? json.get("requestId").getAsInt() : random.nextInt();
            boolean restApiCall = msg.getType().equals(DataConstants.RPC_CALL_FROM_SERVER_TO_DEVICE);

            tmp = msg.getMetaData().getValue("oneway");
            boolean oneway = !StringUtils.isEmpty(tmp) && Boolean.parseBoolean(tmp);

            tmp = msg.getMetaData().getValue(DataConstants.PERSISTENT);
            boolean persisted = !StringUtils.isEmpty(tmp) && Boolean.parseBoolean(tmp);

            tmp = msg.getMetaData().getValue("requestUUID");
            UUID requestUUID = !StringUtils.isEmpty(tmp) ? UUID.fromString(tmp) : Uuids.timeBased();
            tmp = msg.getMetaData().getValue("originServiceId");
            String originServiceId = !StringUtils.isEmpty(tmp) ? tmp : null;

            tmp = msg.getMetaData().getValue(DataConstants.EXPIRATION_TIME);
            long expirationTime = !StringUtils.isEmpty(tmp) ? Long.parseLong(tmp) : (System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(config.getTimeoutInSeconds()));

            tmp = msg.getMetaData().getValue(DataConstants.RETRIES);
            Integer retries = !StringUtils.isEmpty(tmp) ? Integer.parseInt(tmp) : null;

            String params = parseJsonData(json.get("params"));
            String additionalInfo = parseJsonData(json.get(DataConstants.ADDITIONAL_INFO));

            RuleEngineDeviceRpcRequest request = RuleEngineDeviceRpcRequest.builder()
                    .oneway(oneway)
                    .method(json.get("method").getAsString())
                    .body(params)
                    .tenantId(ctx.getTenantId())
                    .deviceId(new DeviceId(msg.getOriginator().getId()))
                    .requestId(requestId)
                    .requestUUID(requestUUID)
                    .originServiceId(originServiceId)
                    .expirationTime(expirationTime)
                    .retries(retries)
                    .restApiCall(restApiCall)
                    .persisted(persisted)
                    .additionalInfo(additionalInfo)
                    .build();

            ctx.getRpcService().sendRpcRequestToDevice(request, ruleEngineDeviceRpcResponse -> {
                if (ruleEngineDeviceRpcResponse.getError().isEmpty()) {
                    TbMsg next = ctx.newMsg(msg.getQueueName(), msg.getType(), msg.getOriginator(), msg.getCustomerId(), msg.getMetaData(), ruleEngineDeviceRpcResponse.getResponse().orElse("{}"));
                    ctx.enqueueForTellNext(next, TbRelationTypes.SUCCESS);
                } else {
                    TbMsg next = ctx.newMsg(msg.getQueueName(), msg.getType(), msg.getOriginator(), msg.getCustomerId(), msg.getMetaData(), wrap("error", ruleEngineDeviceRpcResponse.getError().get().name()));
                    ctx.enqueueForTellFailure(next, ruleEngineDeviceRpcResponse.getError().get().name());
                }
            });
            ctx.ack(msg);
        }
    }

    private String wrap(String name, String body) {
        JsonObject json = new JsonObject();
        json.addProperty(name, body);
        return gson.toJson(json);
    }

    private String parseJsonData(JsonElement paramsEl) {
        if (paramsEl != null) {
            return paramsEl.isJsonPrimitive() ? paramsEl.getAsString() : gson.toJson(paramsEl);
        } else {
            return null;
        }
    }

}
