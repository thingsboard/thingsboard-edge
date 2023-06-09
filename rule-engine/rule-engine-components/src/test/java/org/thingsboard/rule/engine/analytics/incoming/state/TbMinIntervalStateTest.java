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
package org.thingsboard.rule.engine.analytics.incoming.state;

import com.google.gson.JsonPrimitive;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.math.BigDecimal;

@RunWith(MockitoJUnitRunner.class)
class TbMinIntervalStateTest {

    private TbMinIntervalState state;

    @BeforeEach
    public void init() {
        state = new TbMinIntervalState();
    }


    @Test
    public void testDoUpdate() {
        state.doUpdate(new JsonPrimitive("1"));
        state.doUpdate(new JsonPrimitive(2));
        state.doUpdate(new JsonPrimitive(0x3));
        state.doUpdate(new JsonPrimitive('4'));
        TbIntervalStateUtil.assertEquals(
                new BigDecimal(1),
                state.getMin(),
                "TbMinIntervalState MIN"
        );
    }

    @Test
    public void testDoUpdateBigNumbers() {
        BigDecimal num1 = new BigDecimal("11111111111111111111111111111111111111111111111111111");
        BigDecimal num2 = new BigDecimal("22222222222222222222222222222222222222222222222222222");
        BigDecimal num3 = new BigDecimal("33333333333333333333333333333333333333333333333333333");
        BigDecimal num4 = new BigDecimal("44444444444444444444444444444444444444444444444444444");

        TbIntervalStateUtil.doUpdateForValues(state, num1, num2, num3, num4);
        TbIntervalStateUtil.assertEquals(num1, state.getMin(), "TbMinIntervalState MIN");
    }

    @Test
    public void testDoUpdateSmallNumbers() {
        BigDecimal num1 = new BigDecimal("0.11111111111111111111111111111111111111111111111111111");
        BigDecimal num2 = new BigDecimal("0.22222222222222222222222222222222222222222222222222222");
        BigDecimal num3 = new BigDecimal("0.33333333333333333333333333333333333333333333333333333");
        BigDecimal num4 = new BigDecimal("0.44444444444444444444444444444444444444444444444444444");

        TbIntervalStateUtil.doUpdateForValues(state, num1, num2, num3, num4);
        TbIntervalStateUtil.assertEquals(num1, state.getMin(), "TbMinIntervalState MIN");
    }
}