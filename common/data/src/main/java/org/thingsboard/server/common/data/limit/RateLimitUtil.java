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
package org.thingsboard.server.common.data.limit;

import org.thingsboard.server.common.data.tenant.profile.DefaultTenantProfileConfiguration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class RateLimitUtil {

    public static List<RateLimitEntry> parseConfig(String config) {
        if (config == null || config.isEmpty()) {
            return Collections.emptyList();
        }
        return Arrays.stream(config.split(","))
                .map(RateLimitEntry::parse)
                .toList();
    }

    public static Function<DefaultTenantProfileConfiguration, String> merge(
            Function<DefaultTenantProfileConfiguration, String> configExtractor1,
            Function<DefaultTenantProfileConfiguration, String> configExtractor2) {
        return config -> {
            String config1 = configExtractor1.apply(config);
            String config2 = configExtractor2.apply(config);
            return RateLimitUtil.mergeStrConfigs(config1, config2); // merges the configs
        };
    }

    private static String mergeStrConfigs(String firstConfig, String secondConfig) {
        List<RateLimitEntry> all = new ArrayList<>();
        all.addAll(parseConfig(firstConfig));
        all.addAll(parseConfig(secondConfig));

        Map<Long, Long> merged = new HashMap<>();

        for (RateLimitEntry entry : all) {
            merged.merge(entry.durationSeconds(), entry.capacity(), Long::sum);
        }

        return merged.entrySet().stream()
                .sorted(Map.Entry.comparingByKey()) // optional: sort by duration
                .map(e -> e.getValue() + ":" + e.getKey())
                .collect(Collectors.joining(","));
    }

    public static boolean isValid(String configStr) {
        List<RateLimitEntry> limitedApiEntries = parseConfig(configStr);
        Set<Long> distinctDurations = new HashSet<>();
        for (RateLimitEntry entry : limitedApiEntries) {
            if (!distinctDurations.add(entry.durationSeconds())) {
                return false;
            }
        }
        return true;
    }

    @Deprecated(forRemoval = true, since = "4.1")
    public static String deduplicateByDuration(String configStr) {
        if (configStr == null) {
            return null;
        }
        Set<Long> distinctDurations = new HashSet<>();
        return parseConfig(configStr).stream()
                .filter(entry -> distinctDurations.add(entry.durationSeconds()))
                .map(RateLimitEntry::toString)
                .collect(Collectors.joining(","));
    }

}
