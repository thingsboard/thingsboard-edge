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

import org.junit.Assert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.thingsboard.server.common.data.kv.JsonDataEntry;
import org.thingsboard.server.common.data.kv.StringDataEntry;
import org.thingsboard.server.exception.DataValidationException;

class ConstraintValidatorTest {

    private static final int MIN_IN_MS = 60000;
    private static final int _1M = 1_000_000;

    @Test
    void validateFields() {
        StringDataEntry stringDataEntryValid = new StringDataEntry("key", "value");

        StringDataEntry stringDataEntryInvalid1 = new StringDataEntry("<object type=\"text/html\"><script>alert(document)</script></object>", "value");
        StringDataEntry stringDataEntryInvalid2 = new StringDataEntry("key", "<object type=\"text/html\"><script>alert(document)</script></object>");

        JsonDataEntry jsonDataEntryInvalid = new JsonDataEntry("key", "{\"value\": <object type=\"text/html\"><script>alert(document)</script></object>}");

        Assert.assertThrows(DataValidationException.class, () -> ConstraintValidator.validateFields(stringDataEntryInvalid1));
        Assert.assertThrows(DataValidationException.class, () -> ConstraintValidator.validateFields(stringDataEntryInvalid2));
        Assert.assertThrows(DataValidationException.class, () -> ConstraintValidator.validateFields(jsonDataEntryInvalid));
        ConstraintValidator.validateFields(stringDataEntryValid);
    }

    @Test
    void validatePerMinute() {
        StringDataEntry stringDataEntryValid = new StringDataEntry("key", "value");

        long start = System.currentTimeMillis();
        for (int i = 0; i < _1M; i++) {
            ConstraintValidator.validateFields(stringDataEntryValid);
        }
        long end = System.currentTimeMillis();

        Assertions.assertTrue(MIN_IN_MS > end - start);
    }
}