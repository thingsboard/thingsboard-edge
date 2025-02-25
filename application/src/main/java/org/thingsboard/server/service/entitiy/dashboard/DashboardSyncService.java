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
package org.thingsboard.server.service.entitiy.dashboard;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.ResourceSubType;
import org.thingsboard.server.common.data.ResourceType;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.dao.resource.ImageService;
import org.thingsboard.server.dao.resource.ResourceService;
import org.thingsboard.server.dao.widget.WidgetsBundleService;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.util.AfterStartUp;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.install.ProjectInfo;
import org.thingsboard.server.service.sync.GitSyncService;
import org.thingsboard.server.service.sync.vc.GitRepository.FileType;
import org.thingsboard.server.service.sync.vc.GitRepository.RepoFile;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

@Service
@TbCoreComponent
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(value = "transport.gateway.dashboard.sync.enabled", havingValue = "true")
public class DashboardSyncService {

    private final GitSyncService gitSyncService;
    private final ResourceService resourceService;
    private final ImageService imageService;
    private final WidgetsBundleService widgetsBundleService;
    private final PartitionService partitionService;
    private final ProjectInfo projectInfo;

    @Value("${transport.gateway.dashboard.sync.repository_url:}")
    private String repoUrl;
    @Value("${transport.gateway.dashboard.sync.branch:}")
    private String branch;
    @Value("${transport.gateway.dashboard.sync.fetch_frequency:24}")
    private int fetchFrequencyHours;

    private static final String REPO_KEY = "gateways-dashboard";
    private static final String GATEWAYS_DASHBOARD_KEY = "gateways_dashboard.json";

    @AfterStartUp(order = AfterStartUp.REGULAR_SERVICE)
    public void init() throws Exception {
        if (StringUtils.isBlank(branch)) {
            branch = "release/" + projectInfo.getProjectVersion();
        }
        gitSyncService.registerSync(REPO_KEY, repoUrl, branch, TimeUnit.HOURS.toMillis(fetchFrequencyHours), this::update);
    }

    private void update() {
        if (!partitionService.isMyPartition(ServiceType.TB_CORE, TenantId.SYS_TENANT_ID, TenantId.SYS_TENANT_ID)) {
            return;
        }

        List<RepoFile> resources = listFiles("resources");
        for (RepoFile resourceFile : resources) {
            byte[] data = getFileContent(resourceFile.path());
            resourceService.createOrUpdateSystemResource(ResourceType.JS_MODULE, ResourceSubType.EXTENSION, resourceFile.name(), data);
        }
        List<RepoFile> images = listFiles("images");
        for (RepoFile imageFile : images) {
            byte[] data = getFileContent(imageFile.path());
            imageService.createOrUpdateSystemImage(imageFile.name(), data);
        }

        Stream<String> widgetsBundles = listFiles("widget_bundles").stream()
                .map(widgetsBundleFile -> new String(getFileContent(widgetsBundleFile.path()), StandardCharsets.UTF_8));
        Stream<String> widgetTypes = listFiles("widget_types").stream()
                .map(widgetTypeFile -> new String(getFileContent(widgetTypeFile.path()), StandardCharsets.UTF_8));
        widgetsBundleService.updateSystemWidgets(widgetsBundles, widgetTypes);

        RepoFile dashboardFile = listFiles("dashboards").get(0);
        resourceService.createOrUpdateSystemResource(ResourceType.DASHBOARD, null, GATEWAYS_DASHBOARD_KEY, getFileContent(dashboardFile.path()));

        log.info("Gateways dashboard sync completed");
    }

    private List<RepoFile> listFiles(String path) {
        return gitSyncService.listFiles(REPO_KEY, path, 1, FileType.FILE);
    }

    private byte[] getFileContent(String path) {
        return gitSyncService.getFileContent(REPO_KEY, path);
    }

}
