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
package org.thingsboard.server.queue.task;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.job.JobType;
import org.thingsboard.server.gen.transport.TransportProtos.JobStatsMsg;
import org.thingsboard.server.gen.transport.TransportProtos.TaskProto;
import org.thingsboard.server.queue.TbQueueAdmin;
import org.thingsboard.server.queue.TbQueueConsumer;
import org.thingsboard.server.queue.TbQueueProducer;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.discovery.TbServiceInfoProvider;
import org.thingsboard.server.queue.discovery.TopicService;
import org.thingsboard.server.queue.kafka.TbKafkaAdmin;
import org.thingsboard.server.queue.kafka.TbKafkaConsumerStatsService;
import org.thingsboard.server.queue.kafka.TbKafkaConsumerTemplate;
import org.thingsboard.server.queue.kafka.TbKafkaProducerTemplate;
import org.thingsboard.server.queue.kafka.TbKafkaSettings;
import org.thingsboard.server.queue.kafka.TbKafkaTopicConfigs;
import org.thingsboard.server.queue.settings.TasksQueueConfig;

@Component
@ConditionalOnExpression("'${queue.type:null}'=='kafka'")
public class KafkaTaskProcessorQueueFactory implements TaskProcessorQueueFactory {

    private final TopicService topicService;
    private final TbServiceInfoProvider serviceInfoProvider;
    private final TasksQueueConfig tasksQueueConfig;
    private final TbKafkaSettings kafkaSettings;
    private final TbKafkaConsumerStatsService consumerStatsService;

    private final TbQueueAdmin tasksAdmin;

    public KafkaTaskProcessorQueueFactory(TopicService topicService,
                                          TbServiceInfoProvider serviceInfoProvider,
                                          TasksQueueConfig tasksQueueConfig,
                                          TbKafkaSettings kafkaSettings,
                                          TbKafkaConsumerStatsService consumerStatsService,
                                          TbKafkaTopicConfigs kafkaTopicConfigs) {
        this.topicService = topicService;
        this.serviceInfoProvider = serviceInfoProvider;
        this.tasksQueueConfig = tasksQueueConfig;
        this.kafkaSettings = kafkaSettings;
        this.consumerStatsService = consumerStatsService;
        this.tasksAdmin = new TbKafkaAdmin(kafkaSettings, kafkaTopicConfigs.getTasksConfigs());
    }

    @Override
    public TbQueueConsumer<TbProtoQueueMsg<TaskProto>> createTaskConsumer(JobType jobType) {
        return TbKafkaConsumerTemplate.<TbProtoQueueMsg<TaskProto>>builder()
                .settings(kafkaSettings)
                .topic(topicService.buildTopicName(jobType.getTasksTopic()))
                .clientId(jobType.name().toLowerCase() + "-task-consumer-" + serviceInfoProvider.getServiceId())
                .groupId(topicService.buildTopicName(jobType.name().toLowerCase() + "-task-consumer-group"))
                .decoder(msg -> new TbProtoQueueMsg<>(msg.getKey(), TaskProto.parseFrom(msg.getData()), msg.getHeaders()))
                .admin(tasksAdmin)
                .statsService(consumerStatsService)
                .build();
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<JobStatsMsg>> createJobStatsProducer() {
        return TbKafkaProducerTemplate.<TbProtoQueueMsg<JobStatsMsg>>builder()
                .clientId("job-stats-producer-" + serviceInfoProvider.getServiceId())
                .defaultTopic(topicService.buildTopicName(tasksQueueConfig.getStatsTopic()))
                .settings(kafkaSettings)
                .admin(tasksAdmin)
                .build();
    }

}
