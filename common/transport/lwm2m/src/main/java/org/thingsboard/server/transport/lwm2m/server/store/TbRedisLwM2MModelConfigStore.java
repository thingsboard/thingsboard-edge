/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2023 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.transport.lwm2m.server.store;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.RedisClusterConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.transport.lwm2m.server.model.LwM2MModelConfig;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@AllArgsConstructor
public class TbRedisLwM2MModelConfigStore implements TbLwM2MModelConfigStore {
    private static final String MODEL_EP = "MODEL#EP#";
    private final RedisConnectionFactory connectionFactory;

    @Override
    public List<LwM2MModelConfig> getAll() {
        try (var connection = connectionFactory.getConnection()) {
            List<LwM2MModelConfig> configs = new ArrayList<>();
            ScanOptions scanOptions = ScanOptions.scanOptions().count(100).match(MODEL_EP + "*").build();
            List<Cursor<byte[]>> scans = new ArrayList<>();
            if (connection instanceof RedisClusterConnection) {
                ((RedisClusterConnection) connection).clusterGetNodes().forEach(node -> {
                    scans.add(((RedisClusterConnection) connection).scan(node, scanOptions));
                });
            } else {
                scans.add(connection.scan(scanOptions));
            }

            scans.forEach(scan -> {
                scan.forEachRemaining(key -> {
                    byte[] element = connection.get(key);
                    configs.add(JacksonUtil.fromBytes(element, LwM2MModelConfig.class));
                });
            });
            return configs;
        }
    }

    @Override
    public void put(LwM2MModelConfig modelConfig) {
        byte[] clientSerialized = JacksonUtil.writeValueAsBytes(modelConfig);
        try (var connection = connectionFactory.getConnection()) {
            connection.getSet(getKey(modelConfig.getEndpoint()), clientSerialized);
        }
    }

    @Override
    public void remove(String endpoint) {
        try (var connection = connectionFactory.getConnection()) {
            connection.del(getKey(endpoint));
        }
    }

    private byte[] getKey(String endpoint) {
        return (MODEL_EP + endpoint).getBytes();
    }

}
