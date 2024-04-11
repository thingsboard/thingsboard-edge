/**
 * Copyright © 2016-2024 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
