/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2025 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.dao;

import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.junit.ClassRule;
import org.junit.rules.ExternalResource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.OutputFrame;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
public class AbstractRedisClusterContainer {

    static final String NODES = "127.0.0.1:6371,127.0.0.1:6372,127.0.0.1:6373,127.0.0.1:6374,127.0.0.1:6375,127.0.0.1:6376";
    static final String IMAGE = "bitnami/valkey-cluster:8.0";
    static final Map<String,String> ENVS = Map.of(
            "VALKEY_CLUSTER_ANNOUNCE_IP", "127.0.0.1",
            "VALKEY_CLUSTER_DYNAMIC_IPS", "no",
            "ALLOW_EMPTY_PASSWORD", "yes",
            "VALKEY_NODES", NODES
    ); 

    @ClassRule(order = 1)
    public static GenericContainer redis1 = new GenericContainer(IMAGE).withEnv(ENVS).withEnv("VALKEY_PORT_NUMBER", "6371").withNetworkMode("host").withLogConsumer(AbstractRedisClusterContainer::consumeLog);
    @ClassRule(order = 2)
    public static GenericContainer redis2 = new GenericContainer(IMAGE).withEnv(ENVS).withEnv("VALKEY_PORT_NUMBER", "6372").withNetworkMode("host").withLogConsumer(AbstractRedisClusterContainer::consumeLog);
    @ClassRule(order = 3)
    public static GenericContainer redis3 = new GenericContainer(IMAGE).withEnv(ENVS).withEnv("VALKEY_PORT_NUMBER", "6373").withNetworkMode("host").withLogConsumer(AbstractRedisClusterContainer::consumeLog);
    @ClassRule(order = 4)
    public static GenericContainer redis4 = new GenericContainer(IMAGE).withEnv(ENVS).withEnv("VALKEY_PORT_NUMBER", "6374").withNetworkMode("host").withLogConsumer(AbstractRedisClusterContainer::consumeLog);
    @ClassRule(order = 5)
    public static GenericContainer redis5 = new GenericContainer(IMAGE).withEnv(ENVS).withEnv("VALKEY_PORT_NUMBER", "6375").withNetworkMode("host").withLogConsumer(AbstractRedisClusterContainer::consumeLog);
    @ClassRule(order = 6)
    public static GenericContainer redis6 = new GenericContainer(IMAGE).withEnv(ENVS).withEnv("VALKEY_PORT_NUMBER", "6376").withNetworkMode("host").withLogConsumer(AbstractRedisClusterContainer::consumeLog);


    @ClassRule(order = 100)
    public static ExternalResource resource = new ExternalResource() {
        @Override
        protected void before() throws Throwable {
            redis1.start();
            redis2.start();
            redis3.start();
            redis4.start();
            redis5.start();
            redis6.start();

            Thread.sleep(TimeUnit.SECONDS.toMillis(5)); // otherwise not all containers have time to start

            String clusterCreateCommand = "valkey-cli --cluster create " + NODES.replace(","," ") + " --cluster-replicas 1 --cluster-yes";
            log.warn("Command to init ValKey Cluster: {}", clusterCreateCommand);
            var result = redis6.execInContainer("/bin/sh", "-c", clusterCreateCommand);
            log.warn("Init cluster result: {}", result);
            Assertions.assertThat(result.getExitCode()).isEqualTo(0);

            Thread.sleep(TimeUnit.SECONDS.toMillis(5)); // otherwise cluster not always ready

            log.warn("Connect to nodes: {}", NODES);
            System.setProperty("cache.type", "redis");
            System.setProperty("redis.connection.type", "cluster");
            System.setProperty("redis.cluster.nodes", NODES);
            System.setProperty("redis.cluster.useDefaultPoolConfig", "false");
        }

        @Override
        protected void after() {
            redis1.stop();
            redis2.stop();
            redis3.stop();
            redis4.stop();
            redis5.stop();
            redis6.stop();
            List.of("cache.type", "redis.connection.type", "redis.cluster.nodes", "redis.cluster.useDefaultPoolConfig")
                    .forEach(System.getProperties()::remove);
        }
    };

    private static void consumeLog(Object x) {
        log.warn("{}", ((OutputFrame) x).getUtf8StringWithoutLineEnding());
    }
}
