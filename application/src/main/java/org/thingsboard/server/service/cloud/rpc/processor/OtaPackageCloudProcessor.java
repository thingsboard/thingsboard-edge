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
package org.thingsboard.server.service.cloud.rpc.processor;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.protobuf.ByteString;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.DeviceProfileProvisionType;
import org.thingsboard.server.common.data.DeviceProfileType;
import org.thingsboard.server.common.data.DeviceTransportType;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.OtaPackage;
import org.thingsboard.server.common.data.OtaPackageInfo;
import org.thingsboard.server.common.data.cloud.CloudEvent;
import org.thingsboard.server.common.data.cloud.CloudEventType;
import org.thingsboard.server.common.data.device.profile.DeviceProfileData;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.OtaPackageId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.ota.ChecksumAlgorithm;
import org.thingsboard.server.common.data.ota.OtaPackageType;
import org.thingsboard.server.common.data.queue.Queue;
import org.thingsboard.server.dao.device.DeviceProfileService;
import org.thingsboard.server.dao.queue.QueueService;
import org.thingsboard.server.gen.edge.v1.DeviceProfileDevicesRequestMsg;
import org.thingsboard.server.gen.edge.v1.DeviceProfileUpdateMsg;
import org.thingsboard.server.gen.edge.v1.OtaPackageUpdateMsg;
import org.thingsboard.server.gen.edge.v1.UplinkMsg;
import org.thingsboard.server.queue.util.DataDecodingEncodingService;
import org.thingsboard.server.service.edge.rpc.processor.BaseEdgeProcessor;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Component
@Slf4j
public class OtaPackageCloudProcessor extends BaseEdgeProcessor {

    private final Lock otaPackageCreationLock = new ReentrantLock();

    public ListenableFuture<Void> processOtaPackageMsgFromCloud(TenantId tenantId, OtaPackageUpdateMsg otaPackageUpdateMsg) {
        OtaPackageId otaPackageId = new OtaPackageId(new UUID(otaPackageUpdateMsg.getIdMSB(), otaPackageUpdateMsg.getIdLSB()));
        switch (otaPackageUpdateMsg.getMsgType()) {
            case ENTITY_CREATED_RPC_MESSAGE:
            case ENTITY_UPDATED_RPC_MESSAGE:
                otaPackageCreationLock.lock();
                try {
                    OtaPackageInfo otaPackageInfo = otaPackageService.findOtaPackageInfoById(tenantId, otaPackageId);
                    if (otaPackageInfo == null) {
                        otaPackageInfo = new OtaPackageInfo();
                        otaPackageInfo.setId(otaPackageId);
                        otaPackageInfo.setCreatedTime(Uuids.unixTimestamp(otaPackageId.getId()));
                        otaPackageInfo.setTenantId(tenantId);
                    }
                    otaPackageInfo.setDeviceProfileId(new DeviceProfileId(new UUID(otaPackageUpdateMsg.getDeviceProfileIdMSB(), otaPackageUpdateMsg.getDeviceProfileIdLSB())));
                    otaPackageInfo.setType(OtaPackageType.valueOf(otaPackageUpdateMsg.getType()));
                    otaPackageInfo.setTitle(otaPackageUpdateMsg.getTitle());
                    otaPackageInfo.setVersion(otaPackageUpdateMsg.getVersion());
                    otaPackageInfo.setTag(otaPackageUpdateMsg.getTag());
                    if (otaPackageUpdateMsg.hasUrl()) {
                        otaPackageInfo.setUrl(otaPackageUpdateMsg.getUrl());
                    }
                    if (otaPackageUpdateMsg.hasFileName()) {
                        otaPackageInfo.setFileName(otaPackageUpdateMsg.getFileName());
                    }
                    if (otaPackageUpdateMsg.hasContentType()) {
                        otaPackageInfo.setContentType(otaPackageUpdateMsg.getContentType());
                    }
                    if (otaPackageUpdateMsg.hasChecksumAlgorithm()) {
                        otaPackageInfo.setChecksumAlgorithm(ChecksumAlgorithm.valueOf(otaPackageUpdateMsg.getChecksumAlgorithm()));
                    }
                    if (otaPackageUpdateMsg.hasChecksum()) {
                        otaPackageInfo.setChecksum(otaPackageUpdateMsg.getChecksum());
                    }
                    if (otaPackageUpdateMsg.hasDataSize()) {
                        otaPackageInfo.setDataSize(otaPackageUpdateMsg.getDataSize());
                    }
                    if (otaPackageUpdateMsg.hasAdditionalInfo()) {
                        otaPackageInfo.setAdditionalInfo(JacksonUtil.toJsonNode(otaPackageUpdateMsg.getAdditionalInfo()));
                    }
                    otaPackageService.saveOtaPackageInfo(otaPackageInfo, otaPackageUpdateMsg.hasUrl(), false);
                    if (otaPackageUpdateMsg.hasData()) {
                        OtaPackage otaPackage = otaPackageService.findOtaPackageById(tenantId, otaPackageId);
                        otaPackage.setData(ByteBuffer.wrap(otaPackageUpdateMsg.getData().toByteArray()));
                        otaPackageService.saveOtaPackage(otaPackage, false);
                    }
                } finally {
                    otaPackageCreationLock.unlock();
                }
                break;
            case ENTITY_DELETED_RPC_MESSAGE:
                OtaPackage otaPackage = otaPackageService.findOtaPackageById(tenantId, otaPackageId);
                if (otaPackage != null) {
                    otaPackageService.deleteOtaPackage(tenantId, otaPackageId);
                }
                break;
            case UNRECOGNIZED:
                return handleUnsupportedMsgType(otaPackageUpdateMsg.getMsgType());
        }
        return Futures.immediateFuture(null);
    }
}
