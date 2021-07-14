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
package org.thingsboard.server.controller;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.OtaPackage;
import org.thingsboard.server.common.data.OtaPackageInfo;
import org.thingsboard.server.common.data.SaveOtaPackageInfoRequest;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.OtaPackageId;
import org.thingsboard.server.common.data.ota.ChecksumAlgorithm;
import org.thingsboard.server.common.data.ota.OtaPackageType;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.common.data.permission.Resource;
import org.thingsboard.server.queue.util.TbCoreComponent;

import java.nio.ByteBuffer;

@Slf4j
@RestController
@TbCoreComponent
@RequestMapping("/api")
public class OtaPackageController extends BaseController {

    public static final String OTA_PACKAGE_ID = "otaPackageId";
    public static final String CHECKSUM_ALGORITHM = "checksumAlgorithm";

    @PreAuthorize("hasAnyAuthority( 'TENANT_ADMIN')")
    @RequestMapping(value = "/otaPackage/{otaPackageId}/download", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<org.springframework.core.io.Resource> downloadOtaPackage(@PathVariable(OTA_PACKAGE_ID) String strOtaPackageId) throws ThingsboardException {
        checkParameter(OTA_PACKAGE_ID, strOtaPackageId);
        try {
            OtaPackageId otaPackageId = new OtaPackageId(toUUID(strOtaPackageId));
            OtaPackage otaPackage = checkOtaPackageId(otaPackageId, Operation.READ);

            if (otaPackage.hasUrl()) {
                return ResponseEntity.badRequest().build();
            }

            ByteArrayResource resource = new ByteArrayResource(otaPackage.getData().array());
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=" + otaPackage.getFileName())
                    .header("x-filename", otaPackage.getFileName())
                    .contentLength(resource.contentLength())
                    .contentType(parseMediaType(otaPackage.getContentType()))
                    .body(resource);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/otaPackage/info/{otaPackageId}", method = RequestMethod.GET)
    @ResponseBody
    public OtaPackageInfo getOtaPackageInfoById(@PathVariable(OTA_PACKAGE_ID) String strOtaPackageId) throws ThingsboardException {
        checkParameter(OTA_PACKAGE_ID, strOtaPackageId);
        try {
            OtaPackageId otaPackageId = new OtaPackageId(toUUID(strOtaPackageId));
            return checkNotNull(otaPackageService.findOtaPackageInfoById(getTenantId(), otaPackageId));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/otaPackage/{otaPackageId}", method = RequestMethod.GET)
    @ResponseBody
    public OtaPackage getOtaPackageById(@PathVariable(OTA_PACKAGE_ID) String strOtaPackageId) throws ThingsboardException {
        checkParameter(OTA_PACKAGE_ID, strOtaPackageId);
        try {
            OtaPackageId otaPackageId = new OtaPackageId(toUUID(strOtaPackageId));
            return checkOtaPackageId(otaPackageId, Operation.READ);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/otaPackage", method = RequestMethod.POST)
    @ResponseBody
    public OtaPackageInfo saveOtaPackageInfo(@RequestBody SaveOtaPackageInfoRequest otaPackageInfo) throws ThingsboardException {
        boolean created = otaPackageInfo.getId() == null;
        try {
            otaPackageInfo.setTenantId(getTenantId());
            checkEntity(otaPackageInfo.getId(), otaPackageInfo, Resource.OTA_PACKAGE, null);
            OtaPackageInfo savedOtaPackageInfo = otaPackageService.saveOtaPackageInfo(new OtaPackageInfo(otaPackageInfo), otaPackageInfo.isUsesUrl());
            logEntityAction(savedOtaPackageInfo.getId(), savedOtaPackageInfo,
                    null, created ? ActionType.ADDED : ActionType.UPDATED, null);
            return savedOtaPackageInfo;
        } catch (Exception e) {
            logEntityAction(emptyId(EntityType.OTA_PACKAGE), otaPackageInfo,
                    null, created ? ActionType.ADDED : ActionType.UPDATED, e);
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/otaPackage/{otaPackageId}", method = RequestMethod.POST)
    @ResponseBody
    public OtaPackageInfo saveOtaPackageData(@PathVariable(OTA_PACKAGE_ID) String strOtaPackageId,
                                         @RequestParam(required = false) String checksum,
                                         @RequestParam(CHECKSUM_ALGORITHM) String checksumAlgorithmStr,
                                         @RequestBody MultipartFile file) throws ThingsboardException {
        checkParameter(OTA_PACKAGE_ID, strOtaPackageId);
        checkParameter(CHECKSUM_ALGORITHM, checksumAlgorithmStr);
        try {
            OtaPackageId otaPackageId = new OtaPackageId(toUUID(strOtaPackageId));
            OtaPackageInfo info = checkOtaPackageInfoId(otaPackageId, Operation.READ);

            OtaPackage otaPackage = new OtaPackage(otaPackageId);
            otaPackage.setCreatedTime(info.getCreatedTime());
            otaPackage.setTenantId(getTenantId());
            otaPackage.setDeviceProfileId(info.getDeviceProfileId());
            otaPackage.setType(info.getType());
            otaPackage.setTitle(info.getTitle());
            otaPackage.setVersion(info.getVersion());
            otaPackage.setAdditionalInfo(info.getAdditionalInfo());

            ChecksumAlgorithm checksumAlgorithm = ChecksumAlgorithm.valueOf(checksumAlgorithmStr.toUpperCase());

            byte[] bytes = file.getBytes();
            if (StringUtils.isEmpty(checksum)) {
                checksum = otaPackageService.generateChecksum(checksumAlgorithm, ByteBuffer.wrap(bytes));
            }

            otaPackage.setChecksumAlgorithm(checksumAlgorithm);
            otaPackage.setChecksum(checksum);
            otaPackage.setFileName(file.getOriginalFilename());
            otaPackage.setContentType(file.getContentType());
            otaPackage.setData(ByteBuffer.wrap(bytes));
            otaPackage.setDataSize((long) bytes.length);
            OtaPackageInfo savedOtaPackage = otaPackageService.saveOtaPackage(otaPackage);
            logEntityAction(savedOtaPackage.getId(), savedOtaPackage, null, ActionType.UPDATED, null);
            return savedOtaPackage;
        } catch (Exception e) {
            logEntityAction(emptyId(EntityType.OTA_PACKAGE), null, null, ActionType.UPDATED, e, strOtaPackageId);
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/otaPackages", method = RequestMethod.GET)
    @ResponseBody
    public PageData<OtaPackageInfo> getOtaPackages(@RequestParam int pageSize,
                                                   @RequestParam int page,
                                                   @RequestParam(required = false) String textSearch,
                                                   @RequestParam(required = false) String sortProperty,
                                                   @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        try {
            PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
            return checkNotNull(otaPackageService.findTenantOtaPackagesByTenantId(getTenantId(), pageLink));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/otaPackages/{deviceProfileId}/{type}", method = RequestMethod.GET)
    @ResponseBody
    public PageData<OtaPackageInfo> getOtaPackages(@PathVariable("deviceProfileId") String strDeviceProfileId,
                                                   @PathVariable("type") String strType,
                                                   @RequestParam int pageSize,
                                                   @RequestParam int page,
                                                   @RequestParam(required = false) String textSearch,
                                                   @RequestParam(required = false) String sortProperty,
                                                   @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        checkParameter("deviceProfileId", strDeviceProfileId);
        checkParameter("type", strType);
        try {
            PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
            return checkNotNull(otaPackageService.findTenantOtaPackagesByTenantIdAndDeviceProfileIdAndTypeAndHasData(getTenantId(),
                    new DeviceProfileId(toUUID(strDeviceProfileId)), OtaPackageType.valueOf(strType), pageLink));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/otaPackage/{otaPackageId}", method = RequestMethod.DELETE)
    @ResponseBody
    public void deleteOtaPackage(@PathVariable("otaPackageId") String strOtaPackageId) throws ThingsboardException {
        checkParameter(OTA_PACKAGE_ID, strOtaPackageId);
        try {
            OtaPackageId otaPackageId = new OtaPackageId(toUUID(strOtaPackageId));
            OtaPackageInfo info = checkOtaPackageInfoId(otaPackageId, Operation.DELETE);
            otaPackageService.deleteOtaPackage(getTenantId(), otaPackageId);
            logEntityAction(otaPackageId, info, null, ActionType.DELETED, null, strOtaPackageId);
        } catch (Exception e) {
            logEntityAction(emptyId(EntityType.OTA_PACKAGE), null, null, ActionType.DELETED, e, strOtaPackageId);
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/otaPackages/group/{groupId}/{type}", method = RequestMethod.GET)
    @ResponseBody
    public PageData<OtaPackageInfo> getGroupOtaPackages(@PathVariable("groupId") String strGroupId,
                                                   @PathVariable("type") String strType,
                                                   @RequestParam int pageSize,
                                                   @RequestParam int page,
                                                   @RequestParam(required = false) String textSearch,
                                                   @RequestParam(required = false) String sortProperty,
                                                   @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        checkParameter("groupId", strGroupId);
        checkParameter("type", strType);
        try {
            EntityGroupId groupId = new EntityGroupId(toUUID(strGroupId));
            checkEntityGroupId(groupId, Operation.READ);
            PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
            return checkNotNull(otaPackageService.findOtaPackageInfosByGroupIdAndHasData(groupId, OtaPackageType.valueOf(strType), pageLink));
        } catch (Exception e) {
            throw handleException(e);
        }
    }
}
