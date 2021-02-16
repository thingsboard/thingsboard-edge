package org.thingsboard.server.common.adaptor; /**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2021 ThingsBoard, Inc. All Rights Reserved.
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

import com.google.gson.JsonParser;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class JsonConverterTest {

    private static final JsonParser JSON_PARSER = new JsonParser();

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
    public void testParseAsDouble() {
        var result = JsonConverter.convertToTelemetry(JSON_PARSER.parse("{\"meterReadingDelta\": 1.1}"), 0L);
        Assert.assertEquals(1.1, result.get(0L).get(0).getDoubleValue().get(), 0.0);
    }

    @Test
    public void testParseAsLong() {
        var result = JsonConverter.convertToTelemetry(JSON_PARSER.parse("{\"meterReadingDelta\": 11}"), 0L);
        Assert.assertEquals(11L, result.get(0L).get(0).getLongValue().get().longValue());
    }

}