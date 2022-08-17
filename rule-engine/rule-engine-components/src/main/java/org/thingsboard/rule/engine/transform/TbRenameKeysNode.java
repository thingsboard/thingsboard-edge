/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2022 ThingsBoard, Inc. All Rights Reserved.
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
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;

import java.util.Map;
import java.util.concurrent.ExecutionException;

@Slf4j
@RuleNode(
        type = ComponentType.TRANSFORMATION,
        name = "rename keys",
        configClazz = TbRenameKeysNodeConfiguration.class,
        nodeDescription = "Renames msg data or metadata keys to the new key names selected in the key mapping.",
        nodeDetails = "If the key that is selected in the key mapping is missed in the selected msg source(data or metadata), it will be ignored." +
                " If the msg data is not a JSON object returns the incoming message as outbound message with <code>Failure</code> chain," +
                " otherwise returns transformed messages via <code>Success</code> chain",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbTransformationNodeRenameKeysConfig",
        icon = "find_replace"
)
public class TbRenameKeysNode implements TbNode {

    TbRenameKeysNodeConfiguration config;

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, TbRenameKeysNodeConfiguration.class);
    }

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) throws ExecutionException, InterruptedException, TbNodeException {
        Map<String, String> renameKeysMapping = config.getRenameKeysMapping();
        TbMsgMetaData metaData = msg.getMetaData();
        String data = msg.getData();
        if (config.isFromMetadata()) {
            Map<String, String> metaDataMap = metaData.getData();
            renameKeysMapping.forEach((nameKey, newNameKey) -> {
                if (metaDataMap.containsKey(nameKey)) {
                    metaDataMap.put(newNameKey, metaDataMap.get(nameKey));
                    metaDataMap.remove(nameKey);
                }
            });
            metaData = new TbMsgMetaData(metaDataMap);
        } else {
            JsonNode dataNode = JacksonUtil.toJsonNode(msg.getData());
            if (dataNode.isObject()) {
                ObjectNode msgData = (ObjectNode) dataNode;
                renameKeysMapping.forEach((nameKey, newNameKey) -> {
                    if (msgData.has(nameKey)) {
                        msgData.set(newNameKey, msgData.get(nameKey));
                        msgData.remove(nameKey);
                    }
                });
                data = JacksonUtil.toString(msgData);
            } else {
                ctx.tellFailure(msg, new RuntimeException("Msg data is not a JSON Object!"));
                return;
            }
        }
        ctx.tellSuccess(TbMsg.transformMsg(msg, msg.getType(), msg.getOriginator(), metaData, data));
    }

    @Override
    public void destroy() {
    }
}

