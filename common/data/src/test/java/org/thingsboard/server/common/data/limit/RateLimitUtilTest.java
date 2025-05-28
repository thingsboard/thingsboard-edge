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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.thingsboard.server.common.data.tenant.profile.DefaultTenantProfileConfiguration;

import java.util.List;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimitUtilTest {

    @Test
    @DisplayName("LimitedApiUtil should parse single entry correctly")
    void testParseSingleEntry() {
        List<RateLimitEntry> entries = RateLimitUtil.parseConfig("100:60");

        assertThat(entries).hasSize(1);
        assertThat(entries.get(0).capacity()).isEqualTo(100);
        assertThat(entries.get(0).durationSeconds()).isEqualTo(60);
    }

    @Test
    @DisplayName("LimitedApiUtil should parse multiple entries correctly")
    void testParseMultipleEntries() {
        List<RateLimitEntry> entries = RateLimitUtil.parseConfig("100:60,200:30");

        assertThat(entries).hasSize(2);
        assertThat(entries.get(0).capacity()).isEqualTo(100);
        assertThat(entries.get(0).durationSeconds()).isEqualTo(60);
        assertThat(entries.get(1).capacity()).isEqualTo(200);
        assertThat(entries.get(1).durationSeconds()).isEqualTo(30);
    }

    @Test
    @DisplayName("LimitedApiUtil should return empty list for null or empty config")
    void testParseEmptyConfig() {
        assertThat(RateLimitUtil.parseConfig(null)).isEmpty();
        assertThat(RateLimitUtil.parseConfig("")).isEmpty();
    }

    @Test
    @DisplayName("LimitedApiUtil should merge two configs by summing capacities with same durations")
    void testMergeStrConfigs() {
        Function<DefaultTenantProfileConfiguration, String> extractor1 = cfg -> "100:60,50:30";
        Function<DefaultTenantProfileConfiguration, String> extractor2 = cfg -> "200:60,25:10";

        // Fake config instance (not used directly in lambda logic)
        DefaultTenantProfileConfiguration config = new DefaultTenantProfileConfiguration();

        String result = RateLimitUtil.merge(extractor1, extractor2).apply(config);

        // Should be: 300:60 (100+200), 50:30, 25:10
        assertThat(result).isEqualTo("25:10,50:30,300:60");
    }

    @Test
    @DisplayName("LimitedApiUtil should merge configs when one is empty")
    void testMergeWithEmptyOne() {
        Function<DefaultTenantProfileConfiguration, String> extractor1 = cfg -> "100:60";
        Function<DefaultTenantProfileConfiguration, String> extractor2 = cfg -> "";

        // Fake config instance (not used directly in lambda logic)
        DefaultTenantProfileConfiguration config = new DefaultTenantProfileConfiguration();
        String result = RateLimitUtil.merge(extractor1, extractor2).apply(config);

        assertThat(result).isEqualTo("100:60");
    }

    @Test
    @DisplayName("LimitedApiUtil should merge configs when both have distinct durations")
    void testMergeWithDistinctDurations() {
        Function<DefaultTenantProfileConfiguration, String> extractor1 = cfg -> "100:60";
        Function<DefaultTenantProfileConfiguration, String> extractor2 = cfg -> "200:10";

        // Fake config instance (not used directly in lambda logic)
        DefaultTenantProfileConfiguration config = new DefaultTenantProfileConfiguration();
        String result = RateLimitUtil.merge(extractor1, extractor2).apply(config);

        assertThat(result).isEqualTo("200:10,100:60");
    }

}
