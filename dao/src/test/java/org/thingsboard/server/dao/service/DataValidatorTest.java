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

import org.junit.Test;
import org.thingsboard.server.exception.DataValidationException;

public class DataValidatorTest {

    @Test
    public void validateEmail() {
        String email = "aZ1_!#$%&'*+/=?`{|}~^.-@mail.io";
        DataValidator.validateEmail(email);
    }

    @Test(expected = DataValidationException.class)
    public void validateInvalidEmail1() {
        String email = "test:1@mail.io";
        DataValidator.validateEmail(email);
    }
    @Test(expected = DataValidationException.class)
    public void validateInvalidEmail2() {
        String email = "test()1@mail.io";
        DataValidator.validateEmail(email);
    }

    @Test(expected = DataValidationException.class)
    public void validateInvalidEmail3() {
        String email = "test[]1@mail.io";
        DataValidator.validateEmail(email);
    }

    @Test(expected = DataValidationException.class)
    public void validateInvalidEmail4() {
        String email = "test\\1@mail.io";
        DataValidator.validateEmail(email);
    }

    @Test(expected = DataValidationException.class)
    public void validateInvalidEmail5() {
        String email = "test\"1@mail.io";
        DataValidator.validateEmail(email);
    }

    @Test(expected = DataValidationException.class)
    public void validateInvalidEmail6() {
        String email = "test<>1@mail.io";
        DataValidator.validateEmail(email);
    }
}