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
package org.thingsboard.client.tools.migrator;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.EntityType;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class RelatedEntitiesParser {
    private final Map<String, String> allEntityIdsAndTypes = new HashMap<>();
    
    private final Map<String, EntityType> tableNameAndEntityType = Map.ofEntries(
            Map.entry("COPY public.alarm ", EntityType.ALARM),
            Map.entry("COPY public.asset ", EntityType.ASSET),
            Map.entry("COPY public.customer ", EntityType.CUSTOMER),
            Map.entry("COPY public.dashboard ", EntityType.DASHBOARD),
            Map.entry("COPY public.device ", EntityType.DEVICE),
            Map.entry("COPY public.rule_chain ", EntityType.RULE_CHAIN),
            Map.entry("COPY public.rule_node ", EntityType.RULE_NODE),
            Map.entry("COPY public.tenant ", EntityType.TENANT),
            Map.entry("COPY public.tb_user ", EntityType.USER),
            Map.entry("COPY public.entity_view ", EntityType.ENTITY_VIEW),
            Map.entry("COPY public.widgets_bundle ", EntityType.WIDGETS_BUNDLE),
            Map.entry("COPY public.widget_type ", EntityType.WIDGET_TYPE),
            Map.entry("COPY public.tenant_profile ", EntityType.TENANT_PROFILE),
            Map.entry("COPY public.device_profile ", EntityType.DEVICE_PROFILE),
            Map.entry("COPY public.asset_profile ", EntityType.ASSET_PROFILE),
            Map.entry("COPY public.api_usage_state ", EntityType.API_USAGE_STATE)
    );

    public RelatedEntitiesParser(File source) throws IOException {
        processAllTables(FileUtils.lineIterator(source));
    }

    public String getEntityType(String uuid) {
        return this.allEntityIdsAndTypes.get(uuid);
    }

    private boolean isBlockFinished(String line) {
        return StringUtils.isBlank(line) || line.equals("\\.");
    }

    private void processAllTables(LineIterator lineIterator) throws IOException {
        String currentLine;
        try {
            while (lineIterator.hasNext()) {
                currentLine = lineIterator.nextLine();
                for(Map.Entry<String, EntityType> entry : tableNameAndEntityType.entrySet()) {
                    if(currentLine.startsWith(entry.getKey())) {
                        processBlock(lineIterator, entry.getValue());
                    }
                }
            }
        } finally {
            lineIterator.close();
        }
    }

    private void processBlock(LineIterator lineIterator, EntityType entityType) {
        String currentLine;
        while(lineIterator.hasNext()) {
            currentLine = lineIterator.nextLine();
            if(isBlockFinished(currentLine)) {
                return;
            }
            allEntityIdsAndTypes.put(currentLine.split("\t")[0], entityType.name());
        }
    }
}
