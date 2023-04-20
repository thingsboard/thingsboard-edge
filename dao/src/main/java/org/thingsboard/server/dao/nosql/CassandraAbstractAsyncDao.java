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
package org.thingsboard.server.dao.nosql;

import com.google.common.base.Function;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.common.util.ThingsBoardThreadFactory;

import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by ashvayka on 21.02.17.
 */
public abstract class CassandraAbstractAsyncDao extends CassandraAbstractDao {

    protected ExecutorService readResultsProcessingExecutor;

    @PostConstruct
    public void startExecutor() {
        readResultsProcessingExecutor = Executors.newCachedThreadPool(ThingsBoardThreadFactory.forName("cassandra-callback"));
    }

    @PreDestroy
    public void stopExecutor() {
        if (readResultsProcessingExecutor != null) {
            readResultsProcessingExecutor.shutdownNow();
        }
    }

    protected <T> ListenableFuture<T> getFuture(TbResultSetFuture future, java.util.function.Function<TbResultSet, T> transformer) {
        return Futures.transform(future, new Function<TbResultSet, T>() {
            @Nullable
            @Override
            public T apply(@Nullable TbResultSet input) {
                return transformer.apply(input);
            }
        }, readResultsProcessingExecutor);
    }

    protected <T> ListenableFuture<T> getFutureAsync(TbResultSetFuture future, com.google.common.util.concurrent.AsyncFunction<TbResultSet, T> transformer) {
        return Futures.transformAsync(future, new AsyncFunction<TbResultSet, T>() {
            @Nullable
            @Override
            public ListenableFuture<T> apply(@Nullable TbResultSet input) {
                try {
                    return transformer.apply(input);
                } catch (Exception e) {
                    return Futures.immediateFailedFuture(e);
                }
            }
        }, readResultsProcessingExecutor);
    }

}
