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
package org.thingsboard.rule.engine.api;

import com.datastax.driver.core.ResultSetFuture;
import io.netty.channel.EventLoopGroup;
import org.springframework.data.redis.core.RedisTemplate;
import org.thingsboard.common.util.ListeningExecutor;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.RuleNodeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.dao.alarm.AlarmService;
import org.thingsboard.server.dao.asset.AssetService;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.cassandra.CassandraCluster;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.dao.dashboard.DashboardService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.event.EventService;
import org.thingsboard.server.dao.entityview.EntityViewService;
import org.thingsboard.server.dao.nosql.CassandraStatementTask;
import org.thingsboard.server.dao.relation.RelationService;
import org.thingsboard.server.dao.rule.RuleChainService;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.dao.timeseries.TimeseriesService;
import org.thingsboard.server.dao.user.UserService;

import java.util.Set;
import java.util.function.Consumer;

/**
 * Created by ashvayka on 13.01.18.
 */
public interface TbContext {

    /*
     *
     *  METHODS TO CONTROL THE MESSAGE FLOW
     *
     */

    /**
     * Indicates that message was successfully processed by the rule node.
     * Sends message to all Rule Nodes in the Rule Chain
     * that are connected to the current Rule Node using "Success" relationType.
     *
     * @param msg
     */
    void tellSuccess(TbMsg msg);

    /**
     * Sends message to all Rule Nodes in the Rule Chain
     * that are connected to the current Rule Node using specified relationType.
     *
     * @param msg
     * @param relationType
     */
    void tellNext(TbMsg msg, String relationType);

    /**
     * Sends message to all Rule Nodes in the Rule Chain
     * that are connected to the current Rule Node using one of specified relationTypes.
     *
     * @param msg
     * @param relationTypes
     */
    void tellNext(TbMsg msg, Set<String> relationTypes);

    /**
     * Sends message to the current Rule Node with specified delay in milliseconds.
     * Note: this message is not queued and may be lost in case of a server restart.
     *
     * @param msg
     */
    void tellSelf(TbMsg msg, long delayMs);

    /**
     * Notifies Rule Engine about failure to process current message.
     *
     * @param msg - message
     * @param th  - exception
     */
    void tellFailure(TbMsg msg, Throwable th);

    /**
     * Puts new message to queue for processing by the Root Rule Chain
     *
     * @param msg - message
     */
    void enqueue(TbMsg msg, Runnable onSuccess, Consumer<Throwable> onFailure);

    /**
     * Puts new message to custom queue for processing
     *
     * @param msg - message
     */
    void enqueue(TbMsg msg, String queueName, Runnable onSuccess, Consumer<Throwable> onFailure);

    void enqueueForTellFailure(TbMsg msg, String failureMessage);

    void enqueueForTellNext(TbMsg msg, String relationType);

    void enqueueForTellNext(TbMsg msg, Set<String> relationTypes);

    void enqueueForTellNext(TbMsg msg, String relationType, Runnable onSuccess, Consumer<Throwable> onFailure);

    void enqueueForTellNext(TbMsg msg, Set<String> relationTypes, Runnable onSuccess, Consumer<Throwable> onFailure);

    void enqueueForTellNext(TbMsg msg, String queueName, String relationType, Runnable onSuccess, Consumer<Throwable> onFailure);

    void enqueueForTellNext(TbMsg msg, String queueName, Set<String> relationTypes, Runnable onSuccess, Consumer<Throwable> onFailure);

    void ack(TbMsg tbMsg);

    TbMsg newMsg(String type, EntityId originator, TbMsgMetaData metaData, String data);

    TbMsg transformMsg(TbMsg origMsg, String type, EntityId originator, TbMsgMetaData metaData, String data);

    TbMsg customerCreatedMsg(Customer customer, RuleNodeId ruleNodeId);

    TbMsg deviceCreatedMsg(Device device, RuleNodeId ruleNodeId);

    TbMsg assetCreatedMsg(Asset asset, RuleNodeId ruleNodeId);

    // TODO: Does this changes the message?
    TbMsg alarmCreatedMsg(Alarm alarm, RuleNodeId ruleNodeId);

    /*
     *
     *  METHODS TO PROCESS THE MESSAGES
     *
     */

    boolean isLocalEntity(EntityId entityId);

    RuleNodeId getSelfId();

    TenantId getTenantId();

    AttributesService getAttributesService();

    CustomerService getCustomerService();

    TenantService getTenantService();

    UserService getUserService();

    AssetService getAssetService();

    DeviceService getDeviceService();

    DashboardService getDashboardService();

    AlarmService getAlarmService();

    RuleChainService getRuleChainService();

    RuleEngineRpcService getRpcService();

    RuleEngineTelemetryService getTelemetryService();

    TimeseriesService getTimeseriesService();

    RelationService getRelationService();

    EntityViewService getEntityViewService();

    ListeningExecutor getJsExecutor();

    ListeningExecutor getMailExecutor();

    ListeningExecutor getDbCallbackExecutor();

    ListeningExecutor getExternalCallExecutor();

    MailService getMailService();

    ScriptEngine createJsScriptEngine(String script, String... argNames);

    EventService getEventService();
    /**
     * This context is available only in TB PE.
     */
    TbPeContext getPeContext();

    void logJsEvalRequest();

    void logJsEvalResponse();

    void logJsEvalFailure();

    String getServiceId();

    EventLoopGroup getSharedEventLoop();

    CassandraCluster getCassandraCluster();

    ResultSetFuture submitCassandraTask(CassandraStatementTask task);

    @Deprecated
    RedisTemplate<String, Object> getRedisTemplate();

}
