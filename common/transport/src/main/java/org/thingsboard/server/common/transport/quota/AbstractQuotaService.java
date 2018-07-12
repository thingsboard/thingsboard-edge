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
package org.thingsboard.server.common.transport.quota;

import org.thingsboard.server.common.transport.quota.inmemory.IntervalRegistryCleaner;
import org.thingsboard.server.common.transport.quota.inmemory.IntervalRegistryLogger;
import org.thingsboard.server.common.transport.quota.inmemory.KeyBasedIntervalRegistry;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

public class AbstractQuotaService implements QuotaService {

    private final KeyBasedIntervalRegistry requestRegistry;
    private final RequestLimitPolicy requestsPolicy;
    private final IntervalRegistryCleaner registryCleaner;
    private final IntervalRegistryLogger registryLogger;
    private final boolean enabled;

    public AbstractQuotaService(KeyBasedIntervalRegistry requestRegistry, RequestLimitPolicy requestsPolicy,
                                    IntervalRegistryCleaner registryCleaner, IntervalRegistryLogger registryLogger,
                                    boolean enabled) {
        this.requestRegistry = requestRegistry;
        this.requestsPolicy = requestsPolicy;
        this.registryCleaner = registryCleaner;
        this.registryLogger = registryLogger;
        this.enabled = enabled;
    }

    @PostConstruct
    public void init() {
        if (enabled) {
            registryCleaner.schedule();
            registryLogger.schedule();
        }
    }

    @PreDestroy
    public void close() {
        if (enabled) {
            registryCleaner.stop();
            registryLogger.stop();
        }
    }

    @Override
    public boolean isQuotaExceeded(String key) {
        if (enabled) {
            long count = requestRegistry.tick(key);
            return !requestsPolicy.isValid(count);
        }
        return false;
    }
}
