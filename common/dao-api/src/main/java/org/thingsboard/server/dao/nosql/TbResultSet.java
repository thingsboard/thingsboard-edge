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

import com.datastax.oss.driver.api.core.cql.AsyncResultSet;
import com.datastax.oss.driver.api.core.cql.ColumnDefinitions;
import com.datastax.oss.driver.api.core.cql.ExecutionInfo;
import com.datastax.oss.driver.api.core.cql.Row;
import com.datastax.oss.driver.api.core.cql.Statement;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.SettableFuture;
import edu.umd.cs.findbugs.annotations.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.function.Function;

public class TbResultSet implements AsyncResultSet {

    private final Statement originalStatement;
    private final AsyncResultSet delegate;
    private final Function<Statement, TbResultSetFuture> executeAsyncFunction;

    public TbResultSet(Statement originalStatement, AsyncResultSet delegate,
                       Function<Statement, TbResultSetFuture> executeAsyncFunction) {
        this.originalStatement = originalStatement;
        this.delegate = delegate;
        this.executeAsyncFunction = executeAsyncFunction;
    }

    @NonNull
    @Override
    public ColumnDefinitions getColumnDefinitions() {
        return delegate.getColumnDefinitions();
    }

    @NonNull
    @Override
    public ExecutionInfo getExecutionInfo() {
        return delegate.getExecutionInfo();
    }

    @Override
    public int remaining() {
        return delegate.remaining();
    }

    @NonNull
    @Override
    public Iterable<Row> currentPage() {
        return delegate.currentPage();
    }

    @Override
    public boolean hasMorePages() {
        return delegate.hasMorePages();
    }

    @NonNull
    @Override
    public CompletionStage<AsyncResultSet> fetchNextPage() throws IllegalStateException {
        return delegate.fetchNextPage();
    }

    @Override
    public boolean wasApplied() {
        return delegate.wasApplied();
    }

    public ListenableFuture<List<Row>> allRows(Executor executor) {
        List<Row> allRows = new ArrayList<>();
        SettableFuture<List<Row>> resultFuture = SettableFuture.create();
        this.processRows(originalStatement, delegate, allRows, resultFuture, executor);
        return resultFuture;
    }

    private void processRows(Statement statement,
                             AsyncResultSet resultSet,
                             List<Row> allRows,
                             SettableFuture<List<Row>> resultFuture,
                             Executor executor) {
        allRows.addAll(loadRows(resultSet));
        if (resultSet.hasMorePages()) {
            ByteBuffer nextPagingState = resultSet.getExecutionInfo().getPagingState();
            Statement<?> nextStatement = statement.setPagingState(nextPagingState);
            TbResultSetFuture resultSetFuture = executeAsyncFunction.apply(nextStatement);
            Futures.addCallback(resultSetFuture,
                    new FutureCallback<TbResultSet>() {
                        @Override
                        public void onSuccess(@Nullable TbResultSet result) {
                            processRows(nextStatement, result,
                                    allRows, resultFuture, executor);
                        }

                        @Override
                        public void onFailure(Throwable t) {
                            resultFuture.setException(t);
                        }
                    }, executor != null ? executor : MoreExecutors.directExecutor()
            );
        } else {
            resultFuture.set(allRows);
        }
    }

    List<Row> loadRows(AsyncResultSet resultSet) {
        return Lists.newArrayList(resultSet.currentPage());
    }

}
