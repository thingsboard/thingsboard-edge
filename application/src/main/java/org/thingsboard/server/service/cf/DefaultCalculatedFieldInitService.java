/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2025 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.service.cf;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thingsboard.server.actors.ActorSystemContext;
import org.thingsboard.server.common.data.ProfileEntityIdInfo;
import org.thingsboard.server.common.data.page.PageDataIterable;
import org.thingsboard.server.common.msg.cf.CalculatedFieldInitProfileEntityMsg;
import org.thingsboard.server.dao.asset.AssetService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.queue.util.AfterStartUp;
import org.thingsboard.server.queue.util.TbRuleEngineComponent;

@Slf4j
@Service
@TbRuleEngineComponent
@RequiredArgsConstructor
public class DefaultCalculatedFieldInitService implements CalculatedFieldInitService {

    private final AssetService assetService;
    private final DeviceService deviceService;
    private final ActorSystemContext actorSystemContext;

    @Value("${queue.calculated_fields.init_fetch_pack_size:50000}")
    @Getter
    private int initFetchPackSize;

    @AfterStartUp(order = AfterStartUp.CF_READ_PROFILE_ENTITIES_SERVICE)
    public void initCalculatedFieldDefinitions() {
        PageDataIterable<ProfileEntityIdInfo> deviceIdInfos = new PageDataIterable<>(deviceService::findProfileEntityIdInfos, initFetchPackSize);
        for (ProfileEntityIdInfo idInfo : deviceIdInfos) {
            log.trace("Processing device record: {}", idInfo);
            try {
                actorSystemContext.tell(new CalculatedFieldInitProfileEntityMsg(idInfo.getTenantId(), idInfo.getProfileId(), idInfo.getEntityId()));
            } catch (Exception e) {
                log.error("Failed to process device record: {}", idInfo, e);
            }
        }
        PageDataIterable<ProfileEntityIdInfo> assetIdInfos = new PageDataIterable<>(assetService::findProfileEntityIdInfos, initFetchPackSize);
        for (ProfileEntityIdInfo idInfo : assetIdInfos) {
            log.trace("Processing asset record: {}", idInfo);
            try {
                actorSystemContext.tell(new CalculatedFieldInitProfileEntityMsg(idInfo.getTenantId(), idInfo.getProfileId(), idInfo.getEntityId()));
            } catch (Exception e) {
                log.error("Failed to process asset record: {}", idInfo, e);
            }
        }
    }

}
