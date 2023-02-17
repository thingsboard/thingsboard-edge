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
package org.thingsboard.server.cache;

import org.thingsboard.server.common.data.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisClusterConfiguration;
import org.springframework.data.redis.connection.RedisNode;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Configuration
@ConditionalOnMissingBean(TbCaffeineCacheConfiguration.class)
@ConditionalOnProperty(prefix = "redis.connection", value = "type", havingValue = "cluster")
public class TBRedisClusterConfiguration extends TBRedisCacheConfiguration {

    private static final String COMMA = ",";
    private static final String COLON = ":";

    @Value("${redis.cluster.nodes:}")
    private String clusterNodes;

    @Value("${redis.cluster.max-redirects:12}")
    private Integer maxRedirects;

    @Value("${redis.cluster.useDefaultPoolConfig:true}")
    private boolean useDefaultPoolConfig;

    @Value("${redis.password:}")
    private String password;

    public JedisConnectionFactory loadFactory() {
        RedisClusterConfiguration clusterConfiguration = new RedisClusterConfiguration();
        clusterConfiguration.setClusterNodes(getNodes(clusterNodes));
        clusterConfiguration.setMaxRedirects(maxRedirects);
        clusterConfiguration.setPassword(password);
        if (useDefaultPoolConfig) {
            return new JedisConnectionFactory(clusterConfiguration);
        } else {
            return new JedisConnectionFactory(clusterConfiguration, buildPoolConfig());
        }
    }

    private List<RedisNode> getNodes(String nodes) {
        List<RedisNode> result;
        if (StringUtils.isBlank(nodes)) {
            result = Collections.emptyList();
        } else {
            result = new ArrayList<>();
            for (String hostPort : nodes.split(COMMA)) {
                String host = hostPort.split(COLON)[0];
                Integer port = Integer.valueOf(hostPort.split(COLON)[1]);
                result.add(new RedisNode(host, port));
            }
        }
        return result;
    }
}