/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2020 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.service.queue.processing;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.settings.TbRuleEngineQueueAckStrategyConfiguration;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class TbRuleEngineProcessingStrategyFactory {

    public TbRuleEngineProcessingStrategy newInstance(String name, TbRuleEngineQueueAckStrategyConfiguration configuration) {
        switch (configuration.getType()) {
            case "SKIP_ALL":
                return new SkipStrategy(name);
            case "RETRY_ALL":
                return new RetryStrategy(name, true, true, true, configuration);
            case "RETRY_FAILED":
                return new RetryStrategy(name, false, true, false, configuration);
            case "RETRY_TIMED_OUT":
                return new RetryStrategy(name, false, false, true, configuration);
            case "RETRY_FAILED_AND_TIMED_OUT":
                return new RetryStrategy(name, false, true, true, configuration);
            default:
                throw new RuntimeException("TbRuleEngineProcessingStrategy with type " + configuration.getType() + " is not supported!");
        }
    }

    private static class RetryStrategy implements TbRuleEngineProcessingStrategy {
        private final String queueName;
        private final boolean retrySuccessful;
        private final boolean retryFailed;
        private final boolean retryTimeout;
        private final int maxRetries;
        private final double maxAllowedFailurePercentage;
        private final long pauseBetweenRetries;

        private int initialTotalCount;
        private int retryCount;

        public RetryStrategy(String queueName, boolean retrySuccessful, boolean retryFailed, boolean retryTimeout, TbRuleEngineQueueAckStrategyConfiguration configuration) {
            this.queueName = queueName;
            this.retrySuccessful = retrySuccessful;
            this.retryFailed = retryFailed;
            this.retryTimeout = retryTimeout;
            this.maxRetries = configuration.getRetries();
            this.maxAllowedFailurePercentage = configuration.getFailurePercentage();
            this.pauseBetweenRetries = configuration.getPauseBetweenRetries();
        }

        @Override
        public TbRuleEngineProcessingDecision analyze(TbRuleEngineProcessingResult result) {
            if (result.isSuccess()) {
                return new TbRuleEngineProcessingDecision(true, null);
            } else {
                if (retryCount == 0) {
                    initialTotalCount = result.getPendingMap().size() + result.getFailedMap().size() + result.getSuccessMap().size();
                }
                retryCount++;
                double failedCount = result.getFailedMap().size() + result.getPendingMap().size();
                if (maxRetries > 0 && retryCount > maxRetries) {
                    log.info("[{}] Skip reprocess of the rule engine pack due to max retries", queueName);
                    return new TbRuleEngineProcessingDecision(true, null);
                } else if (maxAllowedFailurePercentage > 0 && (failedCount / initialTotalCount) > maxAllowedFailurePercentage) {
                    log.info("[{}] Skip reprocess of the rule engine pack due to max allowed failure percentage", queueName);
                    return new TbRuleEngineProcessingDecision(true, null);
                } else {
                    ConcurrentMap<UUID, TbProtoQueueMsg<TransportProtos.ToRuleEngineMsg>> toReprocess = new ConcurrentHashMap<>(initialTotalCount);
                    if (retryFailed) {
                        result.getFailedMap().forEach(toReprocess::put);
                    }
                    if (retryTimeout) {
                        result.getPendingMap().forEach(toReprocess::put);
                    }
                    if (retrySuccessful) {
                        result.getSuccessMap().forEach(toReprocess::put);
                    }
                    log.info("[{}] Going to reprocess {} messages", queueName, toReprocess.size());
                    if (log.isTraceEnabled()) {
                        toReprocess.forEach((id, msg) -> log.trace("Going to reprocess [{}]: {}", id, msg.getValue()));
                    }
                    if (pauseBetweenRetries > 0) {
                        try {
                            Thread.sleep(TimeUnit.SECONDS.toMillis(pauseBetweenRetries));
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }
                    return new TbRuleEngineProcessingDecision(false, toReprocess);
                }
            }
        }
    }

    private static class SkipStrategy implements TbRuleEngineProcessingStrategy {

        private final String queueName;

        public SkipStrategy(String name) {
            this.queueName = name;
        }

        @Override
        public TbRuleEngineProcessingDecision analyze(TbRuleEngineProcessingResult result) {
            log.info("[{}] Reprocessing skipped for {} failed and {} timeout messages", queueName, result.getFailedMap().size(), result.getPendingMap().size());
            return new TbRuleEngineProcessingDecision(true, null);
        }
    }
}
