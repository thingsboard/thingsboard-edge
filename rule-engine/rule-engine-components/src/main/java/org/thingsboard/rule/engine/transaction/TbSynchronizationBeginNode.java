/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2020 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.rule.engine.transaction;

import lombok.extern.slf4j.Slf4j;
import org.thingsboard.rule.engine.api.EmptyNodeConfiguration;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgDataType;
import org.thingsboard.server.common.msg.TbMsgTransactionData;

import java.util.concurrent.ExecutionException;

import static org.thingsboard.rule.engine.api.TbRelationTypes.SUCCESS;

@Slf4j
@RuleNode(
        type = ComponentType.ACTION,
        name = "synchronization start",
        configClazz = EmptyNodeConfiguration.class,
        nodeDescription = "Starts synchronization of message processing based on message originator",
        nodeDetails = "This node should be used together with \"synchronization end\" node. \n This node will put messages into queue based on message originator id. \n" +
                "Subsequent messages will not be processed until the previous message processing is completed or timeout event occurs.\n" +
                "Size of the queue per originator and timeout values are configurable on a system level",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbNodeEmptyConfig")
public class TbSynchronizationBeginNode implements TbNode {

    private EmptyNodeConfiguration config;

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, EmptyNodeConfiguration.class);
    }

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) {
        log.trace("Msg enters transaction - [{}][{}]", msg.getId(), msg.getType());

        TbMsgTransactionData transactionData = new TbMsgTransactionData(msg.getId(), msg.getOriginator());
        TbMsg tbMsg = new TbMsg(msg.getId(), msg.getType(), msg.getOriginator(), msg.getMetaData(), TbMsgDataType.JSON,
                msg.getData(), transactionData, msg.getRuleChainId(), msg.getRuleNodeId(), msg.getClusterPartition());

        ctx.getRuleChainTransactionService().beginTransaction(tbMsg, startMsg -> {
                    log.trace("Transaction starting...[{}][{}]", startMsg.getId(), startMsg.getType());
                    ctx.tellNext(startMsg, SUCCESS);
                }, endMsg -> log.trace("Transaction ended successfully...[{}][{}]", endMsg.getId(), endMsg.getType()),
                throwable -> {
                    log.trace("Transaction failed! [{}][{}]", tbMsg.getId(), tbMsg.getType(), throwable);
                    ctx.tellFailure(tbMsg, throwable);
                });
    }

    @Override
    public void destroy() {

    }
}
