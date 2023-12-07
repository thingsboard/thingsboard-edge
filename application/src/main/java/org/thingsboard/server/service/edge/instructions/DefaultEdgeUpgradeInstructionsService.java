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
import org.thingsboard.server.common.data.EdgeUpgradeInfo;
import org.thingsboard.server.common.data.edge.EdgeInstructions;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.install.InstallScripts;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "edges", value = "enabled", havingValue = "true")
@TbCoreComponent
public class DefaultEdgeUpgradeInstructionsService implements EdgeUpgradeInstructionsService {

    private static final Map<String, EdgeUpgradeInfo> upgradeVersionHashMap = new HashMap<>();

    private static final String EDGE_DIR = "edge";
    private static final String INSTRUCTIONS_DIR = "instructions";
    private static final String UPGRADE_DIR = "upgrade";

    private final InstallScripts installScripts;

    @Value("${app.version:unknown}")
    private String appVersion;

    @Override
    public EdgeInstructions getUpgradeInstructions(String edgeVersion, String upgradeMethod) {
        String tbVersion = appVersion.replace("PE", "").replace("-SNAPSHOT", "");
        String currentEdgeVersion = convertEdgeVersionToDocsFormat(edgeVersion);
        switch (upgradeMethod.toLowerCase()) {
            case "docker":
                return getDockerUpgradeInstructions(tbVersion, currentEdgeVersion);
            case "ubuntu":
            case "centos":
                return getLinuxUpgradeInstructions(tbVersion, currentEdgeVersion, upgradeMethod.toLowerCase());
            default:
                throw new IllegalArgumentException("Unsupported upgrade method for Edge: " + upgradeMethod);
        }
    }

    @Override
    public void updateApplicationVersion(String version) {
        appVersion = version;
    }

    @Override
    public void updateInstructionMap(Map<String, EdgeUpgradeInfo> map) {
        for (String key : map.keySet()) {
            upgradeVersionHashMap.put(key, map.get(key));
        }
    }

    private EdgeInstructions getDockerUpgradeInstructions(String tbVersion, String currentEdgeVersion) {
        EdgeUpgradeInfo edgeUpgradeInfo = upgradeVersionHashMap.get(currentEdgeVersion);
        if (edgeUpgradeInfo == null || edgeUpgradeInfo.getNextEdgeVersion() == null || tbVersion.equals(currentEdgeVersion)) {
            return new EdgeInstructions("Edge upgrade instruction for " + currentEdgeVersion + "EDGE is not available.");
        }
        boolean rmUpgradeCompose = false;
        StringBuilder result = new StringBuilder(readFile(resolveFile("docker", "upgrade_preparing.md")));
        while (edgeUpgradeInfo.getNextEdgeVersion() != null || !tbVersion.equals(currentEdgeVersion)) {
            String edgeVersion = edgeUpgradeInfo.getNextEdgeVersion();
            String ubuntuUpgradeInstructions = readFile(resolveFile("docker", "instructions.md"));
            if (edgeUpgradeInfo.isRequiresUpdateDb()) {
                String upgradeDb = readFile(resolveFile("docker", "upgrade_db.md"));
                ubuntuUpgradeInstructions = ubuntuUpgradeInstructions.replace("${UPGRADE_DB}", upgradeDb);
            } else {
                ubuntuUpgradeInstructions = ubuntuUpgradeInstructions.replace("${UPGRADE_DB}", "");
            }
            if (!rmUpgradeCompose) {
                rmUpgradeCompose = true;
                ubuntuUpgradeInstructions = ubuntuUpgradeInstructions.replace("${CLEAR_DOCKER_UPGRADE}", "");
            } else {
                String rmUpgrade = readFile(resolveFile("docker", "upgrade_rm.md"));
                ubuntuUpgradeInstructions = ubuntuUpgradeInstructions.replace("${CLEAR_DOCKER_UPGRADE}", rmUpgrade);
            }
            ubuntuUpgradeInstructions = ubuntuUpgradeInstructions.replace("${TB_EDGE_VERSION}", edgeVersion + "EDGEPE");
            ubuntuUpgradeInstructions = ubuntuUpgradeInstructions.replace("${FROM_TB_EDGE_VERSION}", currentEdgeVersion + "EDGEPE");
            currentEdgeVersion = edgeVersion;
            edgeUpgradeInfo = upgradeVersionHashMap.get(edgeUpgradeInfo.getNextEdgeVersion());
            result.append(ubuntuUpgradeInstructions);
        }
        String startService = readFile(resolveFile("docker", "start_service.md"));
        startService = startService.replace("${TB_EDGE_VERSION}", currentEdgeVersion + "EDGE");
        result.append(startService);
        return new EdgeInstructions(result.toString());
    }

    private EdgeInstructions getLinuxUpgradeInstructions(String tbVersion, String currentEdgeVersion, String os) {
        EdgeUpgradeInfo edgeUpgradeInfo = upgradeVersionHashMap.get(currentEdgeVersion);
        if (edgeUpgradeInfo == null || edgeUpgradeInfo.getNextEdgeVersion() == null || tbVersion.equals(currentEdgeVersion)) {
            return new EdgeInstructions("Edge upgrade instruction for " + currentEdgeVersion + "EDGE is not available.");
        }
        String upgrade_preparing = readFile(resolveFile("upgrade_preparing.md"));
        upgrade_preparing = upgrade_preparing.replace("${OS}", os.equals("centos") ? "RHEL/CentOS 7/8" : "Ubuntu");
        StringBuilder result = new StringBuilder(upgrade_preparing);
        while (edgeUpgradeInfo.getNextEdgeVersion() != null || !tbVersion.equals(currentEdgeVersion)) {
            String edgeVersion = edgeUpgradeInfo.getNextEdgeVersion();
            String ubuntuUpgradeInstructions = readFile(resolveFile(os, "instructions.md"));
            if (edgeUpgradeInfo.isRequiresUpdateDb()) {
                String upgradeDb = readFile(resolveFile("upgrade_db.md"));
                ubuntuUpgradeInstructions = ubuntuUpgradeInstructions.replace("${UPGRADE_DB}", upgradeDb);
            } else {
                ubuntuUpgradeInstructions = ubuntuUpgradeInstructions.replace("${UPGRADE_DB}", "");
            }
            ubuntuUpgradeInstructions = ubuntuUpgradeInstructions.replace("${TB_EDGE_TAG}", getTagVersion(edgeVersion));
            ubuntuUpgradeInstructions = ubuntuUpgradeInstructions.replace("${FROM_TB_EDGE_TAG}", getTagVersion(currentEdgeVersion));
            ubuntuUpgradeInstructions = ubuntuUpgradeInstructions.replace("${TB_EDGE_VERSION}", edgeVersion);
            ubuntuUpgradeInstructions = ubuntuUpgradeInstructions.replace("${FROM_TB_EDGE_VERSION}", currentEdgeVersion);
            currentEdgeVersion = edgeVersion;
            edgeUpgradeInfo = upgradeVersionHashMap.get(edgeUpgradeInfo.getNextEdgeVersion());
            result.append(ubuntuUpgradeInstructions);
        }
        String startService = readFile(resolveFile("start_service.md"));
        result.append(startService);
        return new EdgeInstructions(result.toString());
    }

    private String getTagVersion(String version) {
        return version.endsWith(".0") ? version.substring(0, version.length() - 2) : version;
    }

    private String convertEdgeVersionToDocsFormat(String edgeVersion) {
        return edgeVersion.replace("_", ".").substring(2);
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
        return Paths.get(installScripts.getDataDir(), InstallScripts.JSON_DIR, EDGE_DIR, INSTRUCTIONS_DIR, UPGRADE_DIR);
    }
}
