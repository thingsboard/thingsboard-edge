/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2021 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.transport.lwm2m.config;

import com.google.common.io.Resources;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.leshan.server.model.LwM2mModelProvider;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.ResourceUtils;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyStore;

@Slf4j
@Component
@ConditionalOnExpression("('${service.type:null}'=='tb-transport' && '${transport.lwm2m.enabled:false}'=='true') || '${service.type:null}'=='monolith' || '${service.type:null}'=='tb-core'")
public class LwM2MTransportServerConfig implements LwM2MSecureServerConfig {

    @Getter
    @Setter
    private LwM2mModelProvider modelProvider;

    @Getter
    @Value("${transport.lwm2m.timeout:}")
    private Long timeout;

    @Getter
    @Value("${transport.sessions.report_timeout}")
    private long sessionReportTimeout;

    @Getter
    @Value("${transport.lwm2m.security.recommended_ciphers:}")
    private boolean recommendedCiphers;

    @Getter
    @Value("${transport.lwm2m.security.recommended_supported_groups:}")
    private boolean recommendedSupportedGroups;

    @Getter
    @Value("${transport.lwm2m.downlink_pool_size:}")
    private int downlinkPoolSize;

    @Getter
    @Value("${transport.lwm2m.uplink_pool_size:}")
    private int uplinkPoolSize;

    @Getter
    @Value("${transport.lwm2m.ota_pool_size:}")
    private int otaPoolSize;

    @Getter
    @Value("${transport.lwm2m.clean_period_in_sec:}")
    private int cleanPeriodInSec;

    @Getter
    @Value("${transport.lwm2m.security.key_store_type:}")
    private String keyStoreType;

    @Getter
    @Value("${transport.lwm2m.security.key_store:}")
    private String keyStoreFilePath;

    @Getter
    @Setter
    private KeyStore keyStoreValue;

    @Getter
    @Value("${transport.lwm2m.security.key_store_password:}")
    private String keyStorePassword;

    @Getter
    @Value("${transport.lwm2m.security.root_alias:}")
    private String rootCertificateAlias;

    @Getter
    @Value("${transport.lwm2m.server.id:}")
    private Integer id;

    @Getter
    @Value("${transport.lwm2m.server.bind_address:}")
    private String host;

    @Getter
    @Value("${transport.lwm2m.server.bind_port:}")
    private Integer port;

    @Getter
    @Value("${transport.lwm2m.server.security.bind_address:}")
    private String secureHost;

    @Getter
    @Value("${transport.lwm2m.server.security.bind_port:}")
    private Integer securePort;

    @Getter
    @Value("${transport.lwm2m.server.security.key_alias:}")
    private String certificateAlias;

    @Getter
    @Value("${transport.lwm2m.server.security.key_password:}")
    private String certificatePassword;

    @Getter
    @Value("${transport.lwm2m.log_max_length:}")
    private int logMaxLength;

    @Getter
    @Value("${transport.lwm2m.psm_activity_timer:10000}")
    private long psmActivityTimer;

    @Getter
    @Value("${transport.lwm2m.paging_transmission_window:10000}")
    private long pagingTransmissionWindow;

    @PostConstruct
    public void init() {
        try {
            InputStream keyStoreInputStream = ResourceUtils.getInputStream(this, keyStoreFilePath);
            keyStoreValue = KeyStore.getInstance(keyStoreType);
            keyStoreValue.load(keyStoreInputStream, keyStorePassword == null ? null : keyStorePassword.toCharArray());
        } catch (Exception e) {
            log.info("Unable to lookup LwM2M keystore. Reason: {}, {}", keyStoreFilePath, e.getMessage());
        }
    }



}
