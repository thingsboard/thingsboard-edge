/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2020 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.service.cloud.processor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.AdminSettings;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.id.AdminSettingsId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.BaseAttributeKvEntry;
import org.thingsboard.server.common.data.kv.StringDataEntry;
import org.thingsboard.server.dao.util.mapping.JacksonUtil;
import org.thingsboard.server.gen.edge.MailTemplateSettingsProto;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
public class MailTemplatesUpdateProcessor extends BaseUpdateProcessor {

    private static final String MAIL_TEMPLATES = "mailTemplates";

    public void onMailTemplatesUpdate(TenantId tenantId, MailTemplateSettingsProto mailTemplateSettings) {
        UUID uuid = new UUID(mailTemplateSettings.getIdMSB(), mailTemplateSettings.getIdLSB());
        if (uuid.version() != 0) {
            AdminSettings adminSettings = new AdminSettings();
            adminSettings.setId(new AdminSettingsId(uuid));
            adminSettings.setKey(MAIL_TEMPLATES);
            adminSettings.setJsonValue(JacksonUtil.toJsonNode(mailTemplateSettings.getJsonValue()));
            adminSettingsService.saveAdminSettings(TenantId.SYS_TENANT_ID, adminSettings);
        } else {
            List<AttributeKvEntry> attributes = new ArrayList<>();
            long ts = System.currentTimeMillis();
            attributes.add(new BaseAttributeKvEntry(new StringDataEntry(MAIL_TEMPLATES, mailTemplateSettings.getJsonValue()), ts));
            attributesService.save(tenantId, tenantId, DataConstants.SERVER_SCOPE, attributes);
        }
    }
}
