/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2024 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.service.cloud.rpc.processor;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.oauth2.OAuth2Info;
import org.thingsboard.server.gen.edge.v1.OAuth2UpdateMsg;
import org.thingsboard.server.service.edge.rpc.processor.BaseEdgeProcessor;

@Component
@Slf4j
public class OAuth2CloudProcessor extends BaseEdgeProcessor {

    public ListenableFuture<Void> processOAuth2MsgFromCloud(OAuth2UpdateMsg oAuth2UpdateMsg) {
        OAuth2Info oAuth2Info = JacksonUtil.fromString(oAuth2UpdateMsg.getEntity(), OAuth2Info.class, true);
        if (oAuth2Info == null) {
            throw new RuntimeException("[{" + TenantId.SYS_TENANT_ID + "}] oAuth2UpdateMsg {" + oAuth2UpdateMsg + "} cannot be converted to OAuth2Info");
        }
        if (oAuth2Info.isEnabled() && !oAuth2Info.isEdgeEnabled()) {
            oAuth2Info.setEnabled(false);
        }
        oAuth2Info.getOauth2ParamsInfos().forEach(pi -> pi.getClientRegistrations().forEach(cr -> {
            cr.getMapperConfig().setAllowUserCreation(false);
            cr.getMapperConfig().setActivateUser(false);
        }));
        oAuth2Service.saveOAuth2Info(oAuth2Info);
        return Futures.immediateFuture(null);
    }

}
