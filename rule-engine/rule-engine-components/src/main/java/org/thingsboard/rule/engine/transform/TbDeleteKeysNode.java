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
package org.thingsboard.rule.engine.transform;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.rule.engine.util.TbMsgSource;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@RuleNode(
        type = ComponentType.TRANSFORMATION,
        name = "delete key-values",
        version = 1,
        configClazz = TbDeleteKeysNodeConfiguration.class,
        nodeDescription = "Removes key-values from message or message metadata.",
        nodeDetails = "Removes key-values from message or message metadata based on the keys list specified in the configuration. " +
                "Use regular expression(s) as a key(s) to remove keys by pattern.<br><br>" +
                "Output connections: <code>Success</code>, <code>Failure</code>.",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbTransformationNodeDeleteKeysConfig",
        icon = "remove_circle"
)
public class TbDeleteKeysNode extends TbAbstractTransformNodeWithTbMsgSource {

    private TbDeleteKeysNodeConfiguration config;
    private List<Pattern> patternKeys;
    private TbMsgSource deleteFrom;

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, TbDeleteKeysNodeConfiguration.class);
        this.deleteFrom = config.getDeleteFrom();
        if (deleteFrom == null) {
            throw new TbNodeException("DeleteFrom can't be null! Allowed values: " + Arrays.toString(TbMsgSource.values()));
        }
        this.patternKeys = config.getKeys().stream().map(Pattern::compile).collect(Collectors.toList());
    }

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) throws ExecutionException, InterruptedException, TbNodeException {
        TbMsgMetaData metaDataCopy = msg.getMetaData().copy();
        String msgData = msg.getData();
        List<String> keysToDelete = new ArrayList<>();
        switch (deleteFrom) {
            case METADATA:
                Map<String, String> metaDataMap = metaDataCopy.getData();
                metaDataMap.forEach((keyMetaData, valueMetaData) -> {
                    if (matches(keyMetaData)) {
                        keysToDelete.add(keyMetaData);
                    }
                });
                keysToDelete.forEach(metaDataMap::remove);
                metaDataCopy = new TbMsgMetaData(metaDataMap);
                break;
            case DATA:
                JsonNode dataNode = JacksonUtil.toJsonNode(msgData);
                if (dataNode.isObject()) {
                    ObjectNode msgDataObject = (ObjectNode) dataNode;
                    dataNode.fields().forEachRemaining(entry -> {
                        String keyData = entry.getKey();
                        if (matches(keyData)) {
                            keysToDelete.add(keyData);
                        }
                    });
                    msgDataObject.remove(keysToDelete);
                    msgData = JacksonUtil.toString(msgDataObject);
                }
                break;
            default:
                log.debug("Unexpected DeleteFrom value: {}. Allowed values: {}", deleteFrom, TbMsgSource.values());
                break;
        }
        ctx.tellSuccess(keysToDelete.isEmpty() ? msg : TbMsg.transformMsg(msg, metaDataCopy, msgData));
    }

    @Override
    protected String getKeyToUpgradeFromVersionZero() {
        return "deleteFrom";
    }

    boolean matches(String key) {
        return patternKeys.stream().anyMatch(pattern -> pattern.matcher(key).matches());
    }

}
