/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2022 ThingsBoard, Inc. All Rights Reserved.
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
    public void givenQueue_whenPoll_thenReturnList() throws InterruptedException {
        Gson gson = new Gson();
        String topic = "tb_core_notification.tb-node-0";
        List<TbQueueMsg> msgs = new ArrayList<>(1001);
        for (int i = 0; i < 1001; i++) {
            DefaultTbQueueMsg msg = gson.fromJson("{\"key\": \"" + UUID.randomUUID() + "\"}", DefaultTbQueueMsg.class);
            msgs.add(msg);
            storage.put(topic, msg);
        }

        assertThat(storage.getLagTotal()).as("total lag is 1001").isEqualTo(1001);
        assertThat(storage.get(topic)).as("poll exactly 1000 msgs").isEqualTo(msgs.subList(0, 1000));
        assertThat(storage.get(topic)).as("poll last 1 message").isEqualTo(msgs.subList(1000, 1001));
        assertThat(storage.getLagTotal()).as("total lag is zero").isEqualTo(0);
    }
}
