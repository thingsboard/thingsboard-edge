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
package org.thingsboard.server.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.Assert;
import org.junit.Test;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.queue.ProcessingStrategy;
import org.thingsboard.server.common.data.queue.ProcessingStrategyType;
import org.thingsboard.server.common.data.queue.Queue;
import org.thingsboard.server.common.data.queue.SubmitStrategy;
import org.thingsboard.server.common.data.queue.SubmitStrategyType;
import org.thingsboard.server.dao.service.DaoSqlTest;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DaoSqlTest
public class BaseQueueControllerTest extends AbstractControllerTest {

    @Test
    public void testQueueWithServiceTypeRE() throws Exception {
        loginSysAdmin();

        // create queue
        Queue queue = new Queue();
        queue.setName("qwerty");
        queue.setTopic("tb_rule_engine.qwerty");
        queue.setPollInterval(25);
        queue.setPartitions(10);
        queue.setTenantId(TenantId.SYS_TENANT_ID);
        queue.setConsumerPerPartition(false);
        queue.setPackProcessingTimeout(2000);
        SubmitStrategy submitStrategy = new SubmitStrategy();
        submitStrategy.setType(SubmitStrategyType.SEQUENTIAL_BY_ORIGINATOR);
        queue.setSubmitStrategy(submitStrategy);
        ProcessingStrategy processingStrategy = new ProcessingStrategy();
        processingStrategy.setType(ProcessingStrategyType.RETRY_ALL);
        processingStrategy.setRetries(3);
        processingStrategy.setFailurePercentage(0.7);
        processingStrategy.setPauseBetweenRetries(3);
        processingStrategy.setMaxPauseBetweenRetries(5);
        queue.setProcessingStrategy(processingStrategy);

        // create queue
        Queue queue2 = new Queue();
        queue2.setName("qwerty2");
        queue2.setTopic("tb_rule_engine.qwerty2");
        queue2.setPollInterval(25);
        queue2.setPartitions(10);
        queue2.setTenantId(TenantId.SYS_TENANT_ID);
        queue2.setConsumerPerPartition(false);
        queue2.setPackProcessingTimeout(2000);
        submitStrategy.setType(SubmitStrategyType.SEQUENTIAL_BY_ORIGINATOR);
        queue2.setSubmitStrategy(submitStrategy);
        processingStrategy.setType(ProcessingStrategyType.RETRY_ALL);
        processingStrategy.setRetries(3);
        processingStrategy.setFailurePercentage(0.7);
        processingStrategy.setPauseBetweenRetries(3);
        processingStrategy.setMaxPauseBetweenRetries(5);
        queue2.setProcessingStrategy(processingStrategy);

        Queue savedQueue = doPost("/api/queues?serviceType=" + "TB-RULE-ENGINE", queue, Queue.class);
        Queue savedQueue2 = doPost("/api/queues?serviceType=" + "TB_RULE_ENGINE", queue2, Queue.class);

        PageLink pageLink = new PageLink(10);
        PageData<Queue> pageData;
        pageData = doGetTypedWithPageLink("/api/queues?serviceType=TB-RULE-ENGINE&", new TypeReference<>() {
        }, pageLink);
        Assert.assertFalse(pageData.getData().isEmpty());
        doDelete("/api/queues/" + savedQueue.getUuidId())
                .andExpect(status().isOk());

        pageData = doGetTypedWithPageLink("/api/queues?serviceType=TB_RULE_ENGINE&", new TypeReference<>() {
        }, pageLink);
        Assert.assertFalse(pageData.getData().isEmpty());
        doDelete("/api/queues/" + savedQueue2.getUuidId())
                .andExpect(status().isOk());
    }

}
