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
package org.thingsboard.server.queue.common;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

/**
 * This class deduplicate executions of the specified function.
 * Useful in cluster mode, when you get event about partition change multiple times.
 * Assuming that the function execution is expensive, we should execute it immediately when first time event occurs and
 * later, once the processing of first event is done, process last pending task.
 *
 * @param <P> parameters of the function
 */
@Slf4j
public class EventDeduplicationExecutor<P> {
    private final String name;
    private final ExecutorService executor;
    private final Consumer<P> function;
    private P pendingTask;
    private boolean busy;

    public EventDeduplicationExecutor(String name, ExecutorService executor, Consumer<P> function) {
        this.name = name;
        this.executor = executor;
        this.function = function;
    }

    public void submit(P params) {
        log.debug("[{}] Going to submit: {}", name, params);
        synchronized (EventDeduplicationExecutor.this) {
            if (!busy) {
                busy = true;
                pendingTask = null;
                try {
                    log.debug("[{}] Submitting task: {}", name, params);
                    executor.submit(() -> {
                        try {
                            log.debug("[{}] Executing task: {}", name, params);
                            function.accept(params);
                        } catch (Throwable e) {
                            log.warn("[{}] Failed to process task with parameters: {}", name, params, e);
                            throw e;
                        } finally {
                            unlockAndProcessIfAny();
                        }
                    });
                } catch (Throwable e) {
                    log.warn("[{}] Failed to submit task with parameters: {}", name, params, e);
                    unlockAndProcessIfAny();
                    throw e;
                }
            } else {
                log.debug("[{}] Task is already in progress. {} pending task: {}", name, pendingTask == null ? "adding" : "updating", params);
                pendingTask = params;
            }
        }
    }

    private void unlockAndProcessIfAny() {
        synchronized (EventDeduplicationExecutor.this) {
            busy = false;
            if (pendingTask != null) {
                submit(pendingTask);
            }
        }
    }
}
