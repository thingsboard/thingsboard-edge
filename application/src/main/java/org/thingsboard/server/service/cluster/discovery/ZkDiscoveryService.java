/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2018 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.service.cluster.discovery;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.SerializationException;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.curator.framework.state.ConnectionStateListener;
import org.apache.curator.retry.RetryForever;
import org.apache.curator.utils.CloseableUtils;
import org.apache.zookeeper.CreateMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.thingsboard.server.actors.service.ActorService;
import org.thingsboard.server.common.msg.cluster.ServerAddress;
import org.thingsboard.server.service.scheduler.SchedulerService;
import org.thingsboard.server.service.cluster.routing.ClusterRoutingService;
import org.thingsboard.server.service.state.DeviceStateService;
import org.thingsboard.server.service.telemetry.TelemetrySubscriptionService;
import org.thingsboard.server.utils.MiscUtils;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

/**
 * @author Andrew Shvayka
 */
@Service
@ConditionalOnProperty(prefix = "zk", value = "enabled", havingValue = "true", matchIfMissing = false)
@Slf4j
public class ZkDiscoveryService implements DiscoveryService, PathChildrenCacheListener, ApplicationListener<ApplicationReadyEvent> {

    @Value("${zk.url}")
    private String zkUrl;
    @Value("${zk.retry_interval_ms}")
    private Integer zkRetryInterval;
    @Value("${zk.connection_timeout_ms}")
    private Integer zkConnectionTimeout;
    @Value("${zk.session_timeout_ms}")
    private Integer zkSessionTimeout;
    @Value("${zk.zk_dir}")
    private String zkDir;

    private String zkNodesDir;

    @Autowired
    private ServerInstanceService serverInstance;

    @Autowired
    @Lazy
    private TelemetrySubscriptionService tsSubService;

    @Autowired
    @Lazy
    private DeviceStateService deviceStateService;

    @Autowired
    @Lazy
    protected SchedulerService schedulerService;

    @Autowired
    @Lazy
    private ActorService actorService;

    @Autowired
    @Lazy
    private ClusterRoutingService routingService;

    private CuratorFramework client;
    private PathChildrenCache cache;
    private String nodePath;
    //TODO: make persistent?
    private String nodeId;

    @PostConstruct
    public void init() {
        log.info("Initializing...");
        this.nodeId = RandomStringUtils.randomAlphabetic(10);
        Assert.hasLength(zkUrl, MiscUtils.missingProperty("zk.url"));
        Assert.notNull(zkRetryInterval, MiscUtils.missingProperty("zk.retry_interval_ms"));
        Assert.notNull(zkConnectionTimeout, MiscUtils.missingProperty("zk.connection_timeout_ms"));
        Assert.notNull(zkSessionTimeout, MiscUtils.missingProperty("zk.session_timeout_ms"));

        log.info("Initializing discovery service using ZK connect string: {}", zkUrl);

        zkNodesDir = zkDir + "/nodes";
        try {
            client = CuratorFrameworkFactory.newClient(zkUrl, zkSessionTimeout, zkConnectionTimeout, new RetryForever(zkRetryInterval));
            client.start();
            client.blockUntilConnected();
            cache = new PathChildrenCache(client, zkNodesDir, true);
            cache.getListenable().addListener(this);
            cache.start();
        } catch (Exception e) {
            log.error("Failed to connect to ZK: {}", e.getMessage(), e);
            CloseableUtils.closeQuietly(client);
            throw new RuntimeException(e);
        }
    }

    @PreDestroy
    public void destroy() {
        unpublishCurrentServer();
        CloseableUtils.closeQuietly(client);
        log.info("Stopped discovery service");
    }

    @Override
    public void publishCurrentServer() {
        try {
            ServerInstance self = this.serverInstance.getSelf();
            log.info("[{}:{}] Creating ZK node for current instance", self.getHost(), self.getPort());
            nodePath = client.create()
                    .creatingParentsIfNeeded()
                    .withMode(CreateMode.EPHEMERAL_SEQUENTIAL).forPath(zkNodesDir + "/", SerializationUtils.serialize(self.getServerAddress()));
            log.info("[{}:{}] Created ZK node for current instance: {}", self.getHost(), self.getPort(), nodePath);
            client.getConnectionStateListenable().addListener(checkReconnect(self));
        } catch (Exception e) {
            log.error("Failed to create ZK node", e);
            throw new RuntimeException(e);
        }
    }

    private ConnectionStateListener checkReconnect(ServerInstance self) {
        return (client, newState) -> {
            log.info("[{}:{}] ZK state changed: {}", self.getHost(), self.getPort(), newState);
            if (newState == ConnectionState.LOST) {
                reconnect();
            }
        };
    }

    private boolean reconnectInProgress = false;

    private synchronized void reconnect() {
        if (!reconnectInProgress) {
            reconnectInProgress = true;
            try {
                client.blockUntilConnected();
                publishCurrentServer();
            } catch (InterruptedException e) {
                log.error("Failed to reconnect to ZK: {}", e.getMessage(), e);
            } finally {
                reconnectInProgress = false;
            }
        }
    }

    @Override
    public void unpublishCurrentServer() {
        try {
            if (nodePath != null) {
                client.delete().forPath(nodePath);
            }
        } catch (Exception e) {
            log.error("Failed to delete ZK node {}", nodePath, e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getNodeId() {
        return nodeId;
    }

    @Override
    public ServerInstance getCurrentServer() {
        return serverInstance.getSelf();
    }

    @Override
    public List<ServerInstance> getOtherServers() {
        return cache.getCurrentData().stream()
                .filter(cd -> !cd.getPath().equals(nodePath))
                .map(cd -> {
                    try {
                        return new ServerInstance((ServerAddress) SerializationUtils.deserialize(cd.getData()));
                    } catch (NoSuchElementException e) {
                        log.error("Failed to decode ZK node", e);
                        throw new RuntimeException(e);
                    }
                })
                .collect(Collectors.toList());
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent applicationReadyEvent) {
        publishCurrentServer();
        getOtherServers().forEach(
                server -> log.info("Found active server: [{}:{}]", server.getHost(), server.getPort())
        );
    }

    @Override
    public void childEvent(CuratorFramework curatorFramework, PathChildrenCacheEvent pathChildrenCacheEvent) throws Exception {
        ChildData data = pathChildrenCacheEvent.getData();
        if (data == null) {
            log.debug("Ignoring {} due to empty child data", pathChildrenCacheEvent);
            return;
        } else if (data.getData() == null) {
            log.debug("Ignoring {} due to empty child's data", pathChildrenCacheEvent);
            return;
        } else if (nodePath != null && nodePath.equals(data.getPath())) {
            log.debug("Ignoring event about current server {}", pathChildrenCacheEvent);
            return;
        }
        ServerInstance instance;
        try {
            ServerAddress serverAddress = SerializationUtils.deserialize(data.getData());
            instance = new ServerInstance(serverAddress);
        } catch (SerializationException e) {
            log.error("Failed to decode server instance for node {}", data.getPath(), e);
            throw e;
        }
        log.info("Processing [{}] event for [{}:{}]", pathChildrenCacheEvent.getType(), instance.getHost(), instance.getPort());
        switch (pathChildrenCacheEvent.getType()) {
            case CHILD_ADDED:
                routingService.onServerAdded(instance);
                tsSubService.onClusterUpdate();
                deviceStateService.onClusterUpdate();
                schedulerService.onClusterUpdate();
                actorService.onServerAdded(instance);
                break;
            case CHILD_UPDATED:
                routingService.onServerUpdated(instance);
                actorService.onServerUpdated(instance);
                break;
            case CHILD_REMOVED:
                routingService.onServerRemoved(instance);
                tsSubService.onClusterUpdate();
                deviceStateService.onClusterUpdate();
                schedulerService.onClusterUpdate();
                actorService.onServerRemoved(instance);
                break;
            default:
                break;
        }
    }
}
