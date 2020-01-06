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
import org.thingsboard.server.common.data.rule.RuleNode;
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

/**
 * Created by ashvayka on 13.01.18.
 */
public interface TbContext {

    void tellNext(TbMsg msg, String relationType);

    void tellNext(TbMsg msg, String relationType, Throwable th);

    void tellNext(TbMsg msg, Set<String> relationTypes);

    void tellSelf(TbMsg msg, long delayMs);

    boolean isLocalEntity(EntityId entityId);

    void tellFailure(TbMsg msg, Throwable th);

    void updateSelf(RuleNode self);

    void sendTbMsgToRuleEngine(TbMsg msg);

    TbMsg newMsg(String type, EntityId originator, TbMsgMetaData metaData, String data);

    TbMsg transformMsg(TbMsg origMsg, String type, EntityId originator, TbMsgMetaData metaData, String data);

    TbMsg customerCreatedMsg(Customer customer, RuleNodeId ruleNodeId);

    TbMsg deviceCreatedMsg(Device device, RuleNodeId ruleNodeId);

    TbMsg assetCreatedMsg(Asset asset, RuleNodeId ruleNodeId);

    TbMsg alarmCreatedMsg(Alarm alarm, RuleNodeId ruleNodeId);

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

    String getNodeId();

    RuleChainTransactionService getRuleChainTransactionService();

    EventLoopGroup getSharedEventLoop();

    CassandraCluster getCassandraCluster();

    ResultSetFuture submitCassandraTask(CassandraStatementTask task);

    RedisTemplate<String, Object> getRedisTemplate();

    String getServerAddress();
}
