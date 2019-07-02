/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2019 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.integration.api;

import com.fasterxml.jackson.databind.JsonNode;
import io.netty.channel.EventLoopGroup;
import org.thingsboard.integration.api.converter.ConverterContext;
import org.thingsboard.integration.api.data.DownLinkMsg;
import org.thingsboard.integration.api.data.IntegrationDownlinkMsg;
import org.thingsboard.server.common.data.Event;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.cluster.ServerAddress;
import org.thingsboard.server.gen.integration.DeviceUplinkDataProto;

/**
 * Created by ashvayka on 05.12.17.
 */
public interface IntegrationContext {

    /**
     * Returns current server address that is used mostly for logging.
     *
     * @return server address
     */
    ServerAddress getServerAddress();

    /**
     * Returns context of execution for uplink data converter
     *
     * @return
     */
    ConverterContext getUplinkConverterContext();

    /**
     * Returns context of execution for downlink data converter
     *
     * @return
     */
    ConverterContext getDownlinkConverterContext();

    /**
     * Processes the uplink data and executes callback with result. The uplink data is pushed to queue for delivery.
     * Callback is executed when uplink data is queued successfully.
     *
     * @return
     */
    void processUplinkData(DeviceUplinkDataProto uplinkData, IntegrationCallback<Void> callback);

    /**
     * Dispatch custom message to the rule engine.
     * Note that msg originator is verified to be either tenantId or integrationId or any device/asset that belongs to the corresponding tenant.
     *
     * @param msg - custom message to dispatch
     */
    void processCustomMsg(TbMsg msg, IntegrationCallback<Void> callback);

    /**
     * Saves event to ThingsBoard based on provided type and body on behalf of the integration
     */
    void saveEvent(String type, JsonNode body, IntegrationCallback<Void> callback);

    /**
     * Provides Netty Event loop group to be used by integrations in order to avoid creating separate threads per integration.
     *
     * @return event loop group
     */
    EventLoopGroup getEventLoopGroup();

    DownLinkMsg getDownlinkMsg(String deviceName);

    DownLinkMsg putDownlinkMsg(IntegrationDownlinkMsg msg);

    void removeDownlinkMsg(String deviceName);

    //TODO @ashvayka: Implement
    boolean isClosed();

}
