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
package org.thingsboard.server.dao.sql.query;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;

@RunWith(SpringRunner.class )
@SpringBootTest(classes = EntityKeyMapping.class)
public class EntityKeyMappingTest {

    @Autowired
    private EntityKeyMapping entityKeyMapping;

    private static final List<String> result = List.of("device1", "device2", "device3");

    @Test
    public void testSplitToList() {
        String value = "device1, device2, device3";
        Assert.assertEquals(entityKeyMapping.getListValuesWithoutQuote(value), result);
    }

    @Test
    public void testReplaceSingleQuote() {
        String value = "'device1', 'device2', 'device3'";
        Assert.assertEquals(entityKeyMapping.getListValuesWithoutQuote(value), result);
    }

    @Test
    public void testReplaceDoubleQuote() {
        String value = "\"device1\", \"device2\", \"device3\"";
        Assert.assertEquals(entityKeyMapping.getListValuesWithoutQuote(value), result);
    }

    @Test
    public void testSplitWithoutSpace() {
        String value = "\"device1\"    ,    \"device2\"    ,    \"device3\"";
        Assert.assertEquals(entityKeyMapping.getListValuesWithoutQuote(value), result);
    }

    @Test
    public void testSaveSpacesBetweenString() {
        String value = "device 1 , device 2  ,         device 3";
        List<String> result = List.of("device 1", "device 2", "device 3");
        Assert.assertEquals(entityKeyMapping.getListValuesWithoutQuote(value), result);
    }

    @Test
    public void testSaveQuoteInString() {
        String value = "device ''1 , device \"\"2  ,         device \"'3";
        List<String> result = List.of("device ''1", "device \"\"2", "device \"'3");
        Assert.assertEquals(entityKeyMapping.getListValuesWithoutQuote(value), result);
    }

    @Test
    public void testNotDeleteQuoteWhenDifferentStyle() {

        String value = "\"device1\", 'device2', \"device3\"";
        List<String> result = List.of("\"device1\"", "'device2'", "\"device3\"");
        Assert.assertEquals(entityKeyMapping.getListValuesWithoutQuote(value), result);

        value = "'device1', \"device2\", \"device3\"";
        result = List.of("'device1'", "\"device2\"", "\"device3\"");
        Assert.assertEquals(entityKeyMapping.getListValuesWithoutQuote(value), result);

        value = "device1, 'device2', \"device3\"";
        result = List.of("device1", "'device2'", "\"device3\"");
        Assert.assertEquals(entityKeyMapping.getListValuesWithoutQuote(value), result);


        value = "'device1', device2, \"device3\"";
        result = List.of("'device1'", "device2", "\"device3\"");
        Assert.assertEquals(entityKeyMapping.getListValuesWithoutQuote(value), result);

        value = "device1, \"device2\", \"device3\"";
        result = List.of("device1", "\"device2\"", "\"device3\"");
        Assert.assertEquals(entityKeyMapping.getListValuesWithoutQuote(value), result);


        value = "\"device1\", device2, \"device3\"";
        result = List.of("\"device1\"", "device2", "\"device3\"");
        Assert.assertEquals(entityKeyMapping.getListValuesWithoutQuote(value), result);
    }
}