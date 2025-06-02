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
package org.thingsboard.server.common.msg.tools;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.BandwidthBuilder;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.local.LocalBucket;
import io.github.bucket4j.local.LocalBucketBuilder;
import lombok.Getter;
import org.thingsboard.server.common.data.limit.RateLimitEntry;
import org.thingsboard.server.common.data.limit.RateLimitUtil;

import java.time.Duration;
import java.util.List;

/**
 * Created by ashvayka on 22.10.18.
 */
public class TbRateLimits {
    private final LocalBucket bucket;

    @Getter
    private final String configuration;

    public TbRateLimits(String limitsConfiguration) {
        this(limitsConfiguration, false);
    }

    public TbRateLimits(String limitsConfiguration, boolean refillIntervally) {
        List<RateLimitEntry> limitedApiEntries = RateLimitUtil.parseConfig(limitsConfiguration);
        if (limitedApiEntries.isEmpty()) {
            throw new IllegalArgumentException("Failed to parse rate limits configuration: " + limitsConfiguration);
        }
        LocalBucketBuilder localBucket = Bucket.builder();
        for (RateLimitEntry entry : limitedApiEntries) {
            BandwidthBuilder.BandwidthBuilderRefillStage bandwidthBuilder = Bandwidth.builder().capacity(entry.capacity());
            Bandwidth bandwidth = refillIntervally ?
                    bandwidthBuilder.refillIntervally(entry.capacity(), Duration.ofSeconds(entry.durationSeconds())).build() :
                    bandwidthBuilder.refillGreedy(entry.capacity(), Duration.ofSeconds(entry.durationSeconds())).build();
            localBucket.addLimit(bandwidth);
        }
        this.bucket = localBucket.build();
        this.configuration = limitsConfiguration;
    }

    public boolean tryConsume() {
        return bucket.tryConsume(1);
    }

    public boolean tryConsume(long number) {
        return bucket.tryConsume(number);
    }

}
