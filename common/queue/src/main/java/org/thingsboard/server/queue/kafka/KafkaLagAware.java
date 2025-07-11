package org.thingsboard.server.queue.kafka;

import org.apache.kafka.common.TopicPartition;

import java.util.Map;

public interface KafkaLagAware {
    Map<TopicPartition, Long> getLagPerPartition();

}
