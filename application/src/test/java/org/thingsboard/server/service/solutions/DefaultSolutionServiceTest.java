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
package org.thingsboard.server.service.solutions;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.scheduler.SchedulerEvent;
import org.thingsboard.server.service.solutions.data.SolutionInstallContext;
import org.thingsboard.server.service.solutions.data.definition.SchedulerEventDefinition;
import org.thingsboard.server.service.solutions.data.solution.TenantSolutionTemplateInstructions;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.thingsboard.server.common.data.BaseDataWithAdditionalInfo.mapper;

@RunWith(SpringRunner.class)
public class DefaultSolutionServiceTest {

    @Test
    public void testSchedulerEventCreation() throws NoSuchMethodException, JsonProcessingException,
            InvocationTargetException, IllegalAccessException {
        Method getSchedulerEvent =
                DefaultSolutionService.class.getDeclaredMethod("getSchedulerEvent", SolutionInstallContext.class, SchedulerEventDefinition.class);
        getSchedulerEvent.setAccessible(true);

        DefaultSolutionService defaultSolutionService = mock(DefaultSolutionService.class);
        SchedulerEventDefinition schedulerEventDefinition = mockSchedulerEventDefinition();
        SolutionInstallContext ctx = mockSolutionInstallContext(schedulerEventDefinition);
        SchedulerEvent schedulerEvent = (SchedulerEvent) getSchedulerEvent.invoke(defaultSolutionService, ctx, schedulerEventDefinition);
        Assert.assertTrue(schedulerEvent.getOriginatorId() instanceof DeviceId);
    }

    private SolutionInstallContext mockSolutionInstallContext(SchedulerEventDefinition schedulerEventDefinition) {
        TenantId tenantId = new TenantId(UUID.randomUUID());
        String solutionId = "";
        User user = new User(new UserId(UUID.randomUUID()));
        SolutionInstallContext ctx = new SolutionInstallContext(tenantId, solutionId, user, new TenantSolutionTemplateInstructions());
        ctx.getRealIds().put(schedulerEventDefinition.getJsonId(), UUID.randomUUID().toString());
        ctx.getRealIds().put(schedulerEventDefinition.getOriginatorId().getId().toString(), UUID.randomUUID().toString());
        return ctx;
    }

    private SchedulerEventDefinition mockSchedulerEventDefinition() throws JsonProcessingException {
        EntityId originatorId = new DeviceId(UUID.randomUUID());
        String type = "START_IRRIGATION";
        String scheduleJsonString = "{\"timezone\": \"America/New_York\", \"startTime\": 1664877600000,\n\"repeat\": {\n" +
                "        \"type\": \"DAILY\", \"endsOn\": 1893474000000}}";
        JsonNode schedule = mapper.readTree(scheduleJsonString);
        String configJsonString = "{\"msgType\": \"START_IRRIGATION\"," +
                "      \"msgBody\": {\"durationInMinutes\": 60},\"metadata\": {}}";
        JsonNode config = mapper.readTree(scheduleJsonString);
        SchedulerEventDefinition schedulerEventDefinition = new SchedulerEventDefinition(originatorId, type, schedule, config);
        return schedulerEventDefinition;
    }
}