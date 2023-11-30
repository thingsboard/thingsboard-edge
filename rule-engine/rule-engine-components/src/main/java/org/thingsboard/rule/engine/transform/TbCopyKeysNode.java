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

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@RuleNode(
        type = ComponentType.TRANSFORMATION,
        name = "copy key-values",
        version = 1,
        configClazz = TbCopyKeysNodeConfiguration.class,
        nodeDescription = "Copies key-values from message to message metadata or vice-versa.",
        nodeDetails = "Fetches key-values from message or message metadata based on the keys list specified in the configuration " +
                "and copies them into message metadata or into message itself in accordance with the fetch source. " +
                "Keys that are absent in the fetch source will be ignored. " +
                "Use regular expression(s) as a key(s) to copy keys by pattern.<br><br>" +
                "Output connections: <code>Success</code>, <code>Failure</code>.",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbTransformationNodeCopyKeysConfig",
        icon = "content_copy"
)
public class TbCopyKeysNode extends TbAbstractTransformNodeWithTbMsgSource {

    private TbCopyKeysNodeConfiguration config;
    private List<Pattern> patternKeys;
    private TbMsgSource copyFrom;

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, TbCopyKeysNodeConfiguration.class);
        this.copyFrom = config.getCopyFrom();
        if (copyFrom == null) {
            throw new TbNodeException("CopyFrom can't be null! Allowed values: " + Arrays.toString(TbMsgSource.values()));
        }
        this.patternKeys = config.getKeys().stream().map(Pattern::compile).collect(Collectors.toList());
    }

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) throws ExecutionException, InterruptedException, TbNodeException {
        var metaDataCopy = msg.getMetaData().copy();
        String msgData = msg.getData();
        boolean msgChanged = false;
        JsonNode dataNode = JacksonUtil.toJsonNode(msgData);
        if (dataNode.isObject()) {
            switch (copyFrom) {
                case METADATA:
                    ObjectNode msgDataNode = (ObjectNode) dataNode;
                    Map<String, String> metaDataMap = metaDataCopy.getData();
                    for (Map.Entry<String, String> entry : metaDataMap.entrySet()) {
                        String keyData = entry.getKey();
                        if (matches(keyData)) {
                            msgChanged = true;
                            msgDataNode.put(keyData, entry.getValue());
                        }
                    }
                    msgData = JacksonUtil.toString(msgDataNode);
                    break;
                case DATA:
                    Iterator<Map.Entry<String, JsonNode>> iteratorNode = dataNode.fields();
                    while (iteratorNode.hasNext()) {
                        Map.Entry<String, JsonNode> entry = iteratorNode.next();
                        String keyData = entry.getKey();
                        if (matches(keyData)) {
                            msgChanged = true;
                            String value = entry.getValue().isTextual() ?
                                    entry.getValue().asText() : JacksonUtil.toString(entry.getValue());
                            metaDataCopy.putValue(keyData, value);
                        }
                    }
                    break;
                default:
                    log.debug("Unexpected CopyFrom value: {}. Allowed values: {}", copyFrom, TbMsgSource.values());
                    break;
            }
        }
        ctx.tellSuccess(msgChanged ? TbMsg.transformMsg(msg, metaDataCopy, msgData) : msg);
    }

    @Override
    protected String getKeyToUpgradeFromVersionZero() {
        return "copyFrom";
    }

    boolean matches(String key) {
        return patternKeys.stream().anyMatch(pattern -> pattern.matcher(key).matches());
    }

}
