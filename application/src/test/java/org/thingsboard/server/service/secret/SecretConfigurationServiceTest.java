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
package org.thingsboard.server.service.secret;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.lang.reflect.Field;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SecretConfigurationServiceTest {

    private static Pattern SECRET_PATTERN;

    @BeforeAll
    public static void setUp() throws Exception {
        // Access the SECRET_PATTERN from DefaultSecretConfigurationService using reflection
        Field patternField = DefaultSecretConfigurationService.class.getDeclaredField("SECRET_PATTERN");
        patternField.setAccessible(true);
        SECRET_PATTERN = (Pattern) patternField.get(null);
    }

    @ParameterizedTest
    @MethodSource("provideSecretPatternTestCases")
    public void testSecretPattern(String input, boolean shouldMatch, String expectedName, String expectedType) {
        Matcher matcher = SECRET_PATTERN.matcher(input);
        assertEquals(shouldMatch, matcher.find(), "Matching failed for input: " + input);

        if (shouldMatch) {
            assertEquals(expectedName, matcher.group(1), "Name extraction failed for input: " + input);
        }
    }

    private static Stream<Arguments> provideSecretPatternTestCases() {
        return Stream.of(
                // Valid placeholders with basic Latin characters
                Arguments.of("${secret:mySecret;type:TEXT}", true, "mySecret", "TEXT"),
                Arguments.of("${secret:api_key;type:TEXT_FILE}", true, "api_key", "TEXT_FILE"),
                Arguments.of("${secret:db_password;type:TEXT}", true, "db_password", "TEXT"),
                Arguments.of("${secret:token with spaces;type:TEXT_FILE}", true, "token with spaces", "TEXT_FILE"),
                Arguments.of("${secret:special!@#$%^&*()_+-=[]|:\"'<>,./?;type:TEXT}", true, "special!@#$%^&*()_+-=[]|:\"'<>,./?", "TEXT"),

                // Valid placeholders with international characters
                Arguments.of("${secret:中文密码;type:TEXT}", true, "中文密码", "TEXT"),
                Arguments.of("${secret:Український_ключ;type:TEXT_FILE}", true, "Український_ключ", "TEXT_FILE"),
                Arguments.of("${secret:日本語のパスワード;type:TEXT}", true, "日本語のパスワード", "TEXT"),
                Arguments.of("${secret:한국어 비밀번호;type:TEXT_FILE}", true, "한국어 비밀번호", "TEXT_FILE"),
                Arguments.of("${secret:كلمة السر العربية;type:TEXT}", true, "كلمة السر العربية", "TEXT"),

                // Invalid placeholders - wrong format
                Arguments.of("${secret:mySecret:type:TEXT}", false, null, null),
                Arguments.of("${secret=mySecret;type=TEXT}", false, null, null),
                Arguments.of("{secret:mySecret;type:TEXT}", false, null, null),
                Arguments.of("$secret:mySecret;type:TEXT}", false, null, null),
                Arguments.of("${secret:mySecret;type:TEXT", false, null, null),

                // Invalid placeholders - prohibited characters
                Arguments.of("${secret:my{Secret;type:TEXT}", false, null, null),
                Arguments.of("${secret:mySecret};type:TEXT}", false, null, null)
        );
    }

}
