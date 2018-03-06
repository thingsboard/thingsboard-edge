/**
 * Thingsboard OÜ ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2018 Thingsboard OÜ. All Rights Reserved.
 *
 * NOTICE: All information contained herein is, and remains
 * the property of Thingsboard OÜ and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Thingsboard OÜ
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

import com.datastax.driver.core.querybuilder.Clause;
import com.datastax.driver.core.querybuilder.Ordering;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;
import com.datastax.driver.core.querybuilder.Select.Where;
import com.datastax.driver.core.utils.UUIDs;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.dao.model.BaseEntity;
import org.thingsboard.server.dao.model.ModelConstants;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static com.datastax.driver.core.querybuilder.QueryBuilder.select;

public abstract class CassandraAbstractSearchTimeDao<E extends BaseEntity<D>, D> extends CassandraAbstractModelDao<E, D> {


    protected List<E> findPageWithTimeSearch(String searchView, List<Clause> clauses, TimePageLink pageLink) {
        return findPageWithTimeSearch(searchView, clauses, Collections.emptyList(), pageLink);
    }

    protected List<E> findPageWithTimeSearch(String searchView, List<Clause> clauses, Ordering ordering, TimePageLink pageLink) {
        return findPageWithTimeSearch(searchView, clauses, Collections.singletonList(ordering), pageLink);
    }

    protected List<E> findPageWithTimeSearch(String searchView, List<Clause> clauses, List<Ordering> topLevelOrderings, TimePageLink pageLink) {
        return findPageWithTimeSearch(searchView, clauses, topLevelOrderings, pageLink, ModelConstants.ID_PROPERTY);
    }

    protected List<E> findPageWithTimeSearch(String searchView, List<Clause> clauses, TimePageLink pageLink, String idColumn) {
        return findPageWithTimeSearch(searchView, clauses, Collections.emptyList(), pageLink, idColumn);
    }

    protected List<E> findPageWithTimeSearch(String searchView, List<Clause> clauses, List<Ordering> topLevelOrderings, TimePageLink pageLink, String idColumn) {
        return findListByStatement(buildQuery(searchView, clauses, topLevelOrderings, pageLink, idColumn));
    }

    public static Where buildQuery(String searchView, List<Clause> clauses, TimePageLink pageLink, String idColumn) {
        return buildQuery(searchView, clauses, Collections.emptyList(), pageLink, idColumn);
    }

    public static Where buildQuery(String searchView, List<Clause> clauses, Ordering order, TimePageLink pageLink, String idColumn) {
        return buildQuery(searchView, clauses, Collections.singletonList(order), pageLink, idColumn);
    }

    public static Where buildQuery(String searchView, List<Clause> clauses, List<Ordering> topLevelOrderings, TimePageLink pageLink, String idColumn) {
        Select select = select().from(searchView);
        Where query = select.where();
        for (Clause clause : clauses) {
            query.and(clause);
        }
        query.limit(pageLink.getLimit());
        if (pageLink.isAscOrder()) {
            if (pageLink.getIdOffset() != null) {
                query.and(QueryBuilder.gt(idColumn, pageLink.getIdOffset()));
            } else if (pageLink.getStartTime() != null) {
                final UUID startOf = UUIDs.startOf(pageLink.getStartTime());
                query.and(QueryBuilder.gte(idColumn, startOf));
            }
            if (pageLink.getEndTime() != null) {
                final UUID endOf = UUIDs.endOf(pageLink.getEndTime());
                query.and(QueryBuilder.lte(idColumn, endOf));
            }
        } else {
            if (pageLink.getIdOffset() != null) {
                query.and(QueryBuilder.lt(idColumn, pageLink.getIdOffset()));
            } else if (pageLink.getEndTime() != null) {
                final UUID endOf = UUIDs.endOf(pageLink.getEndTime());
                query.and(QueryBuilder.lte(idColumn, endOf));
            }
            if (pageLink.getStartTime() != null) {
                final UUID startOf = UUIDs.startOf(pageLink.getStartTime());
                query.and(QueryBuilder.gte(idColumn, startOf));
            }
        }
        List<Ordering> orderings = new ArrayList<>(topLevelOrderings);
        if (pageLink.isAscOrder()) {
            orderings.add(QueryBuilder.asc(idColumn));
        } else {
            orderings.add(QueryBuilder.desc(idColumn));
        }
        query.orderBy(orderings.toArray(new Ordering[orderings.size()]));
        return query;
    }

}
