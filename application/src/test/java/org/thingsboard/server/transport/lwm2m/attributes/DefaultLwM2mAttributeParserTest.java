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
import org.eclipse.leshan.core.link.attributes.InvalidAttributeException;
import org.eclipse.leshan.core.link.lwm2m.attributes.DefaultLwM2mAttributeParser;
import org.eclipse.leshan.core.link.lwm2m.attributes.LwM2mAttribute;
import org.eclipse.leshan.core.link.lwm2m.attributes.LwM2mAttributeSet;
import org.eclipse.leshan.core.link.lwm2m.attributes.LwM2mAttributes;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class DefaultLwM2mAttributeParserTest {

    private final DefaultLwM2mAttributeParser parser = new DefaultLwM2mAttributeParser();

    private static Stream<Arguments> validAttributes() throws InvalidAttributeException {
        return Stream.of(//
                Arguments.of("pmin", LwM2mAttributes.create(LwM2mAttributes.MINIMUM_PERIOD)), //
                Arguments.of("pmin=30", LwM2mAttributes.create(LwM2mAttributes.MINIMUM_PERIOD, 30l)), //
                Arguments.of("pmax", LwM2mAttributes.create(LwM2mAttributes.MAXIMUM_PERIOD)), //
                Arguments.of("pmax=60", LwM2mAttributes.create(LwM2mAttributes.MAXIMUM_PERIOD, 60l)), //
                Arguments.of("dim=2", LwM2mAttributes.create(LwM2mAttributes.DIMENSION, 2l)), //
                Arguments.of("epmin", LwM2mAttributes.create(LwM2mAttributes.EVALUATE_MINIMUM_PERIOD)), //
                Arguments.of("epmin=30", LwM2mAttributes.create(LwM2mAttributes.EVALUATE_MINIMUM_PERIOD, 30l)), //
                Arguments.of("epmax", LwM2mAttributes.create(LwM2mAttributes.EVALUATE_MAXIMUM_PERIOD)), //
                Arguments.of("epmax=60", LwM2mAttributes.create(LwM2mAttributes.EVALUATE_MAXIMUM_PERIOD, 60l)), //
                Arguments.of("lt", LwM2mAttributes.create(LwM2mAttributes.LESSER_THAN)), //
                Arguments.of("lt=30", LwM2mAttributes.create(LwM2mAttributes.LESSER_THAN, 30d)), //
                Arguments.of("lt=-30", LwM2mAttributes.create(LwM2mAttributes.LESSER_THAN, -30d)), //
                Arguments.of("lt=30.55", LwM2mAttributes.create(LwM2mAttributes.LESSER_THAN, 30.55)), //
                Arguments.of("lt=-30.55", LwM2mAttributes.create(LwM2mAttributes.LESSER_THAN, -30.55)), //
                Arguments.of("gt", LwM2mAttributes.create(LwM2mAttributes.GREATER_THAN)), //
                Arguments.of("gt=60", LwM2mAttributes.create(LwM2mAttributes.GREATER_THAN, 60d)), //
                Arguments.of("gt=-60", LwM2mAttributes.create(LwM2mAttributes.GREATER_THAN, -60d)), //
                Arguments.of("gt=60.55", LwM2mAttributes.create(LwM2mAttributes.GREATER_THAN, 60.55)), //
                Arguments.of("gt=-60.55", LwM2mAttributes.create(LwM2mAttributes.GREATER_THAN, -60.55)), //
                Arguments.of("st", LwM2mAttributes.create(LwM2mAttributes.STEP)), //
                Arguments.of("st=60", LwM2mAttributes.create(LwM2mAttributes.STEP, 60d)), //
                Arguments.of("st=60.55", LwM2mAttributes.create(LwM2mAttributes.STEP, 60.55)) //
        );
    }

    @ParameterizedTest(name = "Tests {index} : {0}")
    @MethodSource("validAttributes")
    public void check_one_valid_attribute(String attributeAsString, LwM2mAttribute<?> expectedValue)
            throws LinkParseException, InvalidAttributeException {
        LwM2mAttributeSet parsed = new LwM2mAttributeSet(parser.parseUriQuery(attributeAsString));
        LwM2mAttributeSet attResult = new LwM2mAttributeSet(expectedValue);
        assertEquals(parsed, attResult);
    }

    private static String[] invalidAttributes() {
        return new String[] { //
                // MINIMUM_PERIOD
                "pmin=", //
                "pmin=1.9", //
                "pmin=-1.8", //
                "pmin=a", //
                // MAXIMUM_PERIOD
                "pmax=", //
                "pmax=-2", //
                "pmax=2.7", //
                "pmax=-2.6", //
                "pmax=bc", //
                // DIMENSION
                "dim", //
                "dim=", //
                "dim=-3", //
                "dim=3.5", //
                "dim=-3.4", //
                "dim=def", //
                // EVALUATE_MINIMUM_PERIOD
                "epmin=", //
                "epmin=-4", //
                "epmin=4.3", //
                "epmin=-4.2", //
                "epmin=ghij", //
                // EVALUATE_MAXIMUM_PERIOD
                "epmax=", //
                "epmax=-5", //
                "epmax=5.1", //
                "epmax=-5.9", //
                "epmax=klmno", //
                // LESSER_THAN
                "lt=", //
                "lt=pqrts", //
                "lt=0abc", //
                // GREATER_THAN
                "gt=", //
                "gt=uvwxyz", //
                "gt=0.xyz", //
                // STEP
                "st=", //
                "st=-6", //
                "st=-6.8", //
                // Multi-attributes
                "&pmin&pmax=60&gt=2&", //
                "&pmin&pmax=60&gt=2", //
                "pmin&pmax=60&gt=2&", //
                "pmin&pmax=60&&gt=2", //
                "pmin&pmax=60&&gt=2&&&", //
                "&&&pmin&pmax=60&&gt=2", //
        };
    }

    @ParameterizedTest(name = "Test {index} : {0}")
    @MethodSource("invalidAttributes")
    public void check_invalid_attributes(String invalidAttributesAsString) {
        assertThrows(InvalidAttributeException.class,
                () -> new LwM2mAttributeSet(parser.parseUriQuery(invalidAttributesAsString)));

    }

    @Test
    public void check_multiple_valid_attributes() throws LinkParseException, InvalidAttributeException {

        LwM2mAttributeSet parsed = new LwM2mAttributeSet(parser.parseUriQuery("pmin&pmax=60&gt=2"));
        LwM2mAttributeSet attResult = new LwM2mAttributeSet( //
                LwM2mAttributes.create(LwM2mAttributes.MINIMUM_PERIOD), //
                LwM2mAttributes.create(LwM2mAttributes.MAXIMUM_PERIOD, 60l), //
                LwM2mAttributes.create(LwM2mAttributes.GREATER_THAN, 2d));

        assertEquals(attResult, parsed);
    }
}
