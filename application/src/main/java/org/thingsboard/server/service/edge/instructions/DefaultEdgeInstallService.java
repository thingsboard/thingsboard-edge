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
package org.thingsboard.server.service.edge.instructions;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.edge.EdgeInstructions;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.install.InstallScripts;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "edges", value = "enabled", havingValue = "true")
@TbCoreComponent
public class DefaultEdgeInstallService implements EdgeInstallService {

    private static final String EDGE_DIR = "edge";
    private static final String INSTRUCTIONS_DIR = "instructions";
    private static final String INSTALL_DIR = "install";

    private final InstallScripts installScripts;

    @Value("${edges.rpc.port}")
    private int rpcPort;

    @Value("${edges.rpc.ssl.enabled}")
    private boolean sslEnabled;

    @Value("${app.version:unknown}")
    private String appVersion;

    @Override
    public EdgeInstructions getInstallInstructions(Edge edge, String installationMethod, HttpServletRequest request) {
        switch (installationMethod.toLowerCase()) {
            case "docker":
                return getDockerInstallInstructions(edge, request);
            case "ubuntu":
                return getUbuntuInstallInstructions(edge, request);
            case "centos":
                return getCentosInstallInstructions(edge, request);
            default:
                throw new IllegalArgumentException("Unsupported installation method for Edge: " + installationMethod);
        }
    }

    private EdgeInstructions getDockerInstallInstructions(Edge edge, HttpServletRequest request) {
        String dockerInstallInstructions = readFile(resolveFile("docker", "instructions.md"));
        String baseUrl = request.getServerName();
        if (baseUrl.contains("localhost") || baseUrl.contains("127.0.0.1")) {
            String localhostWarning = readFile(resolveFile("docker", "localhost_warning.md"));
            dockerInstallInstructions = dockerInstallInstructions.replace("${LOCALHOST_WARNING}", localhostWarning);
            dockerInstallInstructions = dockerInstallInstructions.replace("${BASE_URL}", "!!!REPLACE_ME_TO_HOST_IP_ADDRESS!!!");
        } else {
            dockerInstallInstructions = dockerInstallInstructions.replace("${LOCALHOST_WARNING}", "");
            dockerInstallInstructions = dockerInstallInstructions.replace("${BASE_URL}", baseUrl);
        }
        String edgeVersion = appVersion;
        edgeVersion = edgeVersion.replace("-SNAPSHOT", "");
        edgeVersion = edgeVersion.replace("PE", "EDGEPE");
        dockerInstallInstructions = dockerInstallInstructions.replace("${TB_EDGE_VERSION}", edgeVersion);
        dockerInstallInstructions = replacePlaceholders(dockerInstallInstructions, edge);
        return new EdgeInstructions(dockerInstallInstructions);
    }

    private EdgeInstructions getUbuntuInstallInstructions(Edge edge, HttpServletRequest request) {
        String ubuntuInstallInstructions = readFile(resolveFile("ubuntu", "instructions.md"));
        ubuntuInstallInstructions = replacePlaceholders(ubuntuInstallInstructions, edge);
        ubuntuInstallInstructions = ubuntuInstallInstructions.replace("${BASE_URL}", request.getServerName());
        String edgeVersion = appVersion.replace("-SNAPSHOT", "");
        edgeVersion = edgeVersion.replace("PE", "pe");
        ubuntuInstallInstructions = ubuntuInstallInstructions.replace("${TB_EDGE_VERSION}", edgeVersion);
        return new EdgeInstructions(ubuntuInstallInstructions);
    }


    private EdgeInstructions getCentosInstallInstructions(Edge edge, HttpServletRequest request) {
        String centosInstallInstructions = readFile(resolveFile("centos", "instructions.md"));
        centosInstallInstructions = replacePlaceholders(centosInstallInstructions, edge);
        centosInstallInstructions = centosInstallInstructions.replace("${BASE_URL}", request.getServerName());
        String edgeVersion = appVersion.replace("-SNAPSHOT", "");
        edgeVersion = edgeVersion.replace("PE", "pe");
        centosInstallInstructions = centosInstallInstructions.replace("${TB_EDGE_VERSION}", edgeVersion);
        return new EdgeInstructions(centosInstallInstructions);
    }

    private String replacePlaceholders(String instructions, Edge edge) {
        instructions = instructions.replace("${CLOUD_ROUTING_KEY}", edge.getRoutingKey());
        instructions = instructions.replace("${CLOUD_ROUTING_SECRET}", edge.getSecret());
        instructions = instructions.replace("${CLOUD_RPC_PORT}", Integer.toString(rpcPort));
        instructions = instructions.replace("${CLOUD_RPC_SSL_ENABLED}", Boolean.toString(sslEnabled));
        return instructions;
    }

    private String readFile(Path file) {
        try {
            return Files.readString(file);
        } catch (IOException e) {
            log.warn("Failed to read file: {}", file, e);
            throw new RuntimeException(e);
        }
    }

    private Path resolveFile(String subDir, String... subDirs) {
        return getEdgeInstallInstructionsDir().resolve(Paths.get(subDir, subDirs));
    }

    private Path getEdgeInstallInstructionsDir() {
        return Paths.get(installScripts.getDataDir(), InstallScripts.JSON_DIR, EDGE_DIR, INSTRUCTIONS_DIR, INSTALL_DIR);
    }
}
