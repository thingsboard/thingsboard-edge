/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2024 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.queue.rabbitmq;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.GetResponse;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.queue.TbQueueAdmin;
import org.thingsboard.server.queue.TbQueueMsgDecoder;
import org.thingsboard.server.queue.common.DefaultTbQueueMsg;

@ExtendWith(MockitoExtension.class)
class TbRabbitMqConsumerTemplateTest {

  private static final String TOPIC = "some-topic";

  @Mock
  private TbQueueAdmin admin;

  @Mock
  private ConnectionFactory connectionFactory;

  @Mock
  private TbQueueMsgDecoder<DefaultTbQueueMsg> decoder;

  @Mock
  private Connection connection;

  @Mock
  private Channel channel;

  @Mock
  private TopicPartitionInfo partition;

  @Mock
  private GetResponse getResponse;

  private TbRabbitMqConsumerTemplate<DefaultTbQueueMsg> consumer;

  private void setUpConsumerWithMaxPollMessages(int maxPollMessages) throws Exception {
    when(connectionFactory.newConnection()).thenReturn(connection);
    when(connection.createChannel()).thenReturn(channel);
    TbRabbitMqSettings settings = new TbRabbitMqSettings();
    settings.setMaxPollMessages(maxPollMessages);
    settings.setConnectionFactory(connectionFactory);

    consumer = new TbRabbitMqConsumerTemplate<>(admin, settings, TOPIC, decoder);
    when(partition.getFullTopicName()).thenReturn(TOPIC);
    consumer.subscribe(Set.of(partition));
  }

  @Test
  void pollWithMax5PollMessagesReturnsEmptyListIfNoMessages() throws Exception {
    setUpConsumerWithMaxPollMessages(5);
    when(channel.basicGet(anyString(), anyBoolean())).thenReturn(null);

    assertThat(consumer.poll(0L)).isEmpty();

    verify(channel).basicGet(anyString(), anyBoolean());
  }

  @Test
  void pollWithMax5PollMessagesReturns5MessagesIfQueueContains5() throws Exception {
    setUpConsumerWithMaxPollMessages(5);
    when(getResponse.getBody()).thenReturn(newMessageBody());
    when(channel.basicGet(anyString(), anyBoolean())).thenReturn(getResponse);

    assertThat(consumer.poll(0L)).hasSize(5);

    verify(channel, times(5)).basicGet(anyString(), anyBoolean());
  }

  @Test
  void pollWithMax1PollMessageReturns1MessageIfQueueContainsMore() throws Exception {
    setUpConsumerWithMaxPollMessages(1);
    when(getResponse.getBody()).thenReturn(newMessageBody());
    when(channel.basicGet(anyString(), anyBoolean())).thenReturn(getResponse);

    assertThat(consumer.poll(0L)).hasSize(1);

    verify(channel).basicGet(anyString(), anyBoolean());
  }

  @Test
  void pollWithMax3PollMessagesReturns2MessagesIfQueueContains2() throws Exception {
    setUpConsumerWithMaxPollMessages(3);
    when(getResponse.getBody()).thenReturn(newMessageBody());
    when(channel.basicGet(anyString(), anyBoolean())).thenReturn(getResponse, getResponse, null);

    assertThat(consumer.poll(0L)).hasSize(2);

    verify(channel, times(3)).basicGet(anyString(), anyBoolean());
  }

  private byte[] newMessageBody() {
    return ("{\"key\": \"" + UUID.randomUUID() + "\"}").getBytes(StandardCharsets.UTF_8);
  }

}