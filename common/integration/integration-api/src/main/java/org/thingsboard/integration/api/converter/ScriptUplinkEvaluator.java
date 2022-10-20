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
package org.thingsboard.integration.api.converter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Base64Utils;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.integration.api.data.UplinkMetaData;
import org.thingsboard.script.api.ScriptInvokeService;
import org.thingsboard.script.api.ScriptType;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;

@Slf4j
public class ScriptUplinkEvaluator extends AbstractJSEvaluator {

    private static final ObjectMapper mapper = new ObjectMapper();

    public ScriptUplinkEvaluator(TenantId tenantId, ScriptInvokeService jsInvokeService, EntityId entityId, String script) {
        super(tenantId, jsInvokeService, entityId, ScriptType.UPLINK_CONVERTER_SCRIPT, script);
    }

    public ListenableFuture<String> execute(byte[] data, UplinkMetaData metadata) throws Exception {
        validateSuccessfulScriptLazyInit();
        String[] inArgs = prepareArgs(data, metadata);
        return Futures.transform(scriptInvokeService.invokeScript(tenantId, null, this.scriptId, inArgs[0], inArgs[1]),
                eval -> {
                    if (eval instanceof String) {
                        return eval.toString();
                    } else {
                        return JacksonUtil.toString(eval);
                    }
                }, MoreExecutors.directExecutor());
    }


    //TODO MVEL: adopt to MVEL reality
    private static String[] prepareArgs(byte[] data, UplinkMetaData metadata) {
        try {
            String[] args = new String[2];
            args[0] = Base64Utils.encodeToString(data);
            args[1] = mapper.writeValueAsString(metadata.getKvMap());
            return args;
        } catch (Throwable th) {
            throw new IllegalArgumentException("Cannot bind js args", th);
        }
    }

}
