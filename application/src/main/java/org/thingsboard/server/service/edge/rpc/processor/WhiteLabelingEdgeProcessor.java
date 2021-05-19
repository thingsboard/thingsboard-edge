/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2021 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.service.edge.rpc.processor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.translation.CustomTranslation;
import org.thingsboard.server.common.data.wl.LoginWhiteLabelingParams;
import org.thingsboard.server.common.data.wl.WhiteLabelingParams;
import org.thingsboard.server.gen.edge.CustomTranslationProto;
import org.thingsboard.server.gen.edge.DownlinkMsg;
import org.thingsboard.server.gen.edge.LoginWhiteLabelingParamsProto;
import org.thingsboard.server.gen.edge.WhiteLabelingParamsProto;
import org.thingsboard.server.queue.util.TbCoreComponent;

import java.util.Collections;

@Component
@Slf4j
@TbCoreComponent
public class WhiteLabelingEdgeProcessor extends BaseEdgeProcessor {

    public DownlinkMsg processWhiteLabelingToEdge(EdgeEvent edgeEvent) {
        WhiteLabelingParams whiteLabelingParams = mapper.convertValue(edgeEvent.getBody(), WhiteLabelingParams.class);
        WhiteLabelingParamsProto whiteLabelingParamsProto =
                whiteLabelingParamsProtoConstructor.constructWhiteLabelingParamsProto(whiteLabelingParams);
        return DownlinkMsg.newBuilder()
                .addAllWhiteLabelingParams(Collections.singletonList(whiteLabelingParamsProto))
                .build();
    }

    public DownlinkMsg processLoginWhiteLabelingToEdge(EdgeEvent edgeEvent) {
        LoginWhiteLabelingParams loginWhiteLabelingParams = mapper.convertValue(edgeEvent.getBody(), LoginWhiteLabelingParams.class);
        LoginWhiteLabelingParamsProto loginWhiteLabelingParamsProto =
                whiteLabelingParamsProtoConstructor.constructLoginWhiteLabelingParamsProto(loginWhiteLabelingParams);
        return DownlinkMsg.newBuilder()
                .addAllLoginWhiteLabelingParams(Collections.singletonList(loginWhiteLabelingParamsProto))
                .build();
    }

    public DownlinkMsg processCustomTranslationToEdge(EdgeEvent edgeEvent) {
        CustomTranslation customTranslation = mapper.convertValue(edgeEvent.getBody(), CustomTranslation.class);
        CustomTranslationProto customTranslationProto =
                customTranslationProtoConstructor.constructCustomTranslationProto(customTranslation);
        return DownlinkMsg.newBuilder()
                .addAllCustomTranslationMsg(Collections.singletonList(customTranslationProto))
                .build();
    }
}
