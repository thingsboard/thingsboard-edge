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

import com.google.common.util.concurrent.ListenableFuture;
import lombok.RequiredArgsConstructor;
import org.thingsboard.integration.api.data.UplinkMetaData;
import org.thingsboard.integration.api.util.LogSettingsComponent;
import org.thingsboard.js.api.JsInvokeService;
import org.thingsboard.server.common.data.converter.Converter;

/**
 * Created by ashvayka on 02.12.17.
 */
@RequiredArgsConstructor
public class JSUplinkDataConverter extends AbstractUplinkDataConverter {

    private final JsInvokeService jsInvokeService;
    private final LogSettingsComponent logSettings;
    private JSUplinkEvaluator evaluator;

    @Override
    public void init(Converter configuration) {
        super.init(configuration);
        String decoder = configuration.getConfiguration().get("decoder").asText();
        this.evaluator = new JSUplinkEvaluator(configuration.getTenantId(), jsInvokeService,  configuration.getId(), decoder);
    }

    @Override
    public void update(Converter configuration) {
        destroy();
        init(configuration);
    }

    @Override
    public void destroy() {
        if (this.evaluator != null) {
            this.evaluator.destroy();
        }
    }

    @Override
    public ListenableFuture<String> doConvertUplink(byte[] data, UplinkMetaData metadata) throws Exception {
        return evaluator.execute(data, metadata);
    }

    @Override
    boolean isExceptionStackTraceEnabled() {
        return logSettings.isExceptionStackTraceEnabled();
    }

}
