/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2019 ThingsBoard, Inc. All Rights Reserved.
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

import com.datastax.driver.core.utils.UUIDs;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Random;
import java.util.UUID;

/**
 * Created by ashvayka on 14.07.17.
 */
@RunWith(MockitoJUnitRunner.class)
public class UUIDConverterTest {

    @Test
    public void basicUuidToStringTest() {
        UUID original = UUID.fromString("58e0a7d7-eebc-11d8-9669-0800200c9a66");
        String result = UUIDConverter.fromTimeUUID(original);
        Assert.assertEquals("1d8eebc58e0a7d796690800200c9a66", result);
    }

    @Test
    public void basicStringToUUIDTest() {
        UUID result = UUIDConverter.fromString("1d8eebc58e0a7d796690800200c9a66");
        Assert.assertEquals(UUID.fromString("58e0a7d7-eebc-11d8-9669-0800200c9a66"), result);
    }

    @Test(expected = IllegalArgumentException.class)
    public void nonV1UuidToStringTest() {
        UUIDConverter.fromTimeUUID(UUID.fromString("58e0a7d7-eebc-01d8-9669-0800200c9a66"));
    }

    @Test
    public void basicUuidComperisonTest() {
        Random r = new Random(System.currentTimeMillis());
        for (int i = 0; i < 100000; i++) {
            long ts = System.currentTimeMillis() + 1000 * 60 * 60 * 24 * 365 * 10;
            long before = (long) (Math.random() * ts);
            long after = (long) (Math.random() * ts);
            if (before > after) {
                long tmp = after;
                after = before;
                before = tmp;
            }

            String beforeStr = UUIDConverter.fromTimeUUID(UUIDs.startOf(before));
            String afterStr = UUIDConverter.fromTimeUUID(UUIDs.startOf(after));

            if (afterStr.compareTo(beforeStr) < 0) {
                System.out.println("Before: " + before + " | " + beforeStr);
                System.out.println("After: " + after + " | " + afterStr);
            }
            Assert.assertTrue(afterStr.compareTo(beforeStr) >= 0);
        }
    }


}
