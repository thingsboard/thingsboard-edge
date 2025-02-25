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
package org.thingsboard.rule.engine.analytics.incoming.state;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class TbCountUniqueIntervalStateTest {

    private TbCountUniqueIntervalState state;
    private static final Gson gson = new Gson();

    @BeforeEach
    public void init() {
        state = new TbCountUniqueIntervalState();
    }

    @ParameterizedTest
    @MethodSource("provideUpdates")
    public void testDoUpdate_toStateJson_doUpdate_toStateJson_correctSize(List<String> updates) {
        doUpdate_toStateJson(updates);
        doUpdate_toStateJson(updates);

        Assertions.assertEquals(3, state.getItems().size());
    }

    private void doUpdate_toStateJson(List<String> updates) {
        for (String update : updates) {
            state.doUpdate(new JsonPrimitive(update));
        }

        String stateJson = state.toStateJson(gson);

        state = new TbCountUniqueIntervalState(JsonParser.parseString(stateJson));
    }

    static Stream<Arguments> provideUpdates() {
        List<String> primitiveUpdates = Arrays.asList("UA", "CA", "DE");
        List<String> primitiveUpdates_badFormat_2 = prepareBadlyFormattedUpdates(primitiveUpdates, 2);
        List<String> primitiveUpdates_badFormat_5 = prepareBadlyFormattedUpdates(primitiveUpdates, 5);
        List<String> primitiveUpdates_badFormat_10 = prepareBadlyFormattedUpdates(primitiveUpdates, 10);
        List<String> jsonUpdates = Arrays.asList("{\"code\":\"UA\", \"country\":\"Ukraine\"}", "{\"code\":\"CA\", \"country\":\"Canada\"}", "{\"code\":\"DE\", \"country\":\"Germany\"}");
        List<String> jsonUpdates_badFormat_2 = prepareBadlyFormattedUpdates(jsonUpdates, 2);
        List<String> jsonUpdates_badFormat_5 = prepareBadlyFormattedUpdates(jsonUpdates, 5);
        List<String> jsonUpdates_badFormat_10 = prepareBadlyFormattedUpdates(jsonUpdates, 10);

        return Stream.of(
                Arguments.of(primitiveUpdates),
                Arguments.of(primitiveUpdates_badFormat_2),
                Arguments.of(primitiveUpdates_badFormat_5),
                Arguments.of(primitiveUpdates_badFormat_10),
                Arguments.of(jsonUpdates),
                Arguments.of(jsonUpdates_badFormat_2),
                Arguments.of(jsonUpdates_badFormat_5),
                Arguments.of(jsonUpdates_badFormat_10)
        );
    }

    private static List<String> prepareBadlyFormattedUpdates(List<String> source, int iterations) {
        Set<String> items = new HashSet<>();
        for (int i = 0; i < iterations; i++) {
            if (items.size() > 0) {
                JsonArray array = new JsonArray();
                items.forEach(item -> array.add(new JsonPrimitive(item)));
                String json = gson.toJson(array);
                JsonParser.parseString(json).getAsJsonArray().forEach(jsonElement -> items.add(jsonElement.toString()));
            }
            for (String s : source) {
                items.add(new JsonPrimitive(s).getAsString());
            }
        }
        return new ArrayList<>(items);
    }
}