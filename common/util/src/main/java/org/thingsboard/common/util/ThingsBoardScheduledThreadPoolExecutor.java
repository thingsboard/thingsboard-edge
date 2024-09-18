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
package org.thingsboard.common.util;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

@Slf4j
final class ThingsBoardScheduledThreadPoolExecutor extends ScheduledThreadPoolExecutor {

    public ThingsBoardScheduledThreadPoolExecutor(int corePoolSize, ThreadFactory threadFactory) {
        super(corePoolSize, threadFactory);
    }

    @Override
    protected void afterExecute(Runnable r, Throwable t) {
        super.afterExecute(r, t);
        logExceptionsAfterExecute(r, t);
    }

    private static void logExceptionsAfterExecute(Runnable r, Throwable directThrowable) {
        Throwable wrappedThrowable = extractThrowableWrappedInFuture(r);
        if (wrappedThrowable != null) {
            if (wrappedThrowable instanceof CancellationException) {
                log.debug("Task was cancelled.", wrappedThrowable);
            } else {
                log.error("Uncaught error occurred during task execution!", wrappedThrowable);
            }
        }

        if (directThrowable != null) {
            log.error("Uncaught error occurred during task execution!", directThrowable);
        }
    }

    private static Throwable extractThrowableWrappedInFuture(Runnable runnable) {
        if (runnable instanceof Future<?> future && future.isDone()) {
            try {
                future.get();
            } catch (InterruptedException e) { // should not happen due to isDone() check
                throw new AssertionError("InterruptedException caught after isDone() check on a future", e);
            } catch (CancellationException e) {
                return e;
            } catch (ExecutionException e) {
                return e.getCause();
            }
        }
        return null;
    }

    @Override
    public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
        if (command == null) { // preserve the original NPE behavior of ScheduledThreadPoolExecutor with a more helpful message
            throw new NullPointerException("command is null");
        }
        return super.scheduleAtFixedRate(new SafePeriodicRunnable(command), initialDelay, period, unit);
    }

    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
        if (command == null) { // preserve the original NPE behavior of ScheduledThreadPoolExecutor with a more helpful message
            throw new NullPointerException("command is null");
        }
        return super.scheduleWithFixedDelay(new SafePeriodicRunnable(command), initialDelay, delay, unit);
    }

    private record SafePeriodicRunnable(Runnable runnable) implements Runnable {

        public void run() {
            try {
                runnable.run();
            } catch (Exception ex) {
                log.error("Uncaught exception occurred during periodic task execution!", ex);
            }
            // Intentionally, no catch block for Throwable; uncaught Throwables will be handled in afterExecute()
        }

    }

}
