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
package org.thingsboard.server.queue.util;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PropertyUtilsTest {

    @Test
    void givenNullOrEmpty_whenGetConfig_thenEmptyMap() {
        assertThat(PropertyUtils.getProps(null)).as("null property").isEmpty();
        assertThat(PropertyUtils.getProps("")).as("empty property").isEmpty();
        assertThat(PropertyUtils.getProps(";")).as("ends with ;").isEmpty();
    }

    @Test
    void givenKafkaOtherProperties_whenGetConfig_thenReturnMappedValues() {
        assertThat(PropertyUtils.getProps("metrics.recording.level:INFO;metrics.sample.window.ms:30000"))
                .as("two pairs")
                .isEqualTo(Map.of(
                        "metrics.recording.level", "INFO",
                        "metrics.sample.window.ms", "30000"
                ));

        assertThat(PropertyUtils.getProps("metrics.recording.level:INFO;metrics.sample.window.ms:30000" + ";"))
                .as("two pairs ends with ;")
                .isEqualTo(Map.of(
                        "metrics.recording.level", "INFO",
                        "metrics.sample.window.ms", "30000"
                ));
    }

    @Test
    void givenKafkaTopicProperties_whenGetConfig_thenReturnMappedValues() {
        assertThat(PropertyUtils.getProps("retention.ms:604800000;segment.bytes:26214400;retention.bytes:1048576000;partitions:1;min.insync.replicas:1"))
                .isEqualTo(Map.of(
                        "retention.ms", "604800000",
                        "segment.bytes", "26214400",
                        "retention.bytes", "1048576000",
                        "partitions", "1",
                        "min.insync.replicas", "1"
                ));
    }

}
