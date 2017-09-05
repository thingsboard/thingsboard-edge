/**
 * Thingsboard OÜ ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2017 Thingsboard OÜ. All Rights Reserved.
 *
 * NOTICE: All information contained herein is, and remains
 * the property of Thingsboard OÜ and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Thingsboard OÜ
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
package org.thingsboard.server.extensions.rest.plugin;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.thingsboard.server.common.data.id.RuleId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.core.BasicStatusCodeResponse;
import org.thingsboard.server.extensions.api.plugins.PluginContext;
import org.thingsboard.server.extensions.api.plugins.handlers.RuleMsgHandler;
import org.thingsboard.server.extensions.api.plugins.msg.ResponsePluginToRuleMsg;
import org.thingsboard.server.extensions.api.plugins.msg.RuleToPluginMsg;
import org.thingsboard.server.extensions.api.rules.RuleException;
import org.thingsboard.server.extensions.rest.action.RestApiCallActionMsg;
import org.thingsboard.server.extensions.rest.action.RestApiCallActionPayload;

@RequiredArgsConstructor
public class RestApiCallMsgHandler implements RuleMsgHandler {

    private final String baseUrl;
    private final HttpHeaders headers;

    @Override
    public void process(PluginContext ctx, TenantId tenantId, RuleId ruleId, RuleToPluginMsg<?> msg) throws RuleException {
        if (!(msg instanceof RestApiCallActionMsg)) {
            throw new RuleException("Unsupported message type " + msg.getClass().getName() + "!");
        }
        RestApiCallActionPayload payload = ((RestApiCallActionMsg)msg).getPayload();
        try {
            ResponseEntity<String> exchangeResponse = new RestTemplate().exchange(
                    baseUrl + payload.getActionPath(),
                    payload.getHttpMethod(),
                    new HttpEntity<>(payload.getMsgBody(), headers),
                    String.class);
            if (exchangeResponse.getStatusCode().equals(payload.getExpectedResultCode()) && payload.isSync()) {
                ctx.reply(new ResponsePluginToRuleMsg(msg.getUid(), tenantId, ruleId,
                        BasicStatusCodeResponse.onSuccess(payload.getMsgType(), payload.getRequestId())));
            } else if(!exchangeResponse.getStatusCode().equals(payload.getExpectedResultCode())) {
                throw new RuntimeException("Response Status Code '"
                        + exchangeResponse.getStatusCode()
                        + "' doesn't equals to Expected Status Code '"
                        + payload.getExpectedResultCode() + "'");
            }

        } catch (RestClientException e) {
            throw new RuleException(e.getMessage(), e);
        }
    }
}
