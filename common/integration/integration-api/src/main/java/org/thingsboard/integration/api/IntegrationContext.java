/**
 * Copyright © 2016-2021 ThingsBoard, Inc.
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
package org.thingsboard.integration.api;

import com.fasterxml.jackson.databind.JsonNode;
import io.netty.channel.EventLoopGroup;
import org.thingsboard.integration.api.converter.ConverterContext;
import org.thingsboard.integration.api.data.DownLinkMsg;
import org.thingsboard.integration.api.data.IntegrationDownlinkMsg;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.gen.integration.AssetUplinkDataProto;
import org.thingsboard.server.gen.integration.DeviceUplinkDataProto;
import org.thingsboard.server.gen.integration.EntityViewDataProto;

import java.util.concurrent.ScheduledExecutorService;

/**
 * Created by ashvayka on 05.12.17.
 */
public interface IntegrationContext {

    /**
     * Returns current service id that is used mostly for logging.
     *
     * @return service id
     */
    String getServiceId();

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

    void processUplinkData(AssetUplinkDataProto uplinkData, IntegrationCallback<Void> callback);

    void createEntityView(EntityViewDataProto entityViewDataProto, IntegrationCallback<Void> callback);

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
    void saveEvent(String type, String uid, JsonNode body, IntegrationCallback<Void> callback);

    void saveRawDataEvent(String deviceName, String type, String uid, JsonNode body, IntegrationCallback<Void> callback);

    /**
     * Provides Netty Event loop group to be used by integrations in order to avoid creating separate threads per integration.
     *
     * @return event loop group
     */
    EventLoopGroup getEventLoopGroup();

    /**
     * Provides access to ScheduledExecutorService to schedule periodic tasks.
     * Allows using N threads per M integrations instead of using N threads per integration.
     *
     * @return scheduled executor
     */
    ScheduledExecutorService getScheduledExecutorService();

    DownLinkMsg getDownlinkMsg(String deviceName);

    DownLinkMsg putDownlinkMsg(IntegrationDownlinkMsg msg);

    void removeDownlinkMsg(String deviceName);

    //TODO @ashvayka: Implement
    boolean isClosed();

}
