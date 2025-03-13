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
package org.thingsboard.server.actors.service;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.BDDMockito;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.server.actors.ActorSystemContext;
import org.thingsboard.server.actors.shared.ComponentMsgProcessor;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.msg.TbActorStopReason;

import java.util.concurrent.ScheduledFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;

class ComponentActorTest {
    ComponentActor componentActor;

    @BeforeEach
    void setUp() {
        componentActor = Mockito.mock(ComponentActor.class);
    }

    @Test
    void scheduleStatsPersistTickTest() {
        Assertions.assertNull(componentActor.statsScheduledFuture);
        ScheduledFuture<?> statsScheduledFuture = Mockito.mock(ScheduledFuture.class);
        ActorSystemContext systemContext = Mockito.mock(ActorSystemContext.class);
        ReflectionTestUtils.setField(componentActor, "systemContext", systemContext);
        ComponentMsgProcessor<?> processor = Mockito.mock(ComponentMsgProcessor.class);
        componentActor.processor = processor;
        BDDMockito.willReturn(statsScheduledFuture).given(processor).scheduleStatsPersistTick(any(), anyLong());
        BDDMockito.willCallRealMethod().given(componentActor).scheduleStatsPersistTick();

        componentActor.scheduleStatsPersistTick();

        Assertions.assertNotNull(componentActor.statsScheduledFuture);
    }

    @Test
    void destroyTest() {
        ScheduledFuture<?> statsScheduledFuture = Mockito.mock(ScheduledFuture.class);
        componentActor.statsScheduledFuture = statsScheduledFuture;
        Assertions.assertNotNull(componentActor.statsScheduledFuture);
        Throwable cause = new Throwable();
        EntityId id = Mockito.mock(EntityId.class);
        ReflectionTestUtils.setField(componentActor, "id", id);
        BDDMockito.willCallRealMethod().given(componentActor).destroy(any(), any());

        componentActor.destroy(TbActorStopReason.STOPPED, cause);

        Mockito.verify(statsScheduledFuture).cancel(false);
        Assertions.assertNull(componentActor.statsScheduledFuture);
    }

}
