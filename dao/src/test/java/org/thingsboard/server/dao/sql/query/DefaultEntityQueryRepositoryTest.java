/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2021 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.dao.sql.query;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.support.TransactionTemplate;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = DefaultEntityQueryRepository.class)
public class DefaultEntityQueryRepositoryTest {

    @MockBean
    NamedParameterJdbcTemplate jdbcTemplate;
    @MockBean
    TransactionTemplate transactionTemplate;
    @MockBean
    DefaultQueryLogComponent queryLog;

    @Autowired
    DefaultEntityQueryRepository repo;

    /*
     * This value has to be reasonable small to prevent infinite recursion as early as possible
     * */
    @Test
    public void givenDefaultMaxLevel_whenStaticConstant_thenEqualsTo() {
        assertThat(repo.getMaxLevelAllowed(), equalTo(50));
    }

    @Test
    public void givenMaxLevelZeroOrNegative_whenGetMaxLevel_thenReturnDefaultMaxLevel() {
        assertThat(repo.getMaxLevel(0), equalTo(repo.getMaxLevelAllowed()));
        assertThat(repo.getMaxLevel(-1), equalTo(repo.getMaxLevelAllowed()));
        assertThat(repo.getMaxLevel(-2), equalTo(repo.getMaxLevelAllowed()));
        assertThat(repo.getMaxLevel(Integer.MIN_VALUE), equalTo(repo.getMaxLevelAllowed()));
    }

    @Test
    public void givenMaxLevelPositive_whenGetMaxLevel_thenValueTheSame() {
        assertThat(repo.getMaxLevel(1), equalTo(1));
        assertThat(repo.getMaxLevel(2), equalTo(2));
        assertThat(repo.getMaxLevel(repo.getMaxLevelAllowed()), equalTo(repo.getMaxLevelAllowed()));
        assertThat(repo.getMaxLevel(repo.getMaxLevelAllowed() + 1), equalTo(repo.getMaxLevelAllowed()));
        assertThat(repo.getMaxLevel(Integer.MAX_VALUE), equalTo(repo.getMaxLevelAllowed()));
    }

}
