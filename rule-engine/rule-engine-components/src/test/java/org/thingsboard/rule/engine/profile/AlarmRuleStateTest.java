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
package org.thingsboard.rule.engine.profile;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.thingsboard.server.common.data.device.profile.AlarmCondition;
import org.thingsboard.server.common.data.device.profile.AlarmConditionFilter;
import org.thingsboard.server.common.data.device.profile.AlarmConditionFilterKey;
import org.thingsboard.server.common.data.device.profile.AlarmConditionKeyType;
import org.thingsboard.server.common.data.device.profile.AlarmRule;
import org.thingsboard.server.common.data.device.profile.SimpleAlarmConditionSpec;
import org.thingsboard.server.common.data.query.EntityKeyValueType;
import org.thingsboard.server.common.data.query.FilterPredicateValue;
import org.thingsboard.server.common.data.query.StringFilterPredicate;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

public class AlarmRuleStateTest {

    private static Stream<Arguments> testEvalCondition() {
        return Stream.of(
                Arguments.of(StringFilterPredicate.StringOperation.IN, "test,value", "test", AlarmEvalResult.TRUE),
                Arguments.of(StringFilterPredicate.StringOperation.IN, "test,value", "teeeeest", AlarmEvalResult.FALSE),
                Arguments.of(StringFilterPredicate.StringOperation.NOT_IN, "test,value", "test", AlarmEvalResult.FALSE),
                Arguments.of(StringFilterPredicate.StringOperation.NOT_IN, "test,value", "teeeeest", AlarmEvalResult.TRUE),
                Arguments.of(StringFilterPredicate.StringOperation.CONTAINS, "test value", "test value", AlarmEvalResult.TRUE),
                Arguments.of(StringFilterPredicate.StringOperation.CONTAINS, "test value", "test", AlarmEvalResult.FALSE),
                Arguments.of(StringFilterPredicate.StringOperation.NOT_CONTAINS, "test value", "test", AlarmEvalResult.TRUE),
                Arguments.of(StringFilterPredicate.StringOperation.NOT_CONTAINS, "test value", "test value", AlarmEvalResult.FALSE),
                Arguments.of(StringFilterPredicate.StringOperation.EQUAL, "test value", "test value", AlarmEvalResult.TRUE),
                Arguments.of(StringFilterPredicate.StringOperation.EQUAL, "test value", "test", AlarmEvalResult.FALSE),
                Arguments.of(StringFilterPredicate.StringOperation.NOT_EQUAL, "test value", "test", AlarmEvalResult.TRUE),
                Arguments.of(StringFilterPredicate.StringOperation.NOT_EQUAL, "test value", "test value", AlarmEvalResult.FALSE),
                Arguments.of(StringFilterPredicate.StringOperation.ENDS_WITH, "test value", "some test value", AlarmEvalResult.TRUE),
                Arguments.of(StringFilterPredicate.StringOperation.ENDS_WITH, "test value", "some test value2", AlarmEvalResult.FALSE),
                Arguments.of(StringFilterPredicate.StringOperation.STARTS_WITH, "test value", "test value attribute", AlarmEvalResult.TRUE),
                Arguments.of(StringFilterPredicate.StringOperation.STARTS_WITH, "test value", "test", AlarmEvalResult.FALSE)
        );
    }

    @ParameterizedTest
    @MethodSource
    public void testEvalCondition(StringFilterPredicate.StringOperation operation, String predicateValue, String attributeValue, AlarmEvalResult evalResult) {
            AlarmConditionFilterKey alarmConditionFilterKey = new AlarmConditionFilterKey(AlarmConditionKeyType.ATTRIBUTE, "stringKey");

            StringFilterPredicate predicate = new StringFilterPredicate();
            predicate.setOperation(operation);
            predicate.setValue(new FilterPredicateValue<>(predicateValue));

            AlarmConditionFilter alarmConditionFilter = new AlarmConditionFilter();
            alarmConditionFilter.setKey(alarmConditionFilterKey);
            alarmConditionFilter.setPredicate(predicate);
            alarmConditionFilter.setValueType(EntityKeyValueType.STRING);

            List<AlarmConditionFilter> condition = new ArrayList<>();
            condition.add(alarmConditionFilter);

            AlarmCondition alarmCondition = new AlarmCondition();
            alarmCondition.setSpec(new SimpleAlarmConditionSpec());
            alarmCondition.setCondition(condition);

            AlarmRule alarmRule = new AlarmRule();
            alarmRule.setCondition(alarmCondition);

            AlarmRuleState alarmRuleState = new AlarmRuleState(null, alarmRule, null, null, null);

            Set<AlarmConditionFilterKey> entityKeys = new HashSet<>(List.of(alarmConditionFilterKey));
            DataSnapshot result = new DataSnapshot(entityKeys);
            result.putValue(alarmConditionFilterKey, System.currentTimeMillis(), EntityKeyValue.fromString(attributeValue));
            Assertions.assertEquals(evalResult, alarmRuleState.eval(result));
    }
}
