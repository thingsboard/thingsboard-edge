/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2018 ThingsBoard, Inc.. All Rights Reserved.
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
package org.thingsboard.server.service.cluster.routing;

import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.msg.cluster.ServerAddress;
import org.thingsboard.server.service.cluster.discovery.DiscoveryService;
import org.thingsboard.server.service.cluster.discovery.DiscoveryServiceListener;
import org.thingsboard.server.service.cluster.discovery.ServerInstance;
import org.thingsboard.server.utils.MiscUtils;

import javax.annotation.PostConstruct;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * Cluster service implementation based on consistent hash ring
 */

@Service
@Slf4j
public class ConsistentClusterRoutingService implements ClusterRoutingService, DiscoveryServiceListener {

    @Autowired
    private DiscoveryService discoveryService;

    @Value("${cluster.hash_function_name}")
    private String hashFunctionName;
    @Value("${cluster.vitrual_nodes_size}")
    private Integer virtualNodesSize;

    private ServerInstance currentServer;

    private HashFunction hashFunction;

    private final ConcurrentNavigableMap<Long, ServerInstance> circle =
            new ConcurrentSkipListMap<>();

    @PostConstruct
    public void init() {
        log.info("Initializing Cluster routing service!");
        hashFunction = MiscUtils.forName(hashFunctionName);
        discoveryService.addListener(this);
        this.currentServer = discoveryService.getCurrentServer();
        addNode(discoveryService.getCurrentServer());
        for (ServerInstance instance : discoveryService.getOtherServers()) {
            addNode(instance);
        }
        logCircle();
        log.info("Cluster routing service initialized!");
    }

    @Override
    public ServerAddress getCurrentServer() {
        return discoveryService.getCurrentServer().getServerAddress();
    }

    @Override
    public Optional<ServerAddress> resolveById(EntityId entityId) {
        return resolveByUuid(entityId.getId());
    }

    @Override
    public Optional<ServerAddress> resolveByUuid(UUID uuid) {
        Assert.notNull(uuid);
        if (circle.isEmpty()) {
            return Optional.empty();
        }
        Long hash = hashFunction.newHasher().putLong(uuid.getMostSignificantBits())
                .putLong(uuid.getLeastSignificantBits()).hash().asLong();
        if (!circle.containsKey(hash)) {
            ConcurrentNavigableMap<Long, ServerInstance> tailMap =
                    circle.tailMap(hash);
            hash = tailMap.isEmpty() ?
                    circle.firstKey() : tailMap.firstKey();
        }
        ServerInstance result = circle.get(hash);
        if (!currentServer.equals(result)) {
            return Optional.of(result.getServerAddress());
        } else {
            return Optional.empty();
        }
    }

    @Override
    public void onServerAdded(ServerInstance server) {
        log.info("On server added event: {}", server);
        addNode(server);
        logCircle();
    }

    @Override
    public void onServerUpdated(ServerInstance server) {
        log.debug("Ignoring server onUpdate event: {}", server);
    }

    @Override
    public void onServerRemoved(ServerInstance server) {
        log.info("On server removed event: {}", server);
        removeNode(server);
        logCircle();
    }

    private void addNode(ServerInstance instance) {
        for (int i = 0; i < virtualNodesSize; i++) {
            circle.put(hash(instance, i).asLong(), instance);
        }
    }

    private void removeNode(ServerInstance instance) {
        for (int i = 0; i < virtualNodesSize; i++) {
            circle.remove(hash(instance, i).asLong());
        }
    }

    private HashCode hash(ServerInstance instance, int i) {
        return hashFunction.newHasher().putString(instance.getHost(), MiscUtils.UTF8).putInt(instance.getPort()).putInt(i).hash();
    }

    private void logCircle() {
        log.trace("Consistent Hash Circle Start");
        circle.entrySet().forEach((e) -> log.debug("{} -> {}", e.getKey(), e.getValue().getServerAddress()));
        log.trace("Consistent Hash Circle End");
    }

}
