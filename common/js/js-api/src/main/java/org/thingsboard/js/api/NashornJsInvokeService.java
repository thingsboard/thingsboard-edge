/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2022 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.js.api;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.stats.TbApiUsageReportClient;
import org.thingsboard.server.common.stats.TbApiUsageStateClient;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
@ConditionalOnProperty(prefix = "js", value = "evaluator", havingValue = "local", matchIfMissing = true)
@Service
public class NashornJsInvokeService extends AbstractNashornJsInvokeService {

    @Value("${js.local.use_js_sandbox}")
    private boolean useJsSandbox;

    @Getter
    @Value("${js.local.monitor_thread_pool_size}")
    private int monitorThreadPoolSize;

    @Getter
    @Value("${js.local.max_cpu_time}")
    private long maxCpuTime;

    @Getter
    @Value("${js.local.max_errors}")
    private int maxErrors;

    @Value("${js.local.max_black_list_duration_sec:60}")
    private int maxBlackListDurationSec;

    public NashornJsInvokeService(Optional<TbApiUsageStateClient> apiUsageStateClient, Optional<TbApiUsageReportClient> apiUsageReportClient) {
        super(apiUsageStateClient, apiUsageReportClient);
    }

    @Override
    protected boolean useJsSandbox() {
        return useJsSandbox;
    }

    @Override
    protected long getMaxBlacklistDuration() {
        return TimeUnit.SECONDS.toMillis(maxBlackListDurationSec);
    }
}
