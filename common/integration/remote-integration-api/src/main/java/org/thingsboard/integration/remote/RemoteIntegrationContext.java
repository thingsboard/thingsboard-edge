package org.thingsboard.integration.remote;

import com.fasterxml.jackson.databind.JsonNode;
import io.netty.channel.EventLoopGroup;
import org.thingsboard.integration.api.IntegrationCallback;
import org.thingsboard.integration.api.IntegrationContext;
import org.thingsboard.integration.api.converter.ConverterContext;
import org.thingsboard.integration.api.data.DownLinkMsg;
import org.thingsboard.integration.api.data.IntegrationDownlinkMsg;
import org.thingsboard.server.common.data.Event;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.cluster.ServerAddress;
import org.thingsboard.server.gen.integration.DeviceUplinkDataProto;

//TODO: we will implement together.
public class RemoteIntegrationContext implements IntegrationContext {

    protected final RemoteIntegrationService service;
    protected final Integration configuration;

    public RemoteIntegrationContext(RemoteIntegrationService service, Integration configuration) {
        this.service = service;
        this.configuration = configuration;
    }

    @Override
    public ServerAddress getServerAddress() {
        return null;
    }

    @Override
    public ConverterContext getUplinkConverterContext() {
        return null;
    }

    @Override
    public ConverterContext getDownlinkConverterContext() {
        return null;
    }

    @Override
    public void processUplinkData(DeviceUplinkDataProto uplinkData, IntegrationCallback<Void> callback) {

    }

    @Override
    public void processCustomMsg(TbMsg msg) {

    }

    @Override
    public void saveEvent(String type, JsonNode body, IntegrationCallback<Event> callback) {

    }

    @Override
    public EventLoopGroup getEventLoopGroup() {
        return null;
    }

    @Override
    public DownLinkMsg getDownlinkMsg(String deviceName) {
        return null;
    }

    @Override
    public DownLinkMsg putDownlinkMsg(IntegrationDownlinkMsg msg) {
        return null;
    }

    @Override
    public void removeDownlinkMsg(String deviceName) {

    }

    @Override
    public boolean isClosed() {
        return false;
    }
}
