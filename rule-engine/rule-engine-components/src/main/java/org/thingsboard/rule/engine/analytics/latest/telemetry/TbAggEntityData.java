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
package org.thingsboard.rule.engine.analytics.latest.telemetry;

import com.google.common.util.concurrent.ListenableFuture;
import lombok.Data;
import lombok.SneakyThrows;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.KvEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.thingsboard.server.common.data.DataConstants.CLIENT_SCOPE;
import static org.thingsboard.server.common.data.DataConstants.SERVER_SCOPE;
import static org.thingsboard.server.common.data.DataConstants.SHARED_SCOPE;

@Data
public class TbAggEntityData {

    private final EntityId entityId;
    private final Map<String, AttributeKvEntry> clientAttributes = new HashMap<>();
    private final Map<String, AttributeKvEntry> serverAttributes = new HashMap<>();
    private final Map<String, AttributeKvEntry> sharedAttributes = new HashMap<>();
    private final Map<String, TsKvEntry> latestTs = new HashMap<>();
    private final Map<String, KvEntry> filterMap = new HashMap<>();

    private volatile ListenableFuture<List<TsKvEntry>> tsFuture;
    private volatile ListenableFuture<List<AttributeKvEntry>> clientAttributesFuture;
    private volatile ListenableFuture<List<AttributeKvEntry>> sharedAttributesFuture;
    private volatile ListenableFuture<List<AttributeKvEntry>> serverAttributesFuture;

    public void prepare() {
        putToMap(latestTs, tsFuture);
        putToMap(clientAttributes, clientAttributesFuture);
        putToMap(serverAttributes, serverAttributesFuture);
        putToMap(sharedAttributes, sharedAttributesFuture);
        putToMap(filterMap, clientAttributesFuture, null);
        putToMap(filterMap, sharedAttributesFuture, null);
        putToMap(filterMap, serverAttributesFuture, null);
        putToMap(filterMap, tsFuture, null);
        putToMap(filterMap, clientAttributesFuture, "cs_");
        putToMap(filterMap, sharedAttributesFuture, "shared_");
        putToMap(filterMap, serverAttributesFuture, "ss_");
    }

    @SneakyThrows
    private static <T extends KvEntry> void putToMap(Map<String, T> map, ListenableFuture<List<T>> future) {
        if (future == null) {
            return;
        }
        List<T> kvEntries = future.get();
        if (kvEntries != null) {
            kvEntries.forEach(e -> map.put(e.getKey(), e));
        }
    }

    @SneakyThrows
    private static <T extends KvEntry> void putToMap(Map<String, KvEntry> map, ListenableFuture<List<T>> future, String prefix) {
        if (future == null) {
            return;
        }
        List<T> kvEntries = future.get();
        if (kvEntries != null) {
            kvEntries.forEach(e -> map.put(StringUtils.isNotBlank(prefix) ? prefix + e.getKey() : e.getKey(), e));
        }
    }

    public TsKvEntry getLatestTs(String source) {
        return latestTs.get(source);
    }

    public Optional<KvEntry> getValue(String sourceScope, String source) {
        KvEntry dataPoint;
        switch (sourceScope) {
            case "LATEST_TELEMETRY":
                dataPoint = latestTs.get(source);
                break;
            case DataConstants.CLIENT_SCOPE:
                dataPoint = clientAttributes.get(source);
                break;
            case DataConstants.SHARED_SCOPE:
                dataPoint = sharedAttributes.get(source);
                break;
            case DataConstants.SERVER_SCOPE:
                dataPoint = serverAttributes.get(source);
                break;
            default:
                dataPoint = null;
        }
        return Optional.ofNullable(dataPoint);
    }
}
