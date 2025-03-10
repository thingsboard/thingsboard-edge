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
package org.thingsboard.server.service.edge.instructions;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.edge.EdgeInstructions;
import org.thingsboard.server.dao.util.DeviceConnectivityUtil;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.install.InstallScripts;

@Service
@Slf4j
@ConditionalOnProperty(prefix = "edges", value = "enabled", havingValue = "true")
@TbCoreComponent
public class DefaultEdgeInstallInstructionsService extends BaseEdgeInstallUpgradeInstructionsService implements EdgeInstallInstructionsService {

    private static final String INSTALL_DIR = "install";

    @Value("${edges.rpc.port}")
    private int rpcPort;

    @Value("${edges.rpc.ssl.enabled}")
    private boolean sslEnabled;

    public DefaultEdgeInstallInstructionsService(InstallScripts installScripts) {
        super(installScripts);
    }

    @Override
    public EdgeInstructions getInstallInstructions(Edge edge, String installationMethod, HttpServletRequest request) {
        return switch (installationMethod.toLowerCase()) {
            case "docker" -> getDockerInstallInstructions(edge, request);
            case "ubuntu", "centos" -> getLinuxInstallInstructions(edge, request, installationMethod.toLowerCase());
            default ->
                    throw new IllegalArgumentException("Unsupported installation method for Edge: " + installationMethod);
        };
    }

    private EdgeInstructions getDockerInstallInstructions(Edge edge, HttpServletRequest request) {
        String dockerInstallInstructions = readFile(resolveFile("docker", "instructions.md"));
        String baseUrl = request.getServerName();

        if (DeviceConnectivityUtil.isLocalhost(baseUrl)) {
            dockerInstallInstructions = dockerInstallInstructions.replace("${EXTRA_HOSTS}", "extra_hosts:\n      - \"host.docker.internal:host-gateway\"\n");
            dockerInstallInstructions = dockerInstallInstructions.replace("${BASE_URL}", "host.docker.internal");
        } else {
            dockerInstallInstructions = dockerInstallInstructions.replace("${EXTRA_HOSTS}", "");
            dockerInstallInstructions = dockerInstallInstructions.replace("${BASE_URL}", baseUrl);
        }
        String edgeVersion = appVersion;
        edgeVersion = edgeVersion.replace("-SNAPSHOT", "");
        edgeVersion = edgeVersion.replace("PE", "EDGEPE");
        dockerInstallInstructions = dockerInstallInstructions.replace("${TB_EDGE_VERSION}", edgeVersion);
        dockerInstallInstructions = replacePlaceholders(dockerInstallInstructions, edge);
        return new EdgeInstructions(dockerInstallInstructions);
    }

    private EdgeInstructions getLinuxInstallInstructions(Edge edge, HttpServletRequest request, String os) {
        String ubuntuInstallInstructions = readFile(resolveFile(os, "instructions.md"));
        ubuntuInstallInstructions = replacePlaceholders(ubuntuInstallInstructions, edge);
        ubuntuInstallInstructions = ubuntuInstallInstructions.replace("${BASE_URL}", request.getServerName());
        String edgeVersion = appVersion.replace("-SNAPSHOT", "");
        edgeVersion = edgeVersion.replace("PE", "pe");
        ubuntuInstallInstructions = ubuntuInstallInstructions.replace("${TB_EDGE_VERSION}", edgeVersion);
        ubuntuInstallInstructions = ubuntuInstallInstructions.replace("${TB_EDGE_TAG}", getTagVersion(edgeVersion).replace("pe", ""));
        return new EdgeInstructions(ubuntuInstallInstructions);
    }

    private String replacePlaceholders(String instructions, Edge edge) {
        instructions = instructions.replace("${CLOUD_ROUTING_KEY}", edge.getRoutingKey());
        instructions = instructions.replace("${CLOUD_ROUTING_SECRET}", edge.getSecret());
        instructions = instructions.replace("${CLOUD_RPC_PORT}", Integer.toString(rpcPort));
        instructions = instructions.replace("${CLOUD_RPC_SSL_ENABLED}", Boolean.toString(sslEnabled));
        return instructions;
    }

    @Override
    protected String getBaseDirName() {
        return INSTALL_DIR;
    }

}
