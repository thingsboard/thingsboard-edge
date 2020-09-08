/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2020 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.transport.mqtt.util;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import javax.script.ScriptException;
import java.util.regex.Pattern;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(MockitoJUnitRunner.class)
public class MqttTopicRegexUtilTest {

    private static String TEST_STR_1 = "Sensor/Temperature/House/48";
    private static String TEST_STR_2 = "Sensor/Temperature";
    private static String TEST_STR_3 = "Sensor/Temperature2/House/48";

    @Test
    public void metadataCanBeUpdated() throws ScriptException {
        Pattern filter = MqttTopicRegexUtil.toRegex("Sensor/Temperature/House/+");
        assertTrue(filter.matcher(TEST_STR_1).matches());
        assertFalse(filter.matcher(TEST_STR_2).matches());

        filter = MqttTopicRegexUtil.toRegex("Sensor/+/House/#");
        assertTrue(filter.matcher(TEST_STR_1).matches());
        assertFalse(filter.matcher(TEST_STR_2).matches());

        filter = MqttTopicRegexUtil.toRegex("Sensor/#");
        assertTrue(filter.matcher(TEST_STR_1).matches());
        assertTrue(filter.matcher(TEST_STR_2).matches());
        assertTrue(filter.matcher(TEST_STR_3).matches());

        filter = MqttTopicRegexUtil.toRegex("Sensor/Temperature/#");
        assertTrue(filter.matcher(TEST_STR_1).matches());
        assertTrue(filter.matcher(TEST_STR_2).matches());
        assertFalse(filter.matcher(TEST_STR_3).matches());
    }

}
