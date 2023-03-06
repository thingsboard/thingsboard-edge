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
package org.thingsboard.server.common.adaptor;

import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;

@RunWith(MockitoJUnitRunner.class)
public class JsonConverterTest {

    private static final JsonParser JSON_PARSER = new JsonParser();

    @Before
    public void before() {
        JsonConverter.setTypeCastEnabled(true);
    }

    @Test
    public void testParseBigDecimalAsLong() {
        var result = JsonConverter.convertToTelemetry(JSON_PARSER.parse("{\"meterReadingDelta\": 1E+1}"), 0L);
        Assert.assertEquals(10L, result.get(0L).get(0).getLongValue().get().longValue());
    }

    @Test
    public void testParseBigDecimalAsDouble() {
        var result = JsonConverter.convertToTelemetry(JSON_PARSER.parse("{\"meterReadingDelta\": 101E-1}"), 0L);
        Assert.assertEquals(10.1, result.get(0L).get(0).getDoubleValue().get(), 0.0);
    }

    @Test
    public void testParseAttributesBigDecimalAsLong() {
        var result = new ArrayList<>(JsonConverter.convertToAttributes(JSON_PARSER.parse("{\"meterReadingDelta\": 1E1}")));
        Assert.assertEquals(10L, result.get(0).getLongValue().get().longValue());
    }

    @Test
    public void testParseAsDoubleWithZero() {
        var result = JsonConverter.convertToTelemetry(JSON_PARSER.parse("{\"meterReadingDelta\": 42.0}"), 0L);
        Assert.assertEquals(42.0, result.get(0L).get(0).getDoubleValue().get(), 0.0);
    }

    @Test
    public void testParseAsDouble() {
        var result = JsonConverter.convertToTelemetry(JSON_PARSER.parse("{\"meterReadingDelta\": 1.1}"), 0L);
        Assert.assertEquals(1.1, result.get(0L).get(0).getDoubleValue().get(), 0.0);
    }

    @Test
    public void testParseAsLong() {
        var result = JsonConverter.convertToTelemetry(JSON_PARSER.parse("{\"meterReadingDelta\": 11}"), 0L);
        Assert.assertEquals(11L, result.get(0L).get(0).getLongValue().get().longValue());
    }

    @Test
    public void testParseBigDecimalAsStringOutOfLongRange() {
        var result = JsonConverter.convertToTelemetry(JSON_PARSER.parse("{\"meterReadingDelta\": 9.9701010061400066E19}"), 0L);
        Assert.assertEquals("99701010061400066000", result.get(0L).get(0).getStrValue().get());
    }

    @Test
    public void testParseBigDecimalAsStringOutOfLongRange2() {
        var result = JsonConverter.convertToTelemetry(JSON_PARSER.parse("{\"meterReadingDelta\": 99701010061400066001}"), 0L);
        Assert.assertEquals("99701010061400066001", result.get(0L).get(0).getStrValue().get());
    }

    @Test
    public void testParseBigDecimalAsStringOutOfLongRange3() {
        var result = JsonConverter.convertToTelemetry(JSON_PARSER.parse("{\"meterReadingDelta\": 1E19}"), 0L);
        Assert.assertEquals("10000000000000000000", result.get(0L).get(0).getStrValue().get());
    }

    @Test(expected = JsonSyntaxException.class)
    public void testParseBigDecimalOutOfLongRangeWithoutParsing() {
        JsonConverter.setTypeCastEnabled(false);
        JsonConverter.convertToTelemetry(JSON_PARSER.parse("{\"meterReadingDelta\": 89701010051400054084}"), 0L);
    }

    @Test(expected = JsonSyntaxException.class)
    public void testParseBigDecimalOutOfLongRangeWithoutParsing2() {
        JsonConverter.setTypeCastEnabled(false);
        JsonConverter.convertToTelemetry(JSON_PARSER.parse("{\"meterReadingDelta\": 9.9701010061400066E19}"), 0L);
    }


}