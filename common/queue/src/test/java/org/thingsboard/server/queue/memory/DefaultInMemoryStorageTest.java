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
package org.thingsboard.server.queue.memory;

import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.thingsboard.server.queue.TbQueueMsg;
import org.thingsboard.server.queue.common.DefaultTbQueueMsg;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@Slf4j
public class DefaultInMemoryStorageTest {
    static final int MAX_POLL_SIZE = 1000;
    final Gson gson = new Gson();
    final String topic = "tb_core_notification.tb-node-0";

    InMemoryStorage storage = new DefaultInMemoryStorage();

    @Test
    public void givenStorage_whenGetLagTotal_thenReturnInteger() throws InterruptedException {
        assertThat(storage.getLagTotal()).isEqualTo(0);
        storage.put("main", mock(TbQueueMsg.class));
        assertThat(storage.getLagTotal()).isEqualTo(1);
        storage.put("main", mock(TbQueueMsg.class));
        assertThat(storage.getLagTotal()).isEqualTo(2);
        storage.put("hp", mock(TbQueueMsg.class));
        assertThat(storage.getLagTotal()).isEqualTo(3);
        storage.get("main");
        assertThat(storage.getLagTotal()).isEqualTo(1);
    }

    @Test
    public void givenQueueWithMoreThenBatchSize_whenPoll_thenReturnFullListAndSecondList() throws InterruptedException {
        List<TbQueueMsg> msgs = new ArrayList<>(MAX_POLL_SIZE + 1);
        for (int i = 0; i < MAX_POLL_SIZE + 1; i++) {
            DefaultTbQueueMsg msg = gson.fromJson("{\"key\": \"" + UUID.randomUUID() + "\"}", DefaultTbQueueMsg.class);
            msgs.add(msg);
            storage.put(topic, msg);
        }

        assertThat(storage.getLagTotal()).as("total lag is 1001").isEqualTo(MAX_POLL_SIZE + 1);
        assertThat(storage.get(topic)).as("poll exactly 1000 msgs").isEqualTo(msgs.subList(0, MAX_POLL_SIZE));
        assertThat(storage.get(topic)).as("poll last 1 message").isEqualTo(msgs.subList(MAX_POLL_SIZE, MAX_POLL_SIZE + 1));
        assertThat(storage.getLagTotal()).as("total lag is zero").isEqualTo(0);
    }

    private void testPollOnce(final int msgCount) throws InterruptedException {
        List<TbQueueMsg> msgs = new ArrayList<>(msgCount);
        for (int i = 0; i < msgCount; i++) {
            DefaultTbQueueMsg msg = gson.fromJson("{\"key\": \"" + UUID.randomUUID() + "\"}", DefaultTbQueueMsg.class);
            msgs.add(msg);
            storage.put(topic, msg);
        }

        assertThat(storage.getLagTotal()).as("total lag before poll").isEqualTo(msgCount);
        assertThat(storage.get(topic)).as("polled exactly msgs").isEqualTo(msgs.subList(0, msgCount));
        assertThat(storage.getLagTotal()).as("final lag is zero").isEqualTo(0);
    }

    @Test
    public void givenQueueWithExactBatchSize_whenPoll_thenReturnExactBatchSizeList() throws InterruptedException {
        testPollOnce(MAX_POLL_SIZE);
    }

    @Test
    public void givenQueueWithExactBatchSizeMinusOne_whenPoll_thenReturnCorrectSizeList() throws InterruptedException {
        testPollOnce(MAX_POLL_SIZE - 1);
    }

    @Test
    public void givenQueueWithExactBatchSizeMinusTen_whenPoll_thenReturnCorrectSizeList() throws InterruptedException {
        testPollOnce(MAX_POLL_SIZE - 10);
    }

    @Test
    public void givenQueueEmpty_whenPoll_thenReturnEmptyList() throws InterruptedException {
        testPollOnce(0);
    }

    @Test
    public void givenQueueWithSingleMessage_whenPoll_thenReturnSingletonList() throws InterruptedException {
        testPollOnce(1);
    }

    @Test
    public void givenQueueWithTwoMessages_whenPoll_thenReturnCorrectSizeList() throws InterruptedException {
        testPollOnce(2);
    }

    @Test
    public void givenQueueWithTenMessages_whenPoll_thenReturnCorrectSizeList() throws InterruptedException {
        testPollOnce(10);
    }

}
