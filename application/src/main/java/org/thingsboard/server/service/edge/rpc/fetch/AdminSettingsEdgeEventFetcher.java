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
package org.thingsboard.server.service.edge.rpc.fetch;

import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.AdminSettings;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.Edge;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@AllArgsConstructor
@Slf4j
public class AdminSettingsEdgeEventFetcher extends BasePageableEdgeEventFetcher {

    @Override
    public PageData<EdgeEvent> fetchEdgeEvents(TenantId tenantId, EdgeId edgeId, PageLink pageLink) {
        return null;
    }


//
//    private void syncAdminSettings(TenantId tenantId, Edge edge) {
//        log.trace("[{}] syncAdminSettings [{}]", tenantId, edge.getName());
//        try {
//            AdminSettings systemMailSettings = adminSettingsService.findAdminSettingsByKey(TenantId.SYS_TENANT_ID, "mail");
//            saveEdgeEvent(tenantId, edge.getId(), EdgeEventType.ADMIN_SETTINGS, EdgeEventActionType.UPDATED, null, mapper.valueToTree(systemMailSettings));
//            AdminSettings tenantMailSettings = convertToTenantAdminSettings(systemMailSettings.getKey(), (ObjectNode) systemMailSettings.getJsonValue());
//            saveEdgeEvent(tenantId, edge.getId(), EdgeEventType.ADMIN_SETTINGS, EdgeEventActionType.UPDATED, null, mapper.valueToTree(tenantMailSettings));
//            AdminSettings systemMailTemplates = loadMailTemplates();
//            saveEdgeEvent(tenantId, edge.getId(), EdgeEventType.ADMIN_SETTINGS, EdgeEventActionType.UPDATED, null, mapper.valueToTree(systemMailTemplates));
//            AdminSettings tenantMailTemplates = convertToTenantAdminSettings(systemMailTemplates.getKey(), (ObjectNode) systemMailTemplates.getJsonValue());
//            saveEdgeEvent(tenantId, edge.getId(), EdgeEventType.ADMIN_SETTINGS, EdgeEventActionType.UPDATED, null, mapper.valueToTree(tenantMailTemplates));
//        } catch (Exception e) {
//            log.error("Can't load admin settings", e);
//        }
//    }
//
//    private AdminSettings loadMailTemplates() throws Exception {
//        Map<String, Object> mailTemplates = new HashMap<>();
//        Pattern startPattern = Pattern.compile("<div class=\"content\".*?>");
//        Pattern endPattern = Pattern.compile("<div class=\"footer\".*?>");
//        File[] files = new DefaultResourceLoader().getResource("classpath:/templates/").getFile().listFiles();
//        for (File file : files) {
//            Map<String, String> mailTemplate = new HashMap<>();
//            String name = validateName(file.getName());
//            String stringTemplate = FileUtils.readFileToString(file, StandardCharsets.UTF_8);
//            Matcher start = startPattern.matcher(stringTemplate);
//            Matcher end = endPattern.matcher(stringTemplate);
//            if (start.find() && end.find()) {
//                String body = StringUtils.substringBetween(stringTemplate, start.group(), end.group()).replaceAll("\t", "");
//                String subject = StringUtils.substringBetween(body, "<h2>", "</h2>");
//                mailTemplate.put("subject", subject);
//                mailTemplate.put("body", body);
//                mailTemplates.put(name, mailTemplate);
//            } else {
//                log.error("Can't load mail template from file {}", file.getName());
//            }
//        }
//        AdminSettings adminSettings = new AdminSettings();
//        adminSettings.setId(new AdminSettingsId(Uuids.timeBased()));
//        adminSettings.setKey("mailTemplates");
//        adminSettings.setJsonValue(mapper.convertValue(mailTemplates, JsonNode.class));
//        return adminSettings;
//    }
//
//    private String validateName(String name) throws Exception {
//        StringBuilder nameBuilder = new StringBuilder();
//        name = name.replace(".vm", "");
//        String[] nameParts = name.split("\\.");
//        if (nameParts.length >= 1) {
//            nameBuilder.append(nameParts[0]);
//            for (int i = 1; i < nameParts.length; i++) {
//                String word = WordUtils.capitalize(nameParts[i]);
//                nameBuilder.append(word);
//            }
//            return nameBuilder.toString();
//        } else {
//            throw new Exception("Error during filename validation");
//        }
//    }
//
//    private AdminSettings convertToTenantAdminSettings(String key, ObjectNode jsonValue) {
//        AdminSettings tenantMailSettings = new AdminSettings();
//        jsonValue.put("useSystemMailSettings", true);
//        tenantMailSettings.setJsonValue(jsonValue);
//        tenantMailSettings.setKey(key);
//        return tenantMailSettings;
//    }
}



// PE
//private void syncAdminSettings(TenantId tenantId, Edge edge) {
//    try {
//        List<String> adminSettingsKeys = Arrays.asList("mail", "mailTemplates");
//        for (String key : adminSettingsKeys) {
//            AdminSettings sysAdminMainSettings = adminSettingsService.findAdminSettingsByKey(TenantId.SYS_TENANT_ID, key);
//            saveAdminSettingsEdgeEvent(edge, sysAdminMainSettings);
//            Optional<AttributeKvEntry> tenantMailSettingsAttr = attributesService.find(edge.getTenantId(), edge.getTenantId(), DataConstants.SERVER_SCOPE, key).get();
//            if (tenantMailSettingsAttr.isPresent()) {
//                AdminSettings tenantMailSettings = new AdminSettings();
//                tenantMailSettings.setKey(key);
//                String value = tenantMailSettingsAttr.get().getValueAsString();
//                tenantMailSettings.setJsonValue(mapper.readTree(value));
//                saveAdminSettingsEdgeEvent(edge, tenantMailSettings);
//            }
//        }
//    } catch (Exception e) {
//        log.error("Can't load admin settings", e);
//    }
//}
//
//    private void saveAdminSettingsEdgeEvent(Edge edge, AdminSettings adminSettings) {
//        log.info(String.valueOf(adminSettings));
//        saveEdgeEvent(edge.getTenantId(),
//                edge.getId(),
//                EdgeEventType.ADMIN_SETTINGS,
//                EdgeEventActionType.UPDATED,
//                null,
//                mapper.valueToTree(adminSettings),
//                null);
//    }

//    private AdminSettings convertToTenantAdminSettings(String key, ObjectNode jsonValue) {
//        AdminSettings tenantMailSettings = new AdminSettings();
//        jsonValue.put("useSystemMailSettings", true);
//        tenantMailSettings.setJsonValue(jsonValue);
//        tenantMailSettings.setKey(key);
//        return tenantMailSettings;
//    }
