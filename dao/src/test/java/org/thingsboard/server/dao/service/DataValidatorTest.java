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
package org.thingsboard.server.dao.service;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.thingsboard.server.exception.DataValidationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.spy;

@Slf4j
public class DataValidatorTest {

    DataValidator<?> dataValidator;

    @BeforeEach
    void setUp() {
        dataValidator = spy(DataValidator.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "coffee", "1", "big box", "世界", "!", "--", "~!@#$%^&*()_+=-/|\\[]{};:'`\"?<>,.", "\uD83D\uDC0C", "\041",
            "Gdy Pomorze nie pomoże, to pomoże może morze, a gdy morze nie pomoże, to pomoże może Gdańsk",
    })
    void validateName_thenOK(final String name) {
        dataValidator.validateString("Device name", name);
        dataValidator.validateString("Asset name", name);
        dataValidator.validateString("Asset profile name", name);
        dataValidator.validateString("Alarm type", name);
        dataValidator.validateString("Customer name", name);
        dataValidator.validateString("Tenant name", name);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "", " ", "  ", "\n", "\r\n", "\t", "\000", "\000\000", "\001", "\002", "\040", "\u0000", "\u0000\u0000",
            "F0929906\000\000\000\000\000\000\000\000\000", "\000\000\000F0929906",
            "\u0000F0929906", "F092\u00009906", "F0929906\u0000"
    })
    void validateName_thenDataValidationException(final String name) {
        DataValidationException exception;
        exception = Assertions.assertThrows(DataValidationException.class, () -> dataValidator.validateString("Asset name", name));
        log.warn("Exception message Asset name: {}", exception.getMessage());
        assertThat(exception.getMessage()).as("message Asset name").containsPattern("Asset name .*");

        exception = Assertions.assertThrows(DataValidationException.class, () -> dataValidator.validateString("Device name", name));
        log.warn("Exception message Device name: {}", exception.getMessage());
        assertThat(exception.getMessage()).as("message Device name").containsPattern("Device name .*");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "aZ1_!#$%&'*+/=?`{|}~^.-@mail.io", "support@thingsboard.io",
    })
    public void validateEmail(String email) {
        DataValidator.validateEmail(email);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "test:1@mail.io", "test()1@mail.io", "test[]1@mail.io",
            "test\\1@mail.io", "test\"1@mail.io", "test<>1@mail.io",
    })
    public void validateEmailInvalid(String email) {
        Assertions.assertThrows(DataValidationException.class, () -> DataValidator.validateEmail(email));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "azAZ09_.-", "topic",
    })
    public void validateQueueNameOrTopic(String value) {
        DataValidator.validateQueueNameOrTopic(value, "name");
        DataValidator.validateQueueNameOrTopic(value, "topic");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "", " ", "  ", "\n", "\r\n", "\t", "\000", "\000\000", "\001", "\002", "\040", "\u0000", "\u0000\u0000",
            "topic@home", "!", ",", "Łódź",
            "\uD83D\uDC0C", "\041",
            "F0929906\000\000\000\000\000\000\000\000\000",
    })
    public void validateQueueNameOrTopicInvalid(String value) {
        Assertions.assertThrows(DataValidationException.class, () -> DataValidator.validateQueueNameOrTopic(value, "name"));
        Assertions.assertThrows(DataValidationException.class, () -> DataValidator.validateQueueNameOrTopic(value, "topic"));
    }

}
