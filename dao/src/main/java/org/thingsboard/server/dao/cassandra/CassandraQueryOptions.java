/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2018 ThingsBoard, Inc.. All Rights Reserved.
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
package org.thingsboard.server.dao.cassandra;

import com.datastax.driver.core.ConsistencyLevel;
import com.datastax.driver.core.QueryOptions;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.thingsboard.server.dao.util.NoSqlDao;

import javax.annotation.PostConstruct;

@Component
@Configuration
@Data
@NoSqlDao
public class CassandraQueryOptions {

    @Value("${cassandra.query.default_fetch_size}")
    private Integer defaultFetchSize;
    @Value("${cassandra.query.read_consistency_level}")
    private String readConsistencyLevel;
    @Value("${cassandra.query.write_consistency_level}")
    private String writeConsistencyLevel;

    private QueryOptions opts;

    private ConsistencyLevel defaultReadConsistencyLevel;
    private ConsistencyLevel defaultWriteConsistencyLevel;

    @PostConstruct
    public void initOpts(){
        opts = new QueryOptions();
        opts.setFetchSize(defaultFetchSize);
    }

    protected ConsistencyLevel getDefaultReadConsistencyLevel() {
        if (defaultReadConsistencyLevel == null) {
            if (readConsistencyLevel != null) {
                defaultReadConsistencyLevel = ConsistencyLevel.valueOf(readConsistencyLevel.toUpperCase());
            } else {
                defaultReadConsistencyLevel = ConsistencyLevel.ONE;
            }
        }
        return defaultReadConsistencyLevel;
    }

    protected ConsistencyLevel getDefaultWriteConsistencyLevel() {
        if (defaultWriteConsistencyLevel == null) {
            if (writeConsistencyLevel != null) {
                defaultWriteConsistencyLevel = ConsistencyLevel.valueOf(writeConsistencyLevel.toUpperCase());
            } else {
                defaultWriteConsistencyLevel = ConsistencyLevel.ONE;
            }
        }
        return defaultWriteConsistencyLevel;
    }
}
