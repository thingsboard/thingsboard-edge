/**
 * Thingsboard OÜ ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2017 Thingsboard OÜ. All Rights Reserved.
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
package org.thingsboard.server.dao.cache;

import com.hazelcast.config.*;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.instance.GroupProperty;
import com.hazelcast.spring.cache.HazelcastCacheManager;
import com.hazelcast.zookeeper.ZookeeperDiscoveryProperties;
import com.hazelcast.zookeeper.ZookeeperDiscoveryStrategyFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.thingsboard.server.common.data.CacheConstants;

@Configuration
@EnableCaching
@ConditionalOnProperty(prefix = "cache", value = "enabled", havingValue = "true")
public class ServiceCacheConfiguration {

    private static final String HAZELCAST_CLUSTER_NAME = "hazelcast";

    @Value("${cache.device_credentials.max_size.size}")
    private Integer cacheDeviceCredentialsMaxSizeSize;
    @Value("${cache.device_credentials.max_size.policy}")
    private String cacheDeviceCredentialsMaxSizePolicy;
    @Value("${cache.device_credentials.time_to_live}")
    private Integer cacheDeviceCredentialsTTL;

    @Value("${zk.enabled}")
    private boolean zkEnabled;
    @Value("${zk.url}")
    private String zkUrl;
    @Value("${zk.zk_dir}")
    private String zkDir;

    @Bean
    public HazelcastInstance hazelcastInstance() {
        Config config = new Config();

        if (zkEnabled) {
            addZkConfig(config);
        }

        config.addMapConfig(createDeviceCredentialsCacheConfig());

        return Hazelcast.newHazelcastInstance(config);
    }

    private void addZkConfig(Config config) {
        config.getNetworkConfig().getJoin().getMulticastConfig().setEnabled(false);
        config.setProperty(GroupProperty.DISCOVERY_SPI_ENABLED.getName(), Boolean.TRUE.toString());
        DiscoveryStrategyConfig discoveryStrategyConfig = new DiscoveryStrategyConfig(new ZookeeperDiscoveryStrategyFactory());
        discoveryStrategyConfig.addProperty(ZookeeperDiscoveryProperties.ZOOKEEPER_URL.key(), zkUrl);
        discoveryStrategyConfig.addProperty(ZookeeperDiscoveryProperties.ZOOKEEPER_PATH.key(), zkDir);
        discoveryStrategyConfig.addProperty(ZookeeperDiscoveryProperties.GROUP.key(), HAZELCAST_CLUSTER_NAME);
        config.getNetworkConfig().getJoin().getDiscoveryConfig().addDiscoveryStrategyConfig(discoveryStrategyConfig);
    }

    private MapConfig createDeviceCredentialsCacheConfig() {
        MapConfig deviceCredentialsCacheConfig = new MapConfig(CacheConstants.DEVICE_CREDENTIALS_CACHE);
        deviceCredentialsCacheConfig.setTimeToLiveSeconds(cacheDeviceCredentialsTTL);
        deviceCredentialsCacheConfig.setEvictionPolicy(EvictionPolicy.LRU);
        deviceCredentialsCacheConfig.setMaxSizeConfig(
                new MaxSizeConfig(
                        cacheDeviceCredentialsMaxSizeSize,
                        MaxSizeConfig.MaxSizePolicy.valueOf(cacheDeviceCredentialsMaxSizePolicy))
        );
        return deviceCredentialsCacheConfig;
    }

    @Bean
    public KeyGenerator previousDeviceCredentialsId() {
        return new PreviousDeviceCredentialsIdKeyGenerator();
    }

    @Bean
    public CacheManager cacheManager() {
        return new HazelcastCacheManager(hazelcastInstance());
    }
}
