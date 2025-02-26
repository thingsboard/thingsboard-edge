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
package org.thingsboard.server.transport.lwm2m.attributes;

import org.eclipse.leshan.core.link.LinkParseException;
import org.eclipse.leshan.core.link.attributes.AttributeSet;
import org.eclipse.leshan.core.link.lwm2m.DefaultLwM2mLinkParser;
import org.eclipse.leshan.core.link.lwm2m.LwM2mLink;
import org.eclipse.leshan.core.link.lwm2m.LwM2mLinkParser;
import org.eclipse.leshan.core.link.lwm2m.attributes.LwM2mAttributeSet;
import org.eclipse.leshan.core.link.lwm2m.attributes.LwM2mAttributes;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

public class DefaultLwM2mLinkParserTest {

    private final LwM2mLinkParser parser = new DefaultLwM2mLinkParser();

    @Test
    public void check_invalid_values() throws LinkParseException {
        // first check it's OK with valid value (3/0/11 - "errorCodes")
        LwM2mLink[] parsed = parser.parseLwM2mLinkFromCoreLinkFormat("</3/0/11>;dim=255".getBytes(), null);
        assertEquals(new LwM2mPath(3, 0, 11), parsed[0].getPath());
        AttributeSet attResult = new LwM2mAttributeSet(LwM2mAttributes.create(LwM2mAttributes.DIMENSION, 255l));
        assertEquals(attResult, parsed[0].getAttributes());

        // then check an invalid one
        assertThrowsExactly(LinkParseException.class, () -> {
            // dim should be between 0-255
            parser.parseLwM2mLinkFromCoreLinkFormat("</3/0/11>;dim=256".getBytes(), null);
        });

        // first check it's OK with valid value
        parsed = parser.parseLwM2mLinkFromCoreLinkFormat("</0/1>;ssid=1".getBytes(), null);
        assertEquals(new LwM2mPath(0, 1), parsed[0].getPath());
        attResult = new LwM2mAttributeSet(LwM2mAttributes.create(LwM2mAttributes.SHORT_SERVER_ID, 1l));
        assertEquals(attResult, parsed[0].getAttributes());

        // then check an invalid one
        assertThrowsExactly(LinkParseException.class, () -> {
            // ssid should be between 1-65534
            parser.parseLwM2mLinkFromCoreLinkFormat("</0/1>;ssid=0".getBytes(), null);
        });
    }

    @Test
    public void check_attribute_with_no_value_failed() throws LinkParseException {
        // first check it's OK with value
        LwM2mLink[] parsed = parser.parseLwM2mLinkFromCoreLinkFormat("</3/0/11>;pmin=200".getBytes(), null);
        assertEquals(new LwM2mPath(3, 0, 11), parsed[0].getPath());
        AttributeSet attResult = new LwM2mAttributeSet(LwM2mAttributes.create(LwM2mAttributes.MINIMUM_PERIOD, 200l));
        assertEquals(attResult, parsed[0].getAttributes());

        // then check an invalid one
        assertThrowsExactly(LinkParseException.class, () -> {
            // pmin should be with value
            parser.parseLwM2mLinkFromCoreLinkFormat("</3/0/11>;pmin".getBytes(), null);
        });
    }
}
