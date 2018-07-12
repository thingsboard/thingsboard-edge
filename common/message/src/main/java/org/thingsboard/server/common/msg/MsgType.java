/**
 * Thingsboard OÜ ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2018 Thingsboard OÜ. All Rights Reserved.
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
package org.thingsboard.server.common.msg;

/**
 * Created by ashvayka on 15.03.18.
 */
//TODO: add all "See" references
public enum MsgType {

    /**
     * ADDED/UPDATED/DELETED events for server nodes.
     *
     * See {@link org.thingsboard.server.common.msg.cluster.ClusterEventMsg}
     */
    CLUSTER_EVENT_MSG,

    /**
     * All messages, could be send  to cluster
    */
    SEND_TO_CLUSTER_MSG,

    /**
     * ADDED/UPDATED/DELETED events for main entities.
     *
     * See {@link org.thingsboard.server.common.msg.plugin.ComponentLifecycleMsg}
     */
    COMPONENT_LIFE_CYCLE_MSG,

    /**
     * Misc messages from the REST API/SERVICE layer to the new rule engine.
     *
     * See {@link org.thingsboard.server.common.msg.system.ServiceToRuleEngineMsg}
     */
    SERVICE_TO_RULE_ENGINE_MSG,

    /**
     * Message that is sent by RuleChainActor to RuleActor with command to process TbMsg.
     */
    RULE_CHAIN_TO_RULE_MSG,

    /**
     * Message that is sent by RuleChainActor to other RuleChainActor with command to process TbMsg.
     */
    RULE_CHAIN_TO_RULE_CHAIN_MSG,

    /**
     * Message that is sent by RuleActor to RuleChainActor with command to process TbMsg by next nodes in chain.
     */
    RULE_TO_RULE_CHAIN_TELL_NEXT_MSG,

    /**
     * Message forwarded from original rule chain to remote rule chain due to change in the cluster structure or originator entity of the TbMsg.
     */
    REMOTE_TO_RULE_CHAIN_TELL_NEXT_MSG,

    /**
     * Message that is sent by RuleActor implementation to RuleActor itself to log the error.
     */
    RULE_TO_SELF_ERROR_MSG,

    /**
     * Message that is sent by RuleActor implementation to RuleActor itself to process the message.
     */
    RULE_TO_SELF_MSG,

    /**
     * Message that is sent by Session Actor to Device Actor. Represents messages from the device itself.
     */
    DEVICE_SESSION_TO_DEVICE_ACTOR_MSG,

    DEVICE_ATTRIBUTES_UPDATE_TO_DEVICE_ACTOR_MSG,

    DEVICE_CREDENTIALS_UPDATE_TO_DEVICE_ACTOR_MSG,

    DEVICE_NAME_OR_TYPE_UPDATE_TO_DEVICE_ACTOR_MSG,

    DEVICE_RPC_REQUEST_TO_DEVICE_ACTOR_MSG,

    SERVER_RPC_RESPONSE_TO_DEVICE_ACTOR_MSG,

    DEVICE_ACTOR_SERVER_SIDE_RPC_TIMEOUT_MSG,

    DEVICE_ACTOR_CLIENT_SIDE_RPC_TIMEOUT_MSG,

    DEVICE_ACTOR_QUEUE_TIMEOUT_MSG,

    /**
     * Message that is sent from the Device Actor to Rule Engine. Requires acknowledgement
     */
    DEVICE_ACTOR_TO_RULE_ENGINE_MSG,

    /**
     * Message that is sent from Rule Engine to the Device Actor when message is successfully pushed to queue.
     */
    RULE_ENGINE_QUEUE_PUT_ACK_MSG,
    ACTOR_SYSTEM_TO_DEVICE_SESSION_ACTOR_MSG,
    TRANSPORT_TO_DEVICE_SESSION_ACTOR_MSG,
    SESSION_TIMEOUT_MSG,
    SESSION_CTRL_MSG;

}
