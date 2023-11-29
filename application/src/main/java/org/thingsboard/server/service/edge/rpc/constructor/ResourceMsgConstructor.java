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
package org.thingsboard.server.service.edge.rpc.constructor;

import com.google.protobuf.ByteString;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.TbResource;
import org.thingsboard.server.common.data.id.TbResourceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.gen.edge.v1.ResourceUpdateMsg;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;
import org.thingsboard.server.queue.util.TbCoreComponent;

@Component
@TbCoreComponent
public class ResourceMsgConstructor {

    public ResourceUpdateMsg constructResourceUpdatedMsg(UpdateMsgType msgType, TbResource tbResource) {
        ResourceUpdateMsg.Builder builder = ResourceUpdateMsg.newBuilder()
                .setMsgType(msgType)
                .setIdMSB(tbResource.getId().getId().getMostSignificantBits())
                .setIdLSB(tbResource.getId().getId().getLeastSignificantBits())
                .setTitle(tbResource.getTitle())
                .setResourceKey(tbResource.getResourceKey())
                .setResourceType(tbResource.getResourceType().name())
                .setFileName(tbResource.getFileName());
        if (tbResource.getData() != null) {
            builder.setData(tbResource.getEncodedData());
        }
        if (tbResource.getEtag() != null) {
            builder.setEtag(tbResource.getEtag());
        }
        if (tbResource.getDescriptor() != null) {
            builder.setDescriptorJson(tbResource.getDescriptor().toString());
        }
        if (tbResource.getPreview() != null) {
            builder.setPreview(ByteString.copyFrom(tbResource.getPreview()));
        }
        if (tbResource.getTenantId().equals(TenantId.SYS_TENANT_ID)) {
            builder.setIsSystem(true);
        }
        return builder.build();
    }

    public ResourceUpdateMsg constructResourceDeleteMsg(TbResourceId tbResourceId) {
        return ResourceUpdateMsg.newBuilder()
                .setMsgType(UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE)
                .setIdMSB(tbResourceId.getId().getMostSignificantBits())
                .setIdLSB(tbResourceId.getId().getLeastSignificantBits()).build();
    }
}
