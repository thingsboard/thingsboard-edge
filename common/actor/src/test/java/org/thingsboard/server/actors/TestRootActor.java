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
package org.thingsboard.server.actors;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.msg.TbActorMsg;
import org.thingsboard.server.common.msg.TbActorStopReason;

@Slf4j
public class TestRootActor extends AbstractTbActor {

    @Getter
    private final TbActorId actorId;
    @Getter
    private final ActorTestCtx testCtx;

    private boolean initialized;
    private long sum;
    private int count;

    public TestRootActor(TbActorId actorId, ActorTestCtx testCtx) {
        this.actorId = actorId;
        this.testCtx = testCtx;
    }

    @Override
    public void init(TbActorCtx ctx) throws TbActorException {
        super.init(ctx);
        initialized = true;
    }

    @Override
    public boolean process(TbActorMsg msg) {
        if (initialized) {
            int value = ((IntTbActorMsg) msg).getValue();
            sum += value;
            count += 1;
            if (count == testCtx.getExpectedInvocationCount()) {
                testCtx.getActual().set(sum);
                testCtx.getInvocationCount().addAndGet(count);
                sum = 0;
                count = 0;
                testCtx.getLatch().countDown();
            }
        }
        return true;
    }

    @Override
    public void destroy(TbActorStopReason stopReason, Throwable cause) {

    }

    public static class TestRootActorCreator implements TbActorCreator {

        private final TbActorId actorId;
        private final ActorTestCtx testCtx;

        public TestRootActorCreator(TbActorId actorId, ActorTestCtx testCtx) {
            this.actorId = actorId;
            this.testCtx = testCtx;
        }

        @Override
        public TbActorId createActorId() {
            return actorId;
        }

        @Override
        public TbActor createActor() {
            return new TestRootActor(actorId, testCtx);
        }
    }
}
