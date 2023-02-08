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
package org.thingsboard.server.service.solutions.data.emulator;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.data.util.Pair;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.service.solutions.data.definition.EmulatorDefinition;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

public class FieldIrrigationStateEmulator implements CustomEmulator {

    private static final int START_HOUR = 6;
    private int idx;
    private List<Pair<Long, ObjectNode>> data = new ArrayList<>();

    @Override
    public void init(EmulatorDefinition emulatorDefinition) {
        idx = 0;
        var tz = TimeZone.getTimeZone("America/New_York");
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(System.currentTimeMillis());
        c.setTimeZone(tz);
        int curHour = c.get(Calendar.HOUR_OF_DAY);
        c.add(Calendar.DAY_OF_MONTH, curHour < START_HOUR ? -7 : -6);
        c.set(Calendar.HOUR_OF_DAY, START_HOUR);
        c.set(Calendar.MINUTE, 0);

        add(c, JacksonUtil.newObjectNode()
                .put("startTs", c.getTimeInMillis())
                .put("durationThreshold", 30 * 60000)
                .put("consumption", 423)
                .put("duration", 30 * 60000)
        );
        add(c, JacksonUtil.newObjectNode()
                .put("startTs", c.getTimeInMillis())
                .put("durationThreshold", 30 * 60000)
                .put("consumption", 407)
                .put("duration", 30 * 60000)
        );
        add(c, JacksonUtil.newObjectNode()
                .put("startTs", c.getTimeInMillis())
                .put("consumptionThreshold", 1000)
                .put("consumption", 1000)
                .put("duration", 63 * 60000)
        );
        add(c, JacksonUtil.newObjectNode()
                .put("startTs", c.getTimeInMillis())
                .put("consumptionThreshold", 500)
                .put("consumption", 500)
                .put("duration", 36 * 60000)
        );
        add(c, JacksonUtil.newObjectNode()
                .put("startTs", c.getTimeInMillis())
                .put("consumptionThreshold", 1000)
                .put("consumption", 1000)
                .put("duration", 63 * 60000)
        );
        add(c, JacksonUtil.newObjectNode()
                .put("startTs", c.getTimeInMillis())
                .put("durationThreshold", 30 * 60000)
                .put("duration", 30 * 60000)
                .put("consumption", 452)
        );
        add(c, JacksonUtil.newObjectNode()
                .put("startTs", c.getTimeInMillis())
                .put("durationThreshold", 30 * 60000)
                .put("duration", 30 * 60000)
                .put("consumption", 447)
        );
    }

    private void add(Calendar c, ObjectNode objectNode) {
        data.add(Pair.of(c.getTimeInMillis(), JacksonUtil.newObjectNode().set("irrigationTask", objectNode)));
        c.add(Calendar.DAY_OF_MONTH, 1);
    }

    @Override
    public Pair<Long, ObjectNode> getNextValue() {
        if (idx < data.size()) {
            var result = data.get(idx);
            idx++;
            return result;
        } else {
            return null;
        }
    }
}
