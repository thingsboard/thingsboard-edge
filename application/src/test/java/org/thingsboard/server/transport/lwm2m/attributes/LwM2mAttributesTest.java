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

import org.eclipse.leshan.core.link.attributes.InvalidAttributeException;
import org.eclipse.leshan.core.link.lwm2m.attributes.LwM2mAttribute;
import org.eclipse.leshan.core.link.lwm2m.attributes.LwM2mAttributeModel;
import org.eclipse.leshan.core.link.lwm2m.attributes.LwM2mAttributes;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class LwM2mAttributesTest {

    @ParameterizedTest(name = "Tests {index} : {0}")
    @MethodSource("supportNullAttributes")
    public void check_attribute_can_be_created_with_null_value(LwM2mAttributeModel<?> model)
            throws InvalidAttributeException {
        LwM2mAttribute<?> attribute = LwM2mAttributes.create(model);
        assertNotNull(attribute);
        assertFalse(attribute.hasValue());
        assertNull(attribute.getValue());
        attribute = LwM2mAttributes.create(model, null);
        assertNotNull(attribute);
        assertFalse(attribute.hasValue());
        assertNull(attribute.getValue());
    }

    @ParameterizedTest(name = "Tests {index} : {0}")
    @MethodSource("doesntSupportAttributesWithoutValue")
    public void check_attribute_can_not_be_created_without_value(LwM2mAttributeModel<?> model) {
        assertThrows(IllegalArgumentException.class, () -> LwM2mAttributes.create(model));
    }

    @ParameterizedTest(name = "Tests {index} : {0}")
    @MethodSource("doesntSupportAttributesWithValueNull")
    public void check_attribute_can_not_be_created_with_null(LwM2mAttributeModel<?> model) {
        assertThrows(IllegalArgumentException.class, () -> LwM2mAttributes.create(model, null));
    }

   private static Stream<Arguments> supportNullAttributes() throws InvalidAttributeException {
        return Stream.of(//
                Arguments.of(LwM2mAttributes.MINIMUM_PERIOD), //
                Arguments.of(LwM2mAttributes.MAXIMUM_PERIOD), //
                Arguments.of(LwM2mAttributes.EVALUATE_MINIMUM_PERIOD), //
                Arguments.of(LwM2mAttributes.EVALUATE_MAXIMUM_PERIOD), //
                Arguments.of(LwM2mAttributes.LESSER_THAN), //
                Arguments.of(LwM2mAttributes.GREATER_THAN), //
                Arguments.of(LwM2mAttributes.STEP) //
        );
    }

    private static Stream<Arguments> doesntSupportAttributesWithoutValue() throws InvalidAttributeException {
        return Stream.of(//
                Arguments.of(LwM2mAttributes.ENABLER_VERSION), //
                Arguments.of(LwM2mAttributes.OBJECT_VERSION)//
        );
    }

    private static Stream<Arguments> doesntSupportAttributesWithValueNull() throws InvalidAttributeException {
        return Stream.of(//
                Arguments.of(LwM2mAttributes.DIMENSION), //
                Arguments.of(LwM2mAttributes.SHORT_SERVER_ID) //
        );
    }
}

