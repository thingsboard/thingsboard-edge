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
package org.thingsboard.server.extensions.core.plugin.messaging;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.RuleId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.core.ToServerRpcRequestMsg;
import org.thingsboard.server.common.msg.core.ToServerRpcResponseMsg;
import org.thingsboard.server.extensions.api.plugins.PluginCallback;
import org.thingsboard.server.extensions.api.plugins.PluginContext;
import org.thingsboard.server.extensions.api.plugins.handlers.RuleMsgHandler;
import org.thingsboard.server.extensions.api.plugins.msg.*;
import org.thingsboard.server.extensions.api.rules.RuleException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * @author Andrew Shvayka
 */
@Slf4j
public class DeviceMessagingRuleMsgHandler implements RuleMsgHandler {

    private static final Gson GSON = new Gson();

    private static final String GET_DEVICE_LIST_METHOD_NAME = "getDevices";
    private static final String SEND_MSG_METHOD_NAME = "sendMsg";
    private static final String ON_MSG_METHOD_NAME = "onMsg";
    private static final String ONEWAY = "oneway";
    private static final String TIMEOUT = "timeout";
    private static final String DEVICE_ID = "deviceId";

    private Map<UUID, PendingRpcRequestMetadata> pendingMsgs = new HashMap<>();

    @Setter
    private DeviceMessagingPluginConfiguration configuration;

    @Override
    public void process(PluginContext ctx, TenantId tenantId, RuleId ruleId, RuleToPluginMsg<?> msg) throws RuleException {
        if (msg.getPayload() instanceof ToServerRpcRequestMsg) {
            ToServerRpcRequestMsg request = (ToServerRpcRequestMsg) msg.getPayload();
            try {
                PendingRpcRequestMetadata md = new PendingRpcRequestMetadata(msg.getUid(),
                        request.getRequestId(), tenantId, ruleId, msg.getCustomerId(), msg.getDeviceId());
                switch (request.getMethod()) {
                    case GET_DEVICE_LIST_METHOD_NAME:
                        processGetDeviceList(ctx, md);
                    case SEND_MSG_METHOD_NAME:
                        processSendMsg(ctx, md, request);
                        break;
                    default:
                        throw new RuleException("Method " + request.getMethod() + " not supported!");
                }
            } catch (RuleException e) {
                throw e;
            } catch (Exception e) {
                throw new RuleException(e.getMessage(), e);
            }
        }
    }

    public void process(PluginContext ctx, FromDeviceRpcResponse msg) {
        UUID requestId = msg.getId();
        PendingRpcRequestMetadata pendindMsg = pendingMsgs.remove(requestId);
        if (pendindMsg != null) {
            log.trace("[{}] Received response: {}", requestId, msg);
            ToServerRpcResponseMsg response;
            if (msg.getError().isPresent()) {
                response = new ToServerRpcResponseMsg(pendindMsg.getRequestId(), toJsonString(msg.getError().get()));
            } else {
                response = new ToServerRpcResponseMsg(pendindMsg.getRequestId(), msg.getResponse().orElse(""));
            }
            ctx.reply(new RpcResponsePluginToRuleMsg(
                    pendindMsg.getUid(), pendindMsg.getTenantId(), pendindMsg.getRuleId(), response));
        } else {
            log.trace("[{}] Received stale response: {}", requestId, msg);
        }
    }

    private void processGetDeviceList(PluginContext ctx, PendingRpcRequestMetadata requestMd) {
        CustomerId customerId = requestMd.getCustomerId();
        if (!customerId.isNullUid()) {
            ctx.getCustomerDevices(requestMd.getTenantId(), customerId, configuration.getMaxDeviceCountPerCustomer(), new PluginCallback<List<Device>>() {
                @Override
                public void onSuccess(PluginContext ctx, List<Device> devices) {
                    JsonArray deviceList = new JsonArray();
                    devices.stream().filter(device -> !requestMd.getDeviceId().equals(device.getId())).forEach(device -> {
                        JsonObject deviceJson = new JsonObject();
                        deviceJson.addProperty("id", device.getId().toString());
                        deviceJson.addProperty("name", device.getName());
                        deviceList.add(deviceJson);
                    });
                    ToServerRpcResponseMsg response = new ToServerRpcResponseMsg(requestMd.getRequestId(), GSON.toJson(deviceList));
                    ctx.reply(new RpcResponsePluginToRuleMsg(
                            requestMd.getUid(), requestMd.getTenantId(), requestMd.getRuleId(), response));
                }

                @Override
                public void onFailure(PluginContext ctx, Exception e) {
                    replyWithError(ctx, requestMd, RpcError.INTERNAL);
                }
            });
        } else {
            replyWithError(ctx, requestMd, "Device is unassigned!");
        }
    }

    private void processSendMsg(PluginContext ctx, PendingRpcRequestMetadata requestMd, ToServerRpcRequestMsg request) {
        JsonObject params = new JsonParser().parse(request.getParams()).getAsJsonObject();
        String targetDeviceIdStr = params.get(DEVICE_ID).getAsString();
        DeviceId targetDeviceId = DeviceId.fromString(targetDeviceIdStr);
        boolean oneWay = isOneWay(params);
        long timeout = getTimeout(params);
        if (timeout <= 0) {
            replyWithError(ctx, requestMd, "Timeout can't be negative!");
        } else if (timeout > configuration.getMaxTimeout()) {
            replyWithError(ctx, requestMd, "Timeout is too large!");
        } else {
            ctx.getDevice(targetDeviceId, new PluginCallback<Device>() {
                @Override
                public void onSuccess(PluginContext ctx, Device targetDevice) {
                    UUID uid = UUID.randomUUID();
                    if (targetDevice == null) {
                        replyWithError(ctx, requestMd, RpcError.NOT_FOUND);
                    } else if (!requestMd.getCustomerId().isNullUid() &&
                            requestMd.getTenantId().equals(targetDevice.getTenantId())
                            && requestMd.getCustomerId().equals(targetDevice.getCustomerId())) {
                        pendingMsgs.put(uid, requestMd);
                        log.trace("[{}] Forwarding {} to [{}]", uid, params, targetDeviceId);
                        ToDeviceRpcRequestBody requestBody = new ToDeviceRpcRequestBody(ON_MSG_METHOD_NAME, GSON.toJson(params.get("body")));
                        ctx.sendRpcRequest(new ToDeviceRpcRequest(uid, targetDevice.getTenantId(), targetDeviceId, oneWay, System.currentTimeMillis() + timeout, requestBody));
                    } else {
                        replyWithError(ctx, requestMd, RpcError.FORBIDDEN);
                    }
                }

                @Override
                public void onFailure(PluginContext ctx, Exception e) {
                    replyWithError(ctx, requestMd, RpcError.INTERNAL);
                }
            });
        }
    }

    private boolean isOneWay(JsonObject params) {
        boolean oneWay = false;
        if (params.has(ONEWAY)) {
            oneWay = params.get(ONEWAY).getAsBoolean();
        }
        return oneWay;
    }

    private long getTimeout(JsonObject params) {
        long timeout;
        if (params.has(TIMEOUT)) {
            timeout = params.get(TIMEOUT).getAsLong();
        } else {
            timeout = configuration.getDefaultTimeout();
        }
        return timeout;
    }

    private void replyWithError(PluginContext ctx, PendingRpcRequestMetadata requestMd, RpcError error) {
        replyWithErrorJson(ctx, requestMd, toJsonString(error));
    }

    private void replyWithError(PluginContext ctx, PendingRpcRequestMetadata requestMd, String error) {
        replyWithErrorJson(ctx, requestMd, toJsonString(error));
    }

    private void replyWithErrorJson(PluginContext ctx, PendingRpcRequestMetadata requestMd, String error) {
        ToServerRpcResponseMsg response = new ToServerRpcResponseMsg(requestMd.getRequestId(), error);
        ctx.reply(new RpcResponsePluginToRuleMsg(
                requestMd.getUid(), requestMd.getTenantId(), requestMd.getRuleId(), response));
    }

    private String toJsonString(String error) {
        JsonObject errorObj = new JsonObject();
        errorObj.addProperty("error", error);
        return GSON.toJson(errorObj);
    }

    private String toJsonString(RpcError error) {
        JsonObject errorObj = new JsonObject();
        switch (error) {
            case NOT_FOUND:
                errorObj.addProperty("error", "Target device not found!");
                break;
            case NO_ACTIVE_CONNECTION:
                errorObj.addProperty("error", "No active connection to remote device!");
                break;
            case TIMEOUT:
                errorObj.addProperty("error", "Timeout while waiting response from device!");
                break;
            case FORBIDDEN:
                errorObj.addProperty("error", "This action is not allowed! Devices are unassigned or assigned to different customers!");
                break;
            case INTERNAL:
                errorObj.addProperty("error", "Internal server error!");
                break;
        }
        return GSON.toJson(errorObj);
    }
}
