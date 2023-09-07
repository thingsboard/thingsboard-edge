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
package org.thingsboard.integration.mqtt;

import io.netty.buffer.Unpooled;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.thingsboard.integration.api.data.UplinkContentType;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = BasicMqttIntegrationMsgTest.class)
public class BasicMqttIntegrationMsgTest {

    @Test
    public void testBasicMqttIntegrationMsgContentType() {
        byte[] jsonPayload = "{\"testKey\": \"testValue\"}".getBytes();
        BasicMqttIntegrationMsg message = createMessageWithPayload(jsonPayload);
        Assert.assertEquals(UplinkContentType.JSON, message.getContentType());

        byte[] textPayload = "{\"testKey\":\"testValue\"".getBytes();
        message = createMessageWithPayload(textPayload);
        Assert.assertEquals(UplinkContentType.TEXT, message.getContentType());

        byte[] binaryPayload = {0x01, 0x02, 0x03};
        message = createMessageWithPayload(binaryPayload);
        Assert.assertEquals(UplinkContentType.BINARY, message.getContentType());

        byte[] binaryPayloadButExpectedToGetTextType = {0x64, 0x65, 0x66};
        message = createMessageWithPayload(binaryPayloadButExpectedToGetTextType);
        Assert.assertEquals(UplinkContentType.TEXT, message.getContentType());
    }

    private static BasicMqttIntegrationMsg createMessageWithPayload(byte[] payload) {
        return new BasicMqttIntegrationMsg("test", Unpooled.wrappedBuffer(payload));
    }
}
