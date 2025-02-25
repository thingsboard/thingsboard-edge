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
package org.thingsboard.server.service.cloud.rpc.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.wl.LoginWhiteLabelingParams;
import org.thingsboard.server.common.data.wl.WhiteLabeling;
import org.thingsboard.server.common.data.wl.WhiteLabelingParams;
import org.thingsboard.server.common.data.wl.WhiteLabelingType;
import org.thingsboard.server.dao.exception.IncorrectParameterException;
import org.thingsboard.server.dao.wl.WhiteLabelingService;
import org.thingsboard.server.gen.edge.v1.WhiteLabelingProto;
import org.thingsboard.server.service.edge.rpc.processor.BaseEdgeProcessor;

@Component
@Slf4j
public class WhiteLabelingCloudProcessor extends BaseEdgeProcessor {

    @Autowired
    private WhiteLabelingService whiteLabelingService;

    public ListenableFuture<Void> processWhiteLabelingMsgFromCloud(TenantId tenantId, WhiteLabelingProto whiteLabelingProto) throws Exception {
        WhiteLabeling whiteLabeling = JacksonUtil.fromString(whiteLabelingProto.getEntity(), WhiteLabeling.class, true);
        if (whiteLabeling == null) {
            throw new RuntimeException("[{" + tenantId + "}] whiteLabelingProto {" + whiteLabelingProto + " } cannot be converted to white labeling");
        }
        boolean isSysAdmin = TenantId.SYS_TENANT_ID.equals(whiteLabeling.getTenantId());
        boolean isLogin = WhiteLabelingType.LOGIN.equals(whiteLabeling.getType());
        boolean isGeneral = WhiteLabelingType.GENERAL.equals(whiteLabeling.getType());
        if (whiteLabeling.getCustomerId() == null || whiteLabeling.getCustomerId().isNullUid()) {
            if (isLogin) {
                if (isSysAdmin) {
                    whiteLabelingService.saveSystemLoginWhiteLabelingParams(constructLoginWlParams(whiteLabeling.getSettings()));
                } else {
                    LoginWhiteLabelingParams loginWhiteLabelingParams = constructLoginWlParams(whiteLabeling.getSettings());
                    whiteLabelingService.saveTenantLoginWhiteLabelingParams(tenantId, loginWhiteLabelingParams);
                }
            } else if (isGeneral) {
                if (isSysAdmin) {
                    whiteLabelingService.saveSystemWhiteLabelingParams(constructWlParams(whiteLabeling.getSettings(), true));
                } else {
                    whiteLabelingService.saveTenantWhiteLabelingParams(tenantId, constructWlParams(whiteLabeling.getSettings(), false));
                }
            } else if (WhiteLabelingType.MAIL_TEMPLATES.equals(whiteLabeling.getType())) {
                whiteLabelingService.saveMailTemplates(isSysAdmin ? TenantId.SYS_TENANT_ID : tenantId, whiteLabeling.getSettings());
            }
        } else {
            if (isLogin) {
                LoginWhiteLabelingParams loginWhiteLabelingParams = constructLoginWlParams(whiteLabeling.getSettings());
                whiteLabelingService.saveCustomerLoginWhiteLabelingParams(tenantId, whiteLabeling.getCustomerId(), loginWhiteLabelingParams);
            } else if (isGeneral) {
                whiteLabelingService.saveCustomerWhiteLabelingParams(tenantId, whiteLabeling.getCustomerId(), constructWlParams(whiteLabeling.getSettings(), false));
            }
        }
        return Futures.immediateFuture(null);
    }

    private LoginWhiteLabelingParams constructLoginWlParams(JsonNode json) {
        LoginWhiteLabelingParams result = null;
        if (json != null) {
            try {
                result = JacksonUtil.treeToValue(json, LoginWhiteLabelingParams.class, true);
            } catch (IllegalArgumentException e) {
                log.error("Unable to read Login White Labeling Params from JSON!", e);
                throw new IncorrectParameterException("Unable to read Login White Labeling Params from JSON!");
            }
        }
        if (result == null) {
            result = new LoginWhiteLabelingParams();
        }
        return result;
    }

    private WhiteLabelingParams constructWlParams(JsonNode json, boolean isSystem) {
        WhiteLabelingParams result = null;
        if (json != null) {
            try {
                result = JacksonUtil.treeToValue(json, WhiteLabelingParams.class, true);
                if (isSystem) {
                    if (!json.has("helpLinkBaseUrl")) {
                        result.setHelpLinkBaseUrl("https://thingsboard.io");
                    }
                    if (!json.has("uiHelpBaseUrl")) {
                        result.setUiHelpBaseUrl(null);
                    }
                    if (!json.has("enableHelpLinks")) {
                        result.setEnableHelpLinks(true);
                    }
                }
            } catch (IllegalArgumentException e) {
                log.error("Unable to read White Labeling Params from JSON!", e);
                throw new IncorrectParameterException("Unable to read White Labeling Params from JSON!");
            }
        }
        if (result == null) {
            result = new WhiteLabelingParams();
            if (isSystem) {
                result.setHelpLinkBaseUrl("https://thingsboard.io");
                result.setUiHelpBaseUrl(null);
                result.setEnableHelpLinks(true);
            }
        }
        return result;
    }

}
