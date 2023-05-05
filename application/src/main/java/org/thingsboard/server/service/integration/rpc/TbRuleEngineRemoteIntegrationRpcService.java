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
package org.thingsboard.server.service.integration.rpc;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;
import org.thingsboard.integration.api.data.IntegrationDownlinkMsg;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.converter.Converter;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.queue.discovery.TbServiceInfoProvider;
import org.thingsboard.server.service.integration.RemoteIntegrationRpcService;

@RequiredArgsConstructor
@Service
@Slf4j
@ConditionalOnExpression("'${service.type:null}'=='tb-rule-engine' && '${integrations.rpc.enabled:false}'=='true'")
public class TbRuleEngineRemoteIntegrationRpcService implements RemoteIntegrationRpcService {

    private final TbServiceInfoProvider serviceInfoProvider;
    private final RemoteIntegrationSessionService sessionsCache;
    private final TbClusterService clusterService;

    @Override
    public void updateIntegration(Integration configuration) {
    }

    @Override
    public void updateConverter(Converter converter) {
    }

    @Override
    public boolean handleRemoteDownlink(IntegrationDownlinkMsg msg) {
        IntegrationSession remoteSession = sessionsCache.findIntegrationSession(msg.getIntegrationId());
        if (remoteSession != null && !remoteSession.getServiceId().equals(serviceInfoProvider.getServiceId())) {
            log.debug("[{}] Remote integration session found for [{}] downlink @ Server [{}].", msg.getIntegrationId(), msg.getEntityId(), remoteSession.getServiceId());
            clusterService.pushNotificationToCore(remoteSession.getServiceId(), msg, null);
            return true;
        }
        return false;
    }

}
