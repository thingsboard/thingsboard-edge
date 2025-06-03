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
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.OutputFrame;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@Slf4j
public class RedisJUnit5Test {

    @Container
    private static final GenericContainer REDIS = new GenericContainer("bitnami/valkey:8.0")
            .withEnv("ALLOW_EMPTY_PASSWORD","yes")
            .withLogConsumer(s -> log.error(((OutputFrame) s).getUtf8String().trim()))
            .withExposedPorts(6379);

    @BeforeAll
    static void beforeAll() {
        log.warn("Starting redis...");
        REDIS.start();
        System.setProperty("cache.type", "redis");
        System.setProperty("redis.connection.type", "standalone");
        System.setProperty("redis.standalone.host", REDIS.getHost());
        System.setProperty("redis.standalone.port", String.valueOf(REDIS.getMappedPort(6379)));

    }

    @AfterAll
    static void afterAll() {
        List.of("cache.type", "redis.connection.type", "redis.standalone.host", "redis.standalone.port")
                .forEach(System.getProperties()::remove);
        REDIS.stop();
        log.warn("Redis is stopped");
    }

    @Test
    void test() {
        assertThat(REDIS.isRunning()).isTrue();
    }

}
