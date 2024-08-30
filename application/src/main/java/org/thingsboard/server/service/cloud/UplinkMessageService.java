package org.thingsboard.server.service.cloud;

import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.gen.edge.v1.UplinkResponseMsg;

public interface UplinkMessageService {

    void processHandleMessages(TenantId tenantId) throws Exception;

    void onUplinkResponse(UplinkResponseMsg msg);
}
