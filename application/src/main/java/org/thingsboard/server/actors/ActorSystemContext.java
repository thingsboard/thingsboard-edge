/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2019 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.actors;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Scheduler;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.thingsboard.js.api.JsInvokeService;
import org.thingsboard.rule.engine.api.MailService;
import org.thingsboard.rule.engine.api.ReportService;
import org.thingsboard.rule.engine.api.RuleChainTransactionService;
import org.thingsboard.server.actors.service.ActorService;
import org.thingsboard.server.actors.tenant.DebugTbRateLimits;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.Event;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.cluster.ServerAddress;
import org.thingsboard.server.common.msg.tools.TbRateLimits;
import org.thingsboard.server.common.transport.auth.DeviceAuthService;
import org.thingsboard.server.dao.alarm.AlarmService;
import org.thingsboard.server.dao.asset.AssetService;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.audit.AuditLogService;
import org.thingsboard.server.dao.blob.BlobEntityService;
import org.thingsboard.server.dao.converter.ConverterService;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.dao.dashboard.DashboardService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.entityview.EntityViewService;
import org.thingsboard.server.dao.event.EventService;
import org.thingsboard.server.dao.group.EntityGroupService;
import org.thingsboard.server.dao.grouppermission.GroupPermissionService;
import org.thingsboard.server.dao.integration.IntegrationService;
import org.thingsboard.server.dao.relation.RelationService;
import org.thingsboard.server.dao.role.RoleService;
import org.thingsboard.server.dao.rule.RuleChainService;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.dao.timeseries.TimeseriesService;
import org.thingsboard.server.dao.user.UserService;
import org.thingsboard.server.kafka.TbNodeIdProvider;
import org.thingsboard.server.service.cluster.discovery.DiscoveryService;
import org.thingsboard.server.service.cluster.routing.ClusterRoutingService;
import org.thingsboard.server.service.cluster.rpc.ClusterRpcService;
import org.thingsboard.server.service.component.ComponentDiscoveryService;
import org.thingsboard.server.service.converter.DataConverterService;
import org.thingsboard.server.service.encoding.DataDecodingEncodingService;
import org.thingsboard.server.service.executors.ClusterRpcCallbackExecutorService;
import org.thingsboard.server.service.executors.DbCallbackExecutorService;
import org.thingsboard.server.service.executors.ExternalCallExecutorService;
import org.thingsboard.server.service.executors.SharedEventLoopGroupService;
import org.thingsboard.server.service.integration.PlatformIntegrationService;
import org.thingsboard.server.service.mail.MailExecutorService;
import org.thingsboard.server.service.rpc.DeviceRpcService;
import org.thingsboard.server.service.ruleengine.RuleEngineCallService;
import org.thingsboard.server.service.scheduler.SchedulerService;
import org.thingsboard.server.service.script.JsExecutorService;
import org.thingsboard.server.service.security.permission.OwnersCacheService;
import org.thingsboard.server.service.session.DeviceSessionCacheService;
import org.thingsboard.server.service.state.DeviceStateService;
import org.thingsboard.server.service.telemetry.TelemetrySubscriptionService;
import org.thingsboard.server.service.transport.RuleEngineTransportService;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Slf4j
@Component
public class ActorSystemContext {
    private static final String AKKA_CONF_FILE_NAME = "actor-system.conf";

    protected final ObjectMapper mapper = new ObjectMapper();

    private final ConcurrentMap<TenantId, DebugTbRateLimits> debugPerTenantLimits = new ConcurrentHashMap<>();

    public ConcurrentMap<TenantId, DebugTbRateLimits> getDebugPerTenantLimits() {
        return debugPerTenantLimits;
    }

    @Getter
    @Setter
    private ActorService actorService;

    @Autowired
    @Getter
    private DiscoveryService discoveryService;

    @Autowired
    @Getter
    @Setter
    private ComponentDiscoveryService componentService;

    @Autowired
    @Getter
    private ClusterRoutingService routingService;

    @Autowired
    @Getter
    private ClusterRpcService rpcService;

    @Autowired
    @Getter
    private DataDecodingEncodingService encodingService;

    @Autowired
    @Getter
    private DeviceAuthService deviceAuthService;

    @Autowired
    @Getter
    private DeviceService deviceService;

    @Autowired
    @Getter
    private AssetService assetService;

    @Autowired
    @Getter
    private DashboardService dashboardService;

    @Autowired
    @Getter
    private TenantService tenantService;

    @Autowired
    @Getter
    private CustomerService customerService;

    @Autowired
    @Getter
    private UserService userService;

    @Autowired
    @Getter
    private RuleChainService ruleChainService;

    @Autowired
    @Getter
    private TimeseriesService tsService;

    @Autowired
    @Getter
    private AttributesService attributesService;

    @Autowired
    @Getter
    private EventService eventService;

    @Autowired
    @Getter
    private AlarmService alarmService;

    @Autowired
    @Getter
    private RelationService relationService;

    @Autowired
    @Getter
    private AuditLogService auditLogService;

    @Autowired
    @Getter
    private EntityViewService entityViewService;

    @Autowired
    @Getter
    private TelemetrySubscriptionService tsSubService;

    @Autowired
    @Getter
    private DeviceRpcService deviceRpcService;

    @Autowired
    @Getter
    private JsInvokeService jsSandbox;

    @Autowired
    @Getter
    private JsExecutorService jsExecutor;

    @Autowired
    @Getter
    private MailExecutorService mailExecutor;

    @Autowired
    @Getter private ConverterService converterService;

    @Autowired
    @Getter private IntegrationService integrationService;

    @Autowired
    @Getter private EntityGroupService entityGroupService;

    @Autowired
    @Getter private ReportService reportService;

    @Autowired
    @Getter private BlobEntityService blobEntityService;

    @Autowired
    @Getter private GroupPermissionService groupPermissionService;

    @Autowired
    @Getter private RoleService roleService;

    @Autowired
    @Getter
    private ClusterRpcCallbackExecutorService clusterRpcCallbackExecutor;

    @Autowired
    @Getter
    private DbCallbackExecutorService dbCallbackExecutor;

    @Autowired
    @Getter
    private ExternalCallExecutorService externalCallExecutorService;

    @Autowired
    @Getter
    private SharedEventLoopGroupService sharedEventLoopGroupService;

    @Autowired
    @Getter
    private MailService mailService;

    @Autowired
    @Getter
    private DeviceStateService deviceStateService;

    @Autowired
    @Getter
    private DeviceSessionCacheService deviceSessionCacheService;

    @Lazy
    @Autowired
    @Getter
    private RuleEngineTransportService ruleEngineTransportService;

    @Autowired
    @Getter
    private SchedulerService schedulerService;

    @Autowired
    @Getter
    private RuleEngineCallService ruleEngineCallService;

    @Autowired
    @Getter
    private OwnersCacheService ownersCacheService;

    @Lazy
    @Autowired
    @Getter
    private RuleChainTransactionService ruleChainTransactionService;

    @Value("${cluster.partition_id}")
    @Getter
    private long queuePartitionId;

    @Autowired
    @Getter private PlatformIntegrationService platformIntegrationService;

    @Autowired
    @Getter private DataConverterService dataConverterService;

    @Value("${actors.session.max_concurrent_sessions_per_device:1}")
    @Getter
    private long maxConcurrentSessionsPerDevice;

    @Value("${actors.session.sync.timeout}")
    @Getter
    private long syncSessionTimeout;

    @Value("${actors.queue.enabled}")
    @Getter
    private boolean queuePersistenceEnabled;

    @Value("${actors.queue.timeout}")
    @Getter
    private long queuePersistenceTimeout;

    @Value("${actors.client_side_rpc.timeout}")
    @Getter
    private long clientSideRpcTimeout;

    @Value("${actors.rule.chain.error_persist_frequency}")
    @Getter
    private long ruleChainErrorPersistFrequency;

    @Value("${actors.rule.node.error_persist_frequency}")
    @Getter
    private long ruleNodeErrorPersistFrequency;

    @Value("${actors.statistics.enabled}")
    @Getter
    private boolean statisticsEnabled;

    @Value("${actors.statistics.persist_frequency}")
    @Getter
    private long statisticsPersistFrequency;

    @Value("${actors.tenant.create_components_on_init}")
    @Getter
    private boolean tenantComponentsInitEnabled;

    @Value("${actors.rule.allow_system_mail_service}")
    @Getter
    private boolean allowSystemMailService;

    @Value("${transport.sessions.inactivity_timeout}")
    @Getter
    private long sessionInactivityTimeout;

    @Value("${transport.sessions.report_timeout}")
    @Getter
    private long sessionReportTimeout;

    @Value("${actors.rule.chain.debug_mode_rate_limits_per_tenant.enabled}")
    @Getter
    private boolean debugPerTenantEnabled;

    @Value("${actors.rule.chain.debug_mode_rate_limits_per_tenant.configuration}")
    @Getter
    private String debugPerTenantLimitsConfiguration;

    @Getter
    @Setter
    private ActorSystem actorSystem;

    @Autowired
    @Getter
    private TbNodeIdProvider nodeIdProvider;

    @Getter
    @Setter
    private ActorRef appActor;

    @Getter
    @Setter
    private ActorRef statsActor;

    @Getter
    private final Config config;

    public ActorSystemContext() {
        config = ConfigFactory.parseResources(AKKA_CONF_FILE_NAME).withFallback(ConfigFactory.load());
    }

    public Scheduler getScheduler() {
        return actorSystem.scheduler();
    }

    public void persistError(TenantId tenantId, EntityId entityId, String method, Exception e) {
        Event event = new Event();
        event.setTenantId(tenantId);
        event.setEntityId(entityId);
        event.setType(DataConstants.ERROR);
        event.setBody(toBodyJson(discoveryService.getCurrentServer().getServerAddress(), method, toString(e)));
        persistEvent(event);
    }

    public void persistLifecycleEvent(TenantId tenantId, EntityId entityId, ComponentLifecycleEvent lcEvent, Exception e) {
        Event event = new Event();
        event.setTenantId(tenantId);
        event.setEntityId(entityId);
        event.setType(DataConstants.LC_EVENT);
        event.setBody(toBodyJson(discoveryService.getCurrentServer().getServerAddress(), lcEvent, Optional.ofNullable(e)));
        persistEvent(event);
    }

    private void persistEvent(Event event) {
        eventService.save(event);
    }

    private String toString(Throwable e) {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }

    private JsonNode toBodyJson(ServerAddress server, ComponentLifecycleEvent event, Optional<Exception> e) {
        ObjectNode node = mapper.createObjectNode().put("server", server.toString()).put("event", event.name());
        if (e.isPresent()) {
            node = node.put("success", false);
            node = node.put("error", toString(e.get()));
        } else {
            node = node.put("success", true);
        }
        return node;
    }

    private JsonNode toBodyJson(ServerAddress server, String method, String body) {
        return mapper.createObjectNode().put("server", server.toString()).put("method", method).put("error", body);
    }

    public String getServerAddress() {
        return discoveryService.getCurrentServer().getServerAddress().toString();
    }

    public void persistDebugInput(TenantId tenantId, EntityId entityId, TbMsg tbMsg, String relationType) {
        persistDebugAsync(tenantId, entityId, "IN", tbMsg, relationType, null);
    }

    public void persistDebugInput(TenantId tenantId, EntityId entityId, TbMsg tbMsg, String relationType, Throwable error) {
        persistDebugAsync(tenantId, entityId, "IN", tbMsg, relationType, error);
    }

    public void persistDebugOutput(TenantId tenantId, EntityId entityId, TbMsg tbMsg, String relationType, Throwable error) {
        persistDebugAsync(tenantId, entityId, "OUT", tbMsg, relationType, error);
    }

    public void persistDebugOutput(TenantId tenantId, EntityId entityId, TbMsg tbMsg, String relationType) {
        persistDebugAsync(tenantId, entityId, "OUT", tbMsg, relationType, null);
    }

    private void persistDebugAsync(TenantId tenantId, EntityId entityId, String type, TbMsg tbMsg, String relationType, Throwable error) {
        if (checkLimits(tenantId, tbMsg, error)) {
            try {
                Event event = new Event();
                event.setTenantId(tenantId);
                event.setEntityId(entityId);
                event.setType(DataConstants.DEBUG_RULE_NODE);

                String metadata = mapper.writeValueAsString(tbMsg.getMetaData().getData());

                ObjectNode node = mapper.createObjectNode()
                        .put("type", type)
                        .put("server", getServerAddress())
                        .put("entityId", tbMsg.getOriginator().getId().toString())
                        .put("entityName", tbMsg.getOriginator().getEntityType().name())
                        .put("msgId", tbMsg.getId().toString())
                        .put("msgType", tbMsg.getType())
                        .put("dataType", tbMsg.getDataType().name())
                        .put("relationType", relationType)
                        .put("data", tbMsg.getData())
                        .put("metadata", metadata);

                if (error != null) {
                    node = node.put("error", toString(error));
                }

                event.setBody(node);
                ListenableFuture<Event> future = eventService.saveAsync(event);
                Futures.addCallback(future, new FutureCallback<Event>() {
                    @Override
                    public void onSuccess(@Nullable Event event) {

                    }

                    @Override
                    public void onFailure(Throwable th) {
                        log.error("Could not save debug Event for Node", th);
                    }
                });
            } catch (IOException ex) {
                log.warn("Failed to persist rule node debug message", ex);
            }
        }
    }

    private boolean checkLimits(TenantId tenantId, TbMsg tbMsg, Throwable error) {
        if (debugPerTenantEnabled) {
            DebugTbRateLimits debugTbRateLimits = debugPerTenantLimits.computeIfAbsent(tenantId, id ->
                    new DebugTbRateLimits(new TbRateLimits(debugPerTenantLimitsConfiguration), false));

            if (!debugTbRateLimits.getTbRateLimits().tryConsume()) {
                if (!debugTbRateLimits.isRuleChainEventSaved()) {
                    persistRuleChainDebugModeEvent(tenantId, tbMsg.getRuleChainId(), error);
                    debugTbRateLimits.setRuleChainEventSaved(true);
                }
                if (log.isTraceEnabled()) {
                    log.trace("[{}] Tenant level debug mode rate limit detected: {}", tenantId, tbMsg);
                }
                return false;
            }
        }
        return true;
    }

    private void persistRuleChainDebugModeEvent(TenantId tenantId, EntityId entityId, Throwable error) {
        Event event = new Event();
        event.setTenantId(tenantId);
        event.setEntityId(entityId);
        event.setType(DataConstants.DEBUG_RULE_CHAIN);

        ObjectNode node = mapper.createObjectNode()
                //todo: what fields are needed here?
                .put("server", getServerAddress())
                .put("message", "Reached debug mode rate limit!");

        if (error != null) {
            node = node.put("error", toString(error));
        }

        event.setBody(node);
        ListenableFuture<Event> future = eventService.saveAsync(event);
        Futures.addCallback(future, new FutureCallback<Event>() {
            @Override
            public void onSuccess(@Nullable Event event) {

            }

            @Override
            public void onFailure(Throwable th) {
                log.error("Could not save debug Event for Rule Chain", th);
            }
        });
    }

    public static Exception toException(Throwable error) {
        return Exception.class.isInstance(error) ? (Exception) error : new Exception(error);
    }

}
