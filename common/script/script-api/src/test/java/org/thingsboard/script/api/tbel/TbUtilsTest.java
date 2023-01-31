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
package org.thingsboard.script.api.tbel;

import org.junit.Assert;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class TbUtilsTest {

    @Test
    public void parseHexToInt() {
        Assert.assertEquals(0xAB, TbUtils.parseHexToInt("AB"));
        Assert.assertEquals(0xABBA, TbUtils.parseHexToInt("ABBA", true));
        Assert.assertEquals(0xBAAB, TbUtils.parseHexToInt("ABBA", false));
        Assert.assertEquals(0xAABBCC, TbUtils.parseHexToInt("AABBCC", true));
        Assert.assertEquals(0xAABBCC, TbUtils.parseHexToInt("CCBBAA", false));
        Assert.assertEquals(0xAABBCCDD, TbUtils.parseHexToInt("AABBCCDD", true));
        Assert.assertEquals(0xAABBCCDD, TbUtils.parseHexToInt("DDCCBBAA", false));
        Assert.assertEquals(0xDDCCBBAA, TbUtils.parseHexToInt("DDCCBBAA", true));
        Assert.assertEquals(0xDDCCBBAA, TbUtils.parseHexToInt("AABBCCDD", false));
    }

    @Test
    public void parseBytesToInt_checkPrimitives() {
        int expected = 257;
        byte[] data = ByteBuffer.allocate(4).putInt(expected).array();
        Assert.assertEquals(expected, TbUtils.parseBytesToInt(data, 0, 4));
        Assert.assertEquals(expected, TbUtils.parseBytesToInt(data, 2, 2, true));
        Assert.assertEquals(1, TbUtils.parseBytesToInt(data, 3, 1, true));

        expected = Integer.MAX_VALUE;
        data = ByteBuffer.allocate(4).putInt(expected).array();
        Assert.assertEquals(expected, TbUtils.parseBytesToInt(data, 0, 4, true));

        expected = 0xAABBCCDD;
        data = new byte[]{(byte) 0xAA, (byte) 0xBB, (byte) 0xCC, (byte) 0xDD};
        Assert.assertEquals(expected, TbUtils.parseBytesToInt(data, 0, 4, true));
        data = new byte[]{(byte) 0xDD, (byte) 0xCC, (byte) 0xBB, (byte) 0xAA};
        Assert.assertEquals(expected, TbUtils.parseBytesToInt(data, 0, 4, false));

        expected = 0xAABBCC;
        data = new byte[]{(byte) 0xAA, (byte) 0xBB, (byte) 0xCC};
        Assert.assertEquals(expected, TbUtils.parseBytesToInt(data, 0, 3, true));
        data = new byte[]{(byte) 0xCC, (byte) 0xBB, (byte) 0xAA};
        Assert.assertEquals(expected, TbUtils.parseBytesToInt(data, 0, 3, false));
    }

    @Test
    public void parseBytesToInt_checkLists() {
        int expected = 257;
        List<Byte> data = toList(ByteBuffer.allocate(4).putInt(expected).array());
        Assert.assertEquals(expected, TbUtils.parseBytesToInt(data, 0, 4));
        Assert.assertEquals(expected, TbUtils.parseBytesToInt(data, 2, 2, true));
        Assert.assertEquals(1, TbUtils.parseBytesToInt(data, 3, 1, true));

        expected = Integer.MAX_VALUE;
        data = toList(ByteBuffer.allocate(4).putInt(expected).array());
        Assert.assertEquals(expected, TbUtils.parseBytesToInt(data, 0, 4, true));

        expected = 0xAABBCCDD;
        data = toList(new byte[]{(byte) 0xAA, (byte) 0xBB, (byte) 0xCC, (byte) 0xDD});
        Assert.assertEquals(expected, TbUtils.parseBytesToInt(data, 0, 4, true));
        data = toList(new byte[]{(byte) 0xDD, (byte) 0xCC, (byte) 0xBB, (byte) 0xAA});
        Assert.assertEquals(expected, TbUtils.parseBytesToInt(data, 0, 4, false));

        expected = 0xAABBCC;
        data = toList(new byte[]{(byte) 0xAA, (byte) 0xBB, (byte) 0xCC});
        Assert.assertEquals(expected, TbUtils.parseBytesToInt(data, 0, 3, true));
        data = toList(new byte[]{(byte) 0xCC, (byte) 0xBB, (byte) 0xAA});
        Assert.assertEquals(expected, TbUtils.parseBytesToInt(data, 0, 3, false));
    }

    private static List<Byte> toList(byte[] data) {
        List<Byte> result = new ArrayList<>(data.length);
        for (Byte b : data) {
            result.add(b);
        }
        return result;
    }

}
