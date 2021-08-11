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

import lombok.Data;
import org.eclipse.leshan.core.SecurityMode;
import org.eclipse.leshan.core.request.BindingMode;
import org.eclipse.leshan.core.util.Hex;
import org.eclipse.leshan.server.bootstrap.BootstrapConfig;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;

@Data
public class LwM2MBootstrapConfig implements Serializable {
    /*
      interface BootstrapSecurityConfig
        servers: BootstrapServersSecurityConfig,
        bootstrapServer: ServerSecurityConfig,
        lwm2mServer: ServerSecurityConfig
      }
     */
    /** -servers
     *   shortId: number,
     *   lifetime: number,
     *   defaultMinPeriod: number,
     *   notifIfDisabled: boolean,
     *   binding: string
     * */
    LwM2MBootstrapServers servers;

    /** -bootstrapServer, lwm2mServer
     * interface ServerSecurityConfig
     *   host?: string,
     *   port?: number,
     *   isBootstrapServer?: boolean,
     *   securityMode: string,
     *   clientPublicKeyOrId?: string,
     *   clientSecretKey?: string,
     *   serverPublicKey?: string;
     *   clientHoldOffTime?: number,
     *   serverId?: number,
     *   bootstrapServerAccountTimeout: number
     * */
    LwM2MServerBootstrap bootstrapServer;

    LwM2MServerBootstrap lwm2mServer;

    public BootstrapConfig getLwM2MBootstrapConfig() {
        BootstrapConfig configBs = new BootstrapConfig();
        /* Delete old security objects */
        configBs.toDelete.add("/0");
        configBs.toDelete.add("/1");
        /* Server Configuration (object 1) as defined in LWM2M 1.0.x TS. */
        BootstrapConfig.ServerConfig server0 = new BootstrapConfig.ServerConfig();
        server0.shortId = servers.getShortId();
        server0.lifetime = servers.getLifetime();
        server0.defaultMinPeriod = servers.getDefaultMinPeriod();
        server0.notifIfDisabled = servers.isNotifIfDisabled();
        server0.binding = BindingMode.parse(servers.getBinding());
        configBs.servers.put(0, server0);
        /* Security Configuration (object 0) as defined in LWM2M 1.0.x TS. Bootstrap instance = 0 */
        this.bootstrapServer.setBootstrapServerIs(true);
        configBs.security.put(0, setServerSecurity(this.lwm2mServer.getHost(), this.lwm2mServer.getPort(), this.lwm2mServer.getSecurityHost(), this.lwm2mServer.getSecurityPort(), this.bootstrapServer.isBootstrapServerIs(), this.bootstrapServer.getSecurityMode(), this.bootstrapServer.getClientPublicKeyOrId(), this.bootstrapServer.getServerPublicKey(), this.bootstrapServer.getClientSecretKey(), this.bootstrapServer.getServerId()));
        /* Security Configuration (object 0) as defined in LWM2M 1.0.x TS. Server instance = 1 */
        configBs.security.put(1, setServerSecurity(this.lwm2mServer.getHost(), this.lwm2mServer.getPort(), this.lwm2mServer.getSecurityHost(), this.lwm2mServer.getSecurityPort(), this.lwm2mServer.isBootstrapServerIs(), this.lwm2mServer.getSecurityMode(), this.lwm2mServer.getClientPublicKeyOrId(), this.lwm2mServer.getServerPublicKey(), this.lwm2mServer.getClientSecretKey(), this.lwm2mServer.getServerId()));
        return configBs;
    }

    private BootstrapConfig.ServerSecurity setServerSecurity(String host, Integer port, String securityHost, Integer securityPort, boolean bootstrapServer, SecurityMode securityMode, String clientPublicKey, String serverPublicKey, String secretKey, int serverId) {
        BootstrapConfig.ServerSecurity serverSecurity = new BootstrapConfig.ServerSecurity();
        if (securityMode.equals(SecurityMode.NO_SEC)) {
            serverSecurity.uri = "coap://" + host + ":" + Integer.toString(port);
        } else {
            serverSecurity.uri = "coaps://" + securityHost + ":" + Integer.toString(securityPort);
        }
        serverSecurity.bootstrapServer = bootstrapServer;
        serverSecurity.securityMode = securityMode;
        serverSecurity.publicKeyOrId = setPublicKeyOrId(clientPublicKey, securityMode);
        serverSecurity.serverPublicKey = (serverPublicKey != null && !serverPublicKey.isEmpty()) ? Hex.decodeHex(serverPublicKey.toCharArray()) : new byte[]{};
        serverSecurity.secretKey = (secretKey != null && !secretKey.isEmpty()) ? Hex.decodeHex(secretKey.toCharArray()) : new byte[]{};
        serverSecurity.serverId = serverId;
        return serverSecurity;
    }

    private byte[] setPublicKeyOrId(String publicKeyOrIdStr, SecurityMode securityMode) {
        return (publicKeyOrIdStr == null || publicKeyOrIdStr.isEmpty()) ? new byte[]{} :
                SecurityMode.PSK.equals(securityMode) ? publicKeyOrIdStr.getBytes(StandardCharsets.UTF_8) :
                        Hex.decodeHex(publicKeyOrIdStr.toCharArray());
    }
}
