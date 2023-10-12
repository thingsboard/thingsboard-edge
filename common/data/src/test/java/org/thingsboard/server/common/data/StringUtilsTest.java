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
package org.thingsboard.server.common.data;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class StringUtilsTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "\000", "\u0000", " \000", " \000 ", "\000 ", "\000\000", "\000 \000",
            "世\000界", "F0929906\000\000\000\000\000\000\000\000\000",
    })
    void testContains0x00_thenTrue(String sample) {
        assertThat(StringUtils.contains0x00(sample)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "  ", "abc", "世界", "\001", "\uD83D\uDC0C"})
    void testContains0x00_thenFalse(String sample) {
        assertThat(StringUtils.contains0x00(sample)).isFalse();
    }

    @Test
    void testTruncate() {
        int maxLength = 5;
        assertThat(StringUtils.truncate(null, maxLength)).isNull();
        assertThat(StringUtils.truncate("", maxLength)).isEmpty();
        assertThat(StringUtils.truncate("123", maxLength)).isEqualTo("123");
        assertThat(StringUtils.truncate("1234567", maxLength)).isEqualTo("12345...[truncated 2 symbols]");
        assertThat(StringUtils.truncate("1234567", 0)).isEqualTo("1234567");
    }

}
