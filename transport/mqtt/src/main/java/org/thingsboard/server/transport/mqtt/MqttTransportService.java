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
package org.thingsboard.server.transport.mqtt;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.ResourceLeakDetector;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.transport.SessionMsgProcessor;
import org.thingsboard.server.common.transport.auth.DeviceAuthService;
import org.thingsboard.server.common.transport.quota.host.HostRequestsQuotaService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.relation.RelationService;
import org.thingsboard.server.transport.mqtt.adaptors.MqttTransportAdaptor;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

/**
 * @author Andrew Shvayka
 */
@Service("MqttTransportService")
@ConditionalOnProperty(prefix = "mqtt", value = "enabled", havingValue = "true", matchIfMissing = false)
@Slf4j
public class MqttTransportService {

    private static final String V1 = "v1";
    private static final String DEVICE = "device";

    @Autowired(required = false)
    private ApplicationContext appContext;

    @Autowired(required = false)
    private SessionMsgProcessor processor;

    @Autowired(required = false)
    private DeviceService deviceService;

    @Autowired(required = false)
    private DeviceAuthService authService;

    @Autowired(required = false)
    private RelationService relationService;

    @Autowired(required = false)
    private MqttSslHandlerProvider sslHandlerProvider;

    @Autowired(required = false)
    private HostRequestsQuotaService quotaService;

    @Value("${mqtt.bind_address}")
    private String host;
    @Value("${mqtt.bind_port}")
    private Integer port;
    @Value("${mqtt.adaptor}")
    private String adaptorName;

    @Value("${mqtt.netty.leak_detector_level}")
    private String leakDetectorLevel;
    @Value("${mqtt.netty.boss_group_thread_count}")
    private Integer bossGroupThreadCount;
    @Value("${mqtt.netty.worker_group_thread_count}")
    private Integer workerGroupThreadCount;
    @Value("${mqtt.netty.max_payload_size}")
    private Integer maxPayloadSize;

    private MqttTransportAdaptor adaptor;

    private Channel serverChannel;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;

    @PostConstruct
    public void init() throws Exception {
        log.info("Setting resource leak detector level to {}", leakDetectorLevel);
        ResourceLeakDetector.setLevel(ResourceLeakDetector.Level.valueOf(leakDetectorLevel.toUpperCase()));

        log.info("Starting MQTT transport...");
        log.info("Lookup MQTT transport adaptor {}", adaptorName);
        this.adaptor = (MqttTransportAdaptor) appContext.getBean(adaptorName);

        log.info("Starting MQTT transport server");
        bossGroup = new NioEventLoopGroup(bossGroupThreadCount);
        workerGroup = new NioEventLoopGroup(workerGroupThreadCount);
        ServerBootstrap b = new ServerBootstrap();
        b.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new MqttTransportServerInitializer(processor, deviceService, authService, relationService,
                        adaptor, sslHandlerProvider, quotaService, maxPayloadSize));

        serverChannel = b.bind(host, port).sync().channel();
        log.info("Mqtt transport started!");
    }

    @PreDestroy
    public void shutdown() throws InterruptedException {
        log.info("Stopping MQTT transport!");
        try {
            serverChannel.close().sync();
        } finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
        log.info("MQTT transport stopped!");
    }
}
