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
package org.thingsboard.server.transport.lwm2m.bootstrap.secure;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.leshan.core.SecurityMode;
import org.eclipse.leshan.core.util.Hex;
import org.eclipse.leshan.core.util.SecurityUtil;
import org.eclipse.leshan.server.bootstrap.BootstrapConfig;
import org.eclipse.leshan.server.bootstrap.EditableBootstrapConfigStore;
import org.eclipse.leshan.server.bootstrap.InvalidConfigurationException;
import org.eclipse.leshan.server.security.BootstrapSecurityStore;
import org.eclipse.leshan.server.security.SecurityInfo;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.device.data.lwm2m.BootstrapConfiguration;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.transport.lwm2m.secure.LwM2mCredentialsSecurityInfoValidator;
import org.thingsboard.server.transport.lwm2m.secure.TbLwM2MSecurityInfo;
import org.thingsboard.server.transport.lwm2m.server.LwM2mSessionMsgListener;
import org.thingsboard.server.transport.lwm2m.server.LwM2mTransportContext;
import org.thingsboard.server.transport.lwm2m.server.LwM2mTransportServerHelper;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.Iterator;
import java.util.UUID;

import static org.thingsboard.server.transport.lwm2m.server.LwM2mTransportUtil.LOG_LWM2M_ERROR;
import static org.thingsboard.server.transport.lwm2m.server.LwM2mTransportUtil.LOG_LWM2M_INFO;
import static org.thingsboard.server.transport.lwm2m.server.LwM2mTransportUtil.LOG_LWM2M_TELEMETRY;
import static org.thingsboard.server.transport.lwm2m.server.LwM2mTransportUtil.getBootstrapParametersFromThingsboard;
import static org.thingsboard.server.transport.lwm2m.server.uplink.LwM2mTypeServer.BOOTSTRAP;

@Slf4j
@Service("LwM2MBootstrapSecurityStore")
@ConditionalOnExpression("('${service.type:null}'=='tb-transport' && '${transport.lwm2m.enabled:false}'=='true' && '${transport.lwm2m.bootstrap.enable:false}'=='true') || ('${service.type:null}'=='monolith' && '${transport.lwm2m.enabled}'=='true' && '${transport.lwm2m.bootstrap.enable}'=='true')")
public class LwM2MBootstrapSecurityStore implements BootstrapSecurityStore {

    private final EditableBootstrapConfigStore bootstrapConfigStore;

    private final LwM2mCredentialsSecurityInfoValidator lwM2MCredentialsSecurityInfoValidator;

    private final LwM2mTransportContext context;
    private final LwM2mTransportServerHelper helper;

    public LwM2MBootstrapSecurityStore(EditableBootstrapConfigStore bootstrapConfigStore, LwM2mCredentialsSecurityInfoValidator lwM2MCredentialsSecurityInfoValidator, LwM2mTransportContext context, LwM2mTransportServerHelper helper) {
        this.bootstrapConfigStore = bootstrapConfigStore;
        this.lwM2MCredentialsSecurityInfoValidator = lwM2MCredentialsSecurityInfoValidator;
        this.context = context;
        this.helper = helper;
    }

    @Override
    public Iterator<SecurityInfo> getAllByEndpoint(String endPoint) {
        TbLwM2MSecurityInfo store = lwM2MCredentialsSecurityInfoValidator.getEndpointSecurityInfoByCredentialsId(endPoint, BOOTSTRAP);
        if (store.getBootstrapCredentialConfig() != null && store.getSecurityMode() != null) {
            /* add value to store  from BootstrapJson */
            this.setBootstrapConfigScurityInfo(store);
            BootstrapConfig bsConfigNew = store.getBootstrapConfig();
            if (bsConfigNew != null) {
                try {
                    for (String config : bootstrapConfigStore.getAll().keySet()) {
                        if (config.equals(endPoint)) {
                            bootstrapConfigStore.remove(config);
                        }
                    }
                    bootstrapConfigStore.add(endPoint, bsConfigNew);
                } catch (InvalidConfigurationException e) {
                    log.error("", e);
                }
                return store.getSecurityInfo() == null ? null : Collections.singletonList(store.getSecurityInfo()).iterator();
            }
        }
        return null;
    }

    @Override
    public SecurityInfo getByIdentity(String identity) {
        TbLwM2MSecurityInfo store = lwM2MCredentialsSecurityInfoValidator.getEndpointSecurityInfoByCredentialsId(identity, BOOTSTRAP);
        if (store.getBootstrapCredentialConfig() != null && store.getSecurityMode() != null) {
            /* add value to store  from BootstrapJson */
            this.setBootstrapConfigScurityInfo(store);
            BootstrapConfig bsConfig = store.getBootstrapConfig();
            if (bsConfig.security != null) {
                try {
                    bootstrapConfigStore.add(store.getEndpoint(), bsConfig);
                } catch (InvalidConfigurationException e) {
                    log.error("", e);
                }
                return store.getSecurityInfo();
            }
        }
        return null;
    }

    private void setBootstrapConfigScurityInfo(TbLwM2MSecurityInfo store) {
        /* BootstrapConfig */
        LwM2MBootstrapConfig lwM2MBootstrapConfig = this.getParametersBootstrap(store);
        if (lwM2MBootstrapConfig != null) {
            /* Security info */
            switch (lwM2MBootstrapConfig.getBootstrapServer().getSecurityMode()) {
                /* Use RPK only */
                case PSK:
                    store.setSecurityInfo(SecurityInfo.newPreSharedKeyInfo(store.getEndpoint(),
                            lwM2MBootstrapConfig.getBootstrapServer().getClientPublicKeyOrId(),
                            Hex.decodeHex(lwM2MBootstrapConfig.getBootstrapServer().getClientSecretKey().toCharArray())));
                    store.setSecurityMode(SecurityMode.PSK);
                    break;
                case RPK:
                    try {
                        store.setSecurityInfo(SecurityInfo.newRawPublicKeyInfo(store.getEndpoint(),
                                SecurityUtil.publicKey.decode(Hex.decodeHex(lwM2MBootstrapConfig.getBootstrapServer().getClientPublicKeyOrId().toCharArray()))));
                        store.setSecurityMode(SecurityMode.RPK);
                        break;
                    } catch (IOException | GeneralSecurityException e) {
                        log.error("Unable to decode Client public key for [{}]  [{}]", store.getEndpoint(), e.getMessage());
                    }
                case X509:
                    store.setSecurityInfo(SecurityInfo.newX509CertInfo(store.getEndpoint()));
                    store.setSecurityMode(SecurityMode.X509);
                    break;
                case NO_SEC:
                    store.setSecurityMode(SecurityMode.NO_SEC);
                    store.setSecurityInfo(null);
                    break;
                default:
            }
            BootstrapConfig bootstrapConfig = lwM2MBootstrapConfig.getLwM2MBootstrapConfig();
            store.setBootstrapConfig(bootstrapConfig);
        }
    }

    private LwM2MBootstrapConfig getParametersBootstrap(TbLwM2MSecurityInfo store) {
        LwM2MBootstrapConfig lwM2MBootstrapConfig = store.getBootstrapCredentialConfig();
        if (lwM2MBootstrapConfig != null) {
            BootstrapConfiguration bootstrapObject = getBootstrapParametersFromThingsboard(store.getDeviceProfile());
            lwM2MBootstrapConfig.servers = JacksonUtil.fromString(JacksonUtil.toString(bootstrapObject.getServers()), LwM2MBootstrapServers.class);
            LwM2MServerBootstrap profileServerBootstrap = JacksonUtil.fromString(JacksonUtil.toString(bootstrapObject.getBootstrapServer()), LwM2MServerBootstrap.class);
            LwM2MServerBootstrap profileLwm2mServer = JacksonUtil.fromString(JacksonUtil.toString(bootstrapObject.getLwm2mServer()), LwM2MServerBootstrap.class);
            UUID sessionUUiD = UUID.randomUUID();
            TransportProtos.SessionInfoProto sessionInfo = helper.getValidateSessionInfo(store.getMsg(), sessionUUiD.getMostSignificantBits(), sessionUUiD.getLeastSignificantBits());
            context.getTransportService().registerAsyncSession(sessionInfo, new LwM2mSessionMsgListener(null, null, null, sessionInfo, context.getTransportService()));
            if (this.getValidatedSecurityMode(lwM2MBootstrapConfig.bootstrapServer, profileServerBootstrap, lwM2MBootstrapConfig.lwm2mServer, profileLwm2mServer)) {
                lwM2MBootstrapConfig.bootstrapServer = new LwM2MServerBootstrap(lwM2MBootstrapConfig.bootstrapServer, profileServerBootstrap);
                lwM2MBootstrapConfig.lwm2mServer = new LwM2MServerBootstrap(lwM2MBootstrapConfig.lwm2mServer, profileLwm2mServer);
                String logMsg = String.format("%s: getParametersBootstrap: %s Access connect client with bootstrap server.", LOG_LWM2M_INFO, store.getEndpoint());
                helper.sendParametersOnThingsboardTelemetry(helper.getKvStringtoThingsboard(LOG_LWM2M_TELEMETRY, logMsg), sessionInfo);
                return lwM2MBootstrapConfig;
            } else {
                log.error(" [{}] Different values SecurityMode between of client and profile.", store.getEndpoint());
                log.error("{} getParametersBootstrap: [{}] Different values SecurityMode between of client and profile.", LOG_LWM2M_ERROR, store.getEndpoint());
                String logMsg = String.format("%s: getParametersBootstrap: %s Different values SecurityMode between of client and profile.", LOG_LWM2M_ERROR, store.getEndpoint());
                helper.sendParametersOnThingsboardTelemetry(helper.getKvStringtoThingsboard(LOG_LWM2M_TELEMETRY, logMsg), sessionInfo);
                return null;
            }
        }

        log.error("Unable to decode Json or Certificate for [{}]", store.getEndpoint());
        return null;
    }

    /**
     * Bootstrap security have to sync between (bootstrapServer in credential and  bootstrapServer in profile)
     * and (lwm2mServer  in credential and lwm2mServer  in profile
     *
     * @param bootstrapFromCredential - Bootstrap -> Security of bootstrapServer in credential
     * @param profileServerBootstrap  - Bootstrap -> Security of bootstrapServer in profile
     * @param lwm2mFromCredential     - Bootstrap -> Security of lwm2mServer in credential
     * @param profileLwm2mServer      - Bootstrap -> Security of lwm2mServer in profile
     * @return false if not sync between SecurityMode of Bootstrap credential and profile
     */
    private boolean getValidatedSecurityMode(LwM2MServerBootstrap bootstrapFromCredential, LwM2MServerBootstrap profileServerBootstrap, LwM2MServerBootstrap lwm2mFromCredential, LwM2MServerBootstrap profileLwm2mServer) {
        return (bootstrapFromCredential.getSecurityMode().equals(profileServerBootstrap.getSecurityMode()) &&
                lwm2mFromCredential.getSecurityMode().equals(profileLwm2mServer.getSecurityMode()));
    }
}
