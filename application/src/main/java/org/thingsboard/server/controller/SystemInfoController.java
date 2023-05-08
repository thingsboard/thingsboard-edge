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
package org.thingsboard.server.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.info.BuildProperties;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.DashboardInfo;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.SystemParams;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.permission.MergedUserPermissions;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.common.data.settings.UserSettings;
import org.thingsboard.server.common.data.settings.UserSettingsType;
import org.thingsboard.server.dao.wl.WhiteLabelingService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.model.UserPrincipal;
import org.thingsboard.server.service.sync.vc.EntitiesVersionControlService;
import springfox.documentation.annotations.ApiIgnore;

import javax.annotation.PostConstruct;
import java.util.Collections;
import java.util.stream.Collectors;

@ApiIgnore
@RestController
@TbCoreComponent
@RequestMapping("/api")
@Slf4j
public class SystemInfoController extends BaseController {

    @Value("${security.user_token_access_enabled}")
    private boolean userTokenAccessEnabled;

    @Value("${tbel.enabled:true}")
    private boolean tbelEnabled;

    @Value("${state.persistToTelemetry:false}")
    private boolean persistToTelemetry;

    @Value("${ui.dashboard.max_datapoints_limit}")
    private long maxDatapointsLimit;

    @Autowired(required = false)
    private BuildProperties buildProperties;

    @Autowired
    private EntitiesVersionControlService versionControlService;

    @Autowired
    private WhiteLabelingService whiteLabelingService;

    @PostConstruct
    public void init() {
        JsonNode info = buildInfoObject();
        log.info("System build info: {}", info);
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/system/info", method = RequestMethod.GET)
    @ResponseBody
    public JsonNode getSystemVersionInfo() {
        return buildInfoObject();
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/system/params", method = RequestMethod.GET)
    @ResponseBody
    public SystemParams getSystemParams() throws ThingsboardException {
        SystemParams systemParams = new SystemParams();
        SecurityUser currentUser = getCurrentUser();
        TenantId tenantId = currentUser.getTenantId();
        CustomerId customerId = currentUser.getCustomerId();
        MergedUserPermissions mergedUserPermissions = currentUser.getUserPermissions();
        systemParams.setUserTokenAccessEnabled(userTokenAccessEnabled);
        boolean forceFullscreen = isForceFullscreen(currentUser);
        if (forceFullscreen && (currentUser.isTenantAdmin() || currentUser.isCustomerUser())) {
            PageLink pageLink = new PageLink(100);
            PageData<DashboardInfo> dashboardsPageData = entityService.findUserEntities(tenantId, customerId, mergedUserPermissions, EntityType.DASHBOARD,
                    Operation.READ, null, pageLink, false);
            systemParams.setAllowedDashboardIds(dashboardsPageData.getData().stream().map(d -> d.getId().getId().toString()).collect(Collectors.toList()));
        } else {
            systemParams.setAllowedDashboardIds(Collections.emptyList());
        }
        systemParams.setEdgesSupportEnabled(edgesEnabled);
        if (currentUser.isTenantAdmin()) {
            systemParams.setHasRepository(versionControlService.getVersionControlSettings(tenantId) != null);
            systemParams.setTbelEnabled(tbelEnabled);
        } else {
            systemParams.setHasRepository(false);
            systemParams.setTbelEnabled(false);
        }
        if (currentUser.isTenantAdmin() || currentUser.isCustomerUser()) {
            systemParams.setPersistDeviceStateToTelemetry(persistToTelemetry);
            EntityId entityId;
            if (currentUser.isTenantAdmin()) {
                entityId = tenantId;
            } else {
                entityId = customerId;
            }
            systemParams.setWhiteLabelingAllowed(whiteLabelingService.isWhiteLabelingAllowed(getTenantId(), entityId));
            if (currentUser.isTenantAdmin()) {
                systemParams.setCustomerWhiteLabelingAllowed(whiteLabelingService.isCustomerWhiteLabelingAllowed(tenantId));
            } else {
                systemParams.setCustomerWhiteLabelingAllowed(false);
            }
        } else {
            systemParams.setPersistDeviceStateToTelemetry(false);
            systemParams.setWhiteLabelingAllowed(false);
            systemParams.setCustomerWhiteLabelingAllowed(false);
        }
        UserSettings userSettings = userSettingsService.findUserSettings(currentUser.getTenantId(), currentUser.getId(), UserSettingsType.GENERAL);
        ObjectNode userSettingsNode = userSettings == null ? JacksonUtil.newObjectNode() : (ObjectNode) userSettings.getSettings();
        if (!userSettingsNode.has("openedMenuSections")) {
            userSettingsNode.set("openedMenuSections", JacksonUtil.newArrayNode());
        }
        systemParams.setUserSettings(userSettingsNode);
        systemParams.setMaxDatapointsLimit(maxDatapointsLimit);
        return systemParams;
    }

    private boolean isForceFullscreen(SecurityUser currentUser) {
        return UserPrincipal.Type.PUBLIC_ID.equals(currentUser.getUserPrincipal().getType()) ||
                (currentUser.getAdditionalInfo() != null &&
                        currentUser.getAdditionalInfo().has("defaultDashboardFullscreen") &&
                        currentUser.getAdditionalInfo().get("defaultDashboardFullscreen").booleanValue());
    }

    private JsonNode buildInfoObject() {
        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode infoObject = objectMapper.createObjectNode();
        if (buildProperties != null) {
            infoObject.put("version", buildProperties.getVersion());
            infoObject.put("artifact", buildProperties.getArtifact());
            infoObject.put("name", buildProperties.getName());
        } else {
            infoObject.put("version", "unknown");
        }
        return infoObject;
    }
}
