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
package org.thingsboard.migrator;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;

import java.time.LocalTime;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class BaseMigrationService implements ApplicationRunner {

    @Autowired
    protected ThreadPoolExecutor executor;

    @Value("${stats_print_interval}")
    private int statsPrintInterval;

    protected final ConcurrentMap<Object, AtomicInteger> processed = new ConcurrentHashMap<>();

    @Override
    public final void run(ApplicationArguments args) throws Exception {
        System.out.println("Starting " + getClass().getSimpleName());
        try {
            start();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(0);
            return;
        }
        executor.shutdown();
        executor.awaitTermination(Integer.MAX_VALUE, TimeUnit.SECONDS);
        afterFinished();
        System.out.println("Finished successfully!");
        System.exit(0);
    }

    protected abstract void start() throws Exception;

    protected void afterFinished() throws Exception {}

    protected void reportProcessed(Object key, Object data) {
        int n = processed.computeIfAbsent(key, k -> new AtomicInteger()).incrementAndGet();
        if (n % statsPrintInterval == 0) {
            printStats(key, n, data);
        }
    }

    protected void finishedProcessing(Object key) {
        int n = Optional.ofNullable(processed.remove(key)).map(AtomicInteger::get).orElse(0);
        printStats(key, n, null);
    }

    protected void printStats(Object key, int n, Object lastData) {
        if (n > 0) {
            System.out.println("[" + LocalTime.now() + "] [" + key + "] Processed: " + n +
                    (lastData != null ? ". Last: " + StringUtils.abbreviate(lastData.toString(), 100) : ""));
        }
    }

}
