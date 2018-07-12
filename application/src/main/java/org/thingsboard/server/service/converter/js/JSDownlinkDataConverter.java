/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2018 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.service.converter.js;

import com.fasterxml.jackson.databind.JsonNode;
import org.thingsboard.server.common.data.converter.Converter;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.service.converter.AbstractDownlinkDataConverter;
import org.thingsboard.server.service.converter.IntegrationMetaData;
import org.thingsboard.server.service.script.JsSandboxService;

/**
 * Created by ashvayka on 02.12.17.
 */
public class JSDownlinkDataConverter extends AbstractDownlinkDataConverter {

    private final JsSandboxService sandboxService;
    private JSDownlinkEvaluator evaluator;

    public JSDownlinkDataConverter(JsSandboxService sandboxService) {
        this.sandboxService = sandboxService;
    }

    @Override
    public void init(Converter configuration) {
        super.init(configuration);
        String encoder = configuration.getConfiguration().get("encoder").asText();
        this.evaluator = new JSDownlinkEvaluator(sandboxService, encoder);
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
    protected JsonNode doConvertDownlink(TbMsg msg, IntegrationMetaData metadata) throws Exception {
        return evaluator.execute(msg, metadata);
    }

}
