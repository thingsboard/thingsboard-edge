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
package org.thingsboard.rule.engine.metadata;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.util.TbPair;
import org.thingsboard.server.common.msg.TbMsg;

import static org.thingsboard.common.util.DonAsynchron.withCallback;

@Slf4j
public abstract class TbAbstractGetEntityDataNode<T extends EntityId> extends TbAbstractGetMappedDataNode<T, TbGetEntityDataNodeConfiguration> {

    private final static String DATA_TO_FETCH_PROPERTY_NAME = "dataToFetch";
    private static final String OLD_DATA_TO_FETCH_PROPERTY_NAME = "telemetry";
    private final static String DATA_MAPPING_PROPERTY_NAME = "dataMapping";
    private static final String OLD_DATA_MAPPING_PROPERTY_NAME = "attrMapping";

    private static final String DATA_TO_FETCH_VALIDATION_MSG = "DataToFetch property has invalid value: %s." +
            " Only ATTRIBUTES and LATEST_TELEMETRY values supported!";

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) {
        var msgDataAsObjectNode = FetchTo.DATA.equals(fetchTo) ? getMsgDataAsObjectNode(msg) : null;
        withCallback(findEntityAsync(ctx, msg.getOriginator()),
                entityId -> processDataAndTell(ctx, msg, entityId, msgDataAsObjectNode),
                t -> ctx.tellFailure(msg, t), ctx.getDbCallbackExecutor());
    }

    protected abstract ListenableFuture<T> findEntityAsync(TbContext ctx, EntityId originator);

    protected void checkDataToFetchSupportedOrElseThrow(DataToFetch dataToFetch) throws TbNodeException {
        if (dataToFetch == null || dataToFetch.equals(DataToFetch.FIELDS)) {
            throw new TbNodeException(String.format(DATA_TO_FETCH_VALIDATION_MSG, dataToFetch));
        }
    }

    protected void processDataAndTell(TbContext ctx, TbMsg msg, T entityId, ObjectNode msgDataAsJsonNode) {
        DataToFetch dataToFetch = config.getDataToFetch();
        switch (dataToFetch) {
            case ATTRIBUTES:
                processAttributesKvEntryData(ctx, msg, entityId, msgDataAsJsonNode);
                break;
            case LATEST_TELEMETRY:
                processTsKvEntryData(ctx, msg, entityId, msgDataAsJsonNode);
                break;
            case FIELDS:
                processFieldsData(ctx, msg, entityId, msgDataAsJsonNode, true);
                break;
        }
    }

    protected TbPair<Boolean, JsonNode> upgradeToUseFetchToAndDataToFetch(JsonNode oldConfiguration) throws TbNodeException {
        var newConfigObjectNode = (ObjectNode) oldConfiguration;
        if (!newConfigObjectNode.has(OLD_DATA_TO_FETCH_PROPERTY_NAME)) {
            throw new TbNodeException("property to update: '" + OLD_DATA_TO_FETCH_PROPERTY_NAME + "' doesn't exists in configuration!");
        }
        if (!newConfigObjectNode.has(OLD_DATA_MAPPING_PROPERTY_NAME)) {
            throw new TbNodeException("property to update: '" + OLD_DATA_MAPPING_PROPERTY_NAME + "' doesn't exists in configuration!");
        }
        newConfigObjectNode.set(DATA_MAPPING_PROPERTY_NAME, newConfigObjectNode.get(OLD_DATA_MAPPING_PROPERTY_NAME));
        newConfigObjectNode.remove(OLD_DATA_MAPPING_PROPERTY_NAME);
        var value = newConfigObjectNode.get(OLD_DATA_TO_FETCH_PROPERTY_NAME).asText();
        if ("true".equals(value)) {
            newConfigObjectNode.remove(OLD_DATA_TO_FETCH_PROPERTY_NAME);
            newConfigObjectNode.put(DATA_TO_FETCH_PROPERTY_NAME, DataToFetch.LATEST_TELEMETRY.name());
        } else if ("false".equals(value)) {
            newConfigObjectNode.remove(OLD_DATA_TO_FETCH_PROPERTY_NAME);
            newConfigObjectNode.put(DATA_TO_FETCH_PROPERTY_NAME, DataToFetch.ATTRIBUTES.name());
        } else {
            throw new TbNodeException("property to update: '" + OLD_DATA_TO_FETCH_PROPERTY_NAME + "' has unexpected value: " + value + ". Allowed values: true or false!");
        }
        newConfigObjectNode.put(FETCH_TO_PROPERTY_NAME, FetchTo.METADATA.name());
        return new TbPair<>(true, newConfigObjectNode);
    }

}
