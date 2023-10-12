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
package org.thingsboard.server.service.install;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.rule.RuleChainMetaData;
import org.thingsboard.server.dao.dashboard.DashboardService;
import org.thingsboard.server.dao.group.EntityGroupService;
import org.thingsboard.server.dao.oauth2.OAuth2ConfigTemplateService;
import org.thingsboard.server.dao.resource.ResourceService;
import org.thingsboard.server.dao.rule.RuleChainService;
import org.thingsboard.server.dao.service.validator.RuleChainDataValidator;
import org.thingsboard.server.dao.settings.AdminSettingsService;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.dao.usagerecord.ApiLimitService;
import org.thingsboard.server.dao.widget.WidgetTypeService;
import org.thingsboard.server.dao.widget.WidgetsBundleService;
import org.thingsboard.server.dao.wl.WhiteLabelingService;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.willReturn;

@Slf4j
@SpringBootTest(classes = {InstallScripts.class, RuleChainDataValidator.class})
class InstallScriptsTest {

    @MockBean
    RuleChainService ruleChainService;
    @MockBean
    DashboardService dashboardService;
    @MockBean
    WidgetTypeService widgetTypeService;
    @MockBean
    WidgetsBundleService widgetsBundleService;
    @MockBean
    AdminSettingsService adminSettingsService;
    @MockBean
    EntityGroupService entityGroupService;
    @MockBean
    OAuth2ConfigTemplateService oAuth2TemplateService;
    @MockBean
    ResourceService resourceService;
    @MockBean
    WhiteLabelingService whiteLabelingService;
    @SpyBean
    InstallScripts installScripts;

    @MockBean
    TenantService tenantService;
    @MockBean
    ApiLimitService apiLimitService;
    @SpyBean
    RuleChainDataValidator ruleChainValidator;
    TenantId tenantId = TenantId.fromUUID(UUID.fromString("9ef79cdf-37a8-4119-b682-2e7ed4e018da"));

    @BeforeEach
    void setUp() {
        willReturn(true).given(tenantService).tenantExists(tenantId);
        willReturn(true).given(apiLimitService).checkEntitiesLimit(any(), any());
    }

    @Test
    void testDefaultRuleChainsTemplates() throws IOException {
        Path dir = installScripts.getTenantRuleChainsDir();
        installScripts.findRuleChainsFromPath(dir)
                .forEach(this::validateRuleChainTemplate);
    }

    @Test
    void testRootTenantRuleChainTemplate() {
        validateRuleChainTemplate(installScripts.getRootTenantRuleChainFile());
    }

    @Test
    void testDefaultEdgeRuleChainsTemplates() throws IOException {
        Path dir = installScripts.getEdgeRuleChainsDir();
        installScripts.findRuleChainsFromPath(dir)
                .forEach(this::validateRuleChainTemplate);
    }

    @Test
    void testDeviceProfileDefaultRuleChainTemplate() {
        validateRuleChainTemplate(installScripts.getDeviceProfileDefaultRuleChainTemplateFilePath());
    }

    private void validateRuleChainTemplate(Path templateFilePath) {
        log.warn("validateRuleChainTemplate {}", templateFilePath);
        JsonNode ruleChainJson = JacksonUtil.toJsonNode(templateFilePath.toFile());

        RuleChain ruleChain = JacksonUtil.treeToValue(ruleChainJson.get("ruleChain"), RuleChain.class);
        ruleChain.setTenantId(tenantId);
        ruleChainValidator.validate(ruleChain, RuleChain::getTenantId);
        ruleChain.setId(new RuleChainId(UUID.randomUUID()));

        RuleChainMetaData ruleChainMetaData = JacksonUtil.treeToValue(ruleChainJson.get("metadata"), RuleChainMetaData.class);
        ruleChainMetaData.setRuleChainId(ruleChain.getId());
        List<Throwable> throwables = RuleChainDataValidator.validateMetaData(ruleChainMetaData);

        assertThat(throwables).as("templateFilePath " + templateFilePath)
                .containsExactlyInAnyOrderElementsOf(Collections.emptyList());
    }

}
