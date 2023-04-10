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
package org.thingsboard.rule.engine.edge;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.session.SessionMsgType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
public abstract class AbstractTbMsgPushNode<T extends BaseTbMsgPushNodeConfiguration, S, U> implements TbNode {

    protected T config;

    private static final String SCOPE = "scope";

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, getConfigClazz());
    }

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) {
        if (getIgnoredMessageSource().equalsIgnoreCase(msg.getMetaData().getValue(DataConstants.MSG_SOURCE_KEY))) {
            log.debug("Ignoring msg from the {}, msg [{}]", getIgnoredMessageSource(), msg);
            ctx.ack(msg);
            return;
        }
        if (isSupportedOriginator(msg.getOriginator().getEntityType())) {
            if (isSupportedMsgType(msg.getType())) {
                processMsg(ctx, msg);
            } else {
                String errMsg = String.format("Unsupported msg type %s", msg.getType());
                log.debug(errMsg);
                ctx.tellFailure(msg, new RuntimeException(errMsg));
            }
        } else {
            String errMsg = String.format("Unsupported originator type %s", msg.getOriginator().getEntityType());
            log.debug(errMsg);
            ctx.tellFailure(msg, new RuntimeException(errMsg));
        }
    }

    protected S buildEvent(TbMsg msg, TbContext ctx) {
        String msgType = msg.getType();
        if (DataConstants.ALARM.equals(msgType)) {
            EdgeEventActionType actionType = getAlarmActionType(msg);
            return buildEvent(ctx.getTenantId(), actionType, getUUIDFromMsgData(msg), getAlarmEventType(), null);
        } else {
            EdgeEventActionType actionType = getEdgeEventActionTypeByMsgType(msgType);
            Map<String, Object> entityBody = new HashMap<>();
            Map<String, String> metadata = msg.getMetaData().getData();
            JsonNode dataJson = JacksonUtil.toJsonNode(msg.getData());
            switch (actionType) {
                case ATTRIBUTES_UPDATED:
                case POST_ATTRIBUTES:
                    entityBody.put("kv", dataJson);
                    entityBody.put(SCOPE, getScope(metadata));
                    if (EdgeEventActionType.POST_ATTRIBUTES.equals(actionType)) {
                        entityBody.put("isPostAttributes", true);
                    }
                    break;
                case ATTRIBUTES_DELETED:
                    List<String> keys = JacksonUtil.convertValue(dataJson.get("attributes"), new TypeReference<>() {});
                    entityBody.put("keys", keys);
                    entityBody.put(SCOPE, getScope(metadata));
                    break;
                case TIMESERIES_UPDATED:
                    entityBody.put("data", dataJson);
                    entityBody.put("ts", msg.getMetaDataTs());
                    break;
            }
            return buildEvent(ctx.getTenantId(),
                    actionType,
                    msg.getOriginator().getId(),
                    getEventTypeByEntityType(msg.getOriginator().getEntityType()),
                    JacksonUtil.valueToTree(entityBody));
        }
    }

    private static EdgeEventActionType getAlarmActionType(TbMsg msg) {
        boolean isNewAlarm = Boolean.parseBoolean(msg.getMetaData().getValue(DataConstants.IS_NEW_ALARM));
        boolean isClearedAlarm = Boolean.parseBoolean(msg.getMetaData().getValue(DataConstants.IS_CLEARED_ALARM));
        EdgeEventActionType eventAction;
        if (isNewAlarm) {
            eventAction = EdgeEventActionType.ADDED;
        } else if (isClearedAlarm) {
            eventAction = EdgeEventActionType.ALARM_CLEAR;
        } else {
            eventAction = EdgeEventActionType.UPDATED;
        }
        return eventAction;
    }

    abstract S buildEvent(TenantId tenantId, EdgeEventActionType eventAction, UUID entityId, U eventType, JsonNode entityBody);

    abstract U getEventTypeByEntityType(EntityType entityType);

    abstract U getAlarmEventType();

    abstract String getIgnoredMessageSource();

    abstract protected Class<T> getConfigClazz();

    abstract void processMsg(TbContext ctx, TbMsg msg);

    protected UUID getUUIDFromMsgData(TbMsg msg) {
        JsonNode data = JacksonUtil.toJsonNode(msg.getData()).get("id");
        String id = JacksonUtil.convertValue(data.get("id"), String.class);
        return UUID.fromString(id);
    }

    protected String getScope(Map<String, String> metadata) {
        String scope = metadata.get(SCOPE);
        if (StringUtils.isEmpty(scope)) {
            scope = config.getScope();
        }
        return scope;
    }

    protected EdgeEventActionType getEdgeEventActionTypeByMsgType(String msgType) {
        EdgeEventActionType actionType;
        if (SessionMsgType.POST_TELEMETRY_REQUEST.name().equals(msgType)
                || DataConstants.TIMESERIES_UPDATED.equals(msgType)) {
            actionType = EdgeEventActionType.TIMESERIES_UPDATED;
        } else if (DataConstants.ATTRIBUTES_UPDATED.equals(msgType)) {
            actionType = EdgeEventActionType.ATTRIBUTES_UPDATED;
        } else if (SessionMsgType.POST_ATTRIBUTES_REQUEST.name().equals(msgType)) {
            actionType = EdgeEventActionType.POST_ATTRIBUTES;
        } else if (DataConstants.ATTRIBUTES_DELETED.equals(msgType)) {
            actionType = EdgeEventActionType.ATTRIBUTES_DELETED;
        } else {
            log.warn("Unsupported msg type [{}]", msgType);
            throw new IllegalArgumentException("Unsupported msg type: " + msgType);
        }
        return actionType;
    }

    protected boolean isSupportedMsgType(String msgType) {
        return SessionMsgType.POST_TELEMETRY_REQUEST.name().equals(msgType)
                || SessionMsgType.POST_ATTRIBUTES_REQUEST.name().equals(msgType)
                || DataConstants.ATTRIBUTES_UPDATED.equals(msgType)
                || DataConstants.ATTRIBUTES_DELETED.equals(msgType)
                || DataConstants.TIMESERIES_UPDATED.equals(msgType)
                || DataConstants.ALARM.equals(msgType);
    }

    protected boolean isSupportedOriginator(EntityType entityType) {
        switch (entityType) {
            case DEVICE:
            case ASSET:
            case ENTITY_VIEW:
            case DASHBOARD:
            case TENANT:
            case CUSTOMER:
            case USER:
            case EDGE:
            case ENTITY_GROUP:
                return true;
            default:
                return false;
        }
    }
}
