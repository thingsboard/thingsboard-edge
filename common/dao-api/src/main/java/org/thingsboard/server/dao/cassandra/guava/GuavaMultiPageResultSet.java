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
package org.thingsboard.server.dao.cassandra.guava;

import com.datastax.oss.driver.api.core.cql.AsyncResultSet;
import com.datastax.oss.driver.api.core.cql.ColumnDefinitions;
import com.datastax.oss.driver.api.core.cql.ExecutionInfo;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.Row;
import com.datastax.oss.driver.api.core.cql.Statement;
import com.datastax.oss.driver.internal.core.util.CountingIterator;
import com.datastax.oss.driver.internal.core.util.concurrent.BlockingOperation;
import edu.umd.cs.findbugs.annotations.NonNull;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class GuavaMultiPageResultSet implements ResultSet {

    private final RowIterator iterator;
    private final List<ExecutionInfo> executionInfos = new ArrayList<>();
    private ColumnDefinitions columnDefinitions;

    public GuavaMultiPageResultSet(@NonNull GuavaSession session, @NonNull Statement statement, @NonNull AsyncResultSet firstPage) {
        assert firstPage.hasMorePages();
        this.iterator = new RowIterator(session, statement, firstPage);
        this.executionInfos.add(firstPage.getExecutionInfo());
        this.columnDefinitions = firstPage.getColumnDefinitions();
    }

    @NonNull
    @Override
    public ColumnDefinitions getColumnDefinitions() {
        return columnDefinitions;
    }

    @NonNull
    @Override
    public List<ExecutionInfo> getExecutionInfos() {
        return executionInfos;
    }

    @Override
    public boolean isFullyFetched() {
        return iterator.isFullyFetched();
    }

    @Override
    public int getAvailableWithoutFetching() {
        return iterator.remaining();
    }

    @NonNull
    @Override
    public Iterator<Row> iterator() {
        return iterator;
    }

    @Override
    public boolean wasApplied() {
        return iterator.wasApplied();
    }

    private class RowIterator extends CountingIterator<Row> {
        private GuavaSession session;
        private Statement statement;
        private AsyncResultSet currentPage;
        private Iterator<Row> currentRows;

        private RowIterator(GuavaSession session, Statement statement, AsyncResultSet firstPage) {
            super(firstPage.remaining());
            this.session = session;
            this.statement = statement;
            this.currentPage = firstPage;
            this.currentRows = firstPage.currentPage().iterator();
        }

        @Override
        protected Row computeNext() {
            maybeMoveToNextPage();
            return currentRows.hasNext() ? currentRows.next() : endOfData();
        }

        private void maybeMoveToNextPage() {
            if (!currentRows.hasNext() && currentPage.hasMorePages()) {
                BlockingOperation.checkNotDriverThread();
                ByteBuffer nextPagingState = currentPage.getExecutionInfo().getPagingState();
                this.statement = this.statement.setPagingState(nextPagingState);
                AsyncResultSet nextPage = GuavaSession.getSafe(this.session.executeAsync(this.statement));
                currentPage = nextPage;
                remaining += nextPage.remaining();
                currentRows = nextPage.currentPage().iterator();
                executionInfos.add(nextPage.getExecutionInfo());
                // The definitions can change from page to page if this result set was built from a bound
                // 'SELECT *', and the schema was altered.
                columnDefinitions = nextPage.getColumnDefinitions();
            }
        }

        private boolean isFullyFetched() {
            return !currentPage.hasMorePages();
        }

        private boolean wasApplied() {
            return currentPage.wasApplied();
        }
    }
}
