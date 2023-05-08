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
package org.thingsboard.integration.remote;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.ByteString;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.integration.api.IntegrationCallback;
import org.thingsboard.integration.api.converter.ConverterContext;
import org.thingsboard.integration.storage.EventStorage;
import org.thingsboard.server.common.data.FSTUtils;
import org.thingsboard.server.common.data.event.Event;
import org.thingsboard.server.gen.integration.TbEventProto;
import org.thingsboard.server.gen.integration.TbEventSource;
import org.thingsboard.server.gen.integration.UplinkMsg;

@Data
@Slf4j
public class RemoteConverterContext implements ConverterContext {

    private final EventStorage eventStorage;
    private final boolean isUplink;
    private final ObjectMapper mapper;
    private final String clientId;
    private final int port;

    @Override
    public String getServiceId() {
        return "[" + clientId + ":" + port + "]";
    }

    @Override
    public void saveEvent(Event event, IntegrationCallback<Void> callback) {
        TbEventSource source;
        if (isUplink) {
            source = TbEventSource.UPLINK_CONVERTER;
        } else {
            source = TbEventSource.DOWNLINK_CONVERTER;
        }
        eventStorage.write(UplinkMsg.newBuilder()
                .addEventsData(TbEventProto.newBuilder()
                        .setSource(source)
                        .setEvent(ByteString.copyFrom(FSTUtils.encode(event)))
                        .build()
                ).build(), callback);
    }
}
