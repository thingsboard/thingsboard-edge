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
package org.thingsboard.integration.api;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.integration.IntegrationType;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnExpression("('${metrics.enabled:false}'=='false') && ('${service.type:null}'=='tb-integration' " +
        "|| '${service.type:null}'=='tb-integration-executor' || '${service.type:null}'=='monolith' || '${service.type:null}'=='tb-core')")
public class IntegrationStatisticsEmpty implements IntegrationStatisticsService {
    @Override
    public void onIntegrationMsgsStateSuccessCounterAdd(IntegrationType integrationType) {

    }

    @Override
    public void onIntegrationMsgsStateFailedCounterAdd(IntegrationType integrationType) {

    }

    @Override
    public void onIntegrationStateSuccessGauge(IntegrationType integrationType, int cntIntegration) {

    }

    @Override
    public void onIntegrationStateFailedGauge(IntegrationType integrationType, int cntIntegration) {

    }

    @Override
    public void onIntegrationMsgsUplinkSuccess(IntegrationType integrationType) {

    }

    @Override
    public void onIntegrationMsgsUplinkFailed(IntegrationType integrationType) {

    }

    @Override
    public void onIntegrationMsgsDownlinkSuccess(IntegrationType integrationType) {

    }

    @Override
    public void onIntegrationMsgsDownlinkFailed(IntegrationType integrationType) {

    }

    @Override
    public Map<IntegrationType, Long> getGaugesSuccess() {
        return null;
    }

    @Override
    public Map<IntegrationType, Long> getGaugesFailed() {
        return null;
    }

    @Override
    public void printStats() {

    }

    @Override
    public void reset() {

    }
}
