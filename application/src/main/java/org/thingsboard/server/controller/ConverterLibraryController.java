/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2025 ThingsBoard, Inc. All Rights Reserved.
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


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.integration.IntegrationType;
import org.thingsboard.server.config.annotations.ApiOperation;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.converter.ConverterLibraryService;
import org.thingsboard.server.service.converter.Model;
import org.thingsboard.server.service.converter.Vendor;

import java.util.List;

@RestController
@TbCoreComponent
@RequiredArgsConstructor
@RequestMapping("/api/converter/library")
@Slf4j
public class ConverterLibraryController extends BaseController {

    private final ConverterLibraryService converterLibraryService;

    @ApiOperation(value = "Get vendors (getVendors)",
            notes = "Returns a list of vendors for the integration type")
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @GetMapping(value = "/{integrationType}/vendors")
    public List<Vendor> getVendors(@PathVariable IntegrationType integrationType,
                                   @RequestParam(required = false) String converterType) {
        return converterLibraryService.getVendors(integrationType, converterType);
    }

    @ApiOperation(value = "Get vendor models (getVendorModels)",
            notes = "Returns a list of models for the vendor, integration type and converter type")
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @GetMapping(value = "/{integrationType}/{vendorName}/models")
    public List<Model> getVendorModels(@PathVariable IntegrationType integrationType,
                                       @PathVariable String vendorName,
                                       @RequestParam(required = false) String converterType) {
        return converterLibraryService.getVendorModels(integrationType, converterType, vendorName);
    }

    @ApiOperation(value = "Get uplink converter (getUplinkConverter)",
            notes = "Returns uplink converter body for the vendor, integration type and model")
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @GetMapping(value = "/{integrationType}/{vendorName}/{model}/uplink", produces = "text/plain")
    public String getUplinkConverter(@PathVariable IntegrationType integrationType,
                                     @PathVariable String vendorName,
                                     @PathVariable String model) {
        return converterLibraryService.getConverter(integrationType, "uplink", vendorName, model);
    }

    @ApiOperation(value = "Get uplink converter metadata (getUplinkConverterMetadata)",
            notes = "Returns uplink converter metadata for the vendor, integration type and model")
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @GetMapping(value = "/{integrationType}/{vendorName}/{model}/uplink/metadata", produces = "text/plain")
    public String getUplinkConverterMetadata(@PathVariable IntegrationType integrationType,
                                             @PathVariable String vendorName,
                                             @PathVariable String model) {
        return converterLibraryService.getConverterMetadata(integrationType, "uplink", vendorName, model);
    }

    @ApiOperation(value = "Get uplink payload (getUplinkPayload)",
            notes = "Returns payload example for the uplink converter for the vendor, integration type and model")
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @GetMapping(value = "/{integrationType}/{vendorName}/{model}/uplink/payload", produces = "text/plain")
    public String getUplinkPayload(@PathVariable IntegrationType integrationType,
                                   @PathVariable String vendorName,
                                   @PathVariable String model) {
        return converterLibraryService.getPayload(integrationType, "uplink", vendorName, model);
    }

    @ApiOperation(value = "Get downlink converter (getDownlinkConverter)",
            notes = "Returns downlink converter body for the vendor, integration type and model")
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @GetMapping(value = "/{integrationType}/{vendorName}/{model}/downlink", produces = "text/plain")
    public String getDownlinkConverter(@PathVariable IntegrationType integrationType,
                                       @PathVariable String vendorName,
                                       @PathVariable String model) {
        return converterLibraryService.getConverter(integrationType, "downlink", vendorName, model);
    }

    @ApiOperation(value = "Get downlink converter metadata (getDownlinkConverterMetadata)",
            notes = "Returns downlink converter metadata for the vendor, integration type and model")
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @GetMapping(value = "/{integrationType}/{vendorName}/{model}/downlink/metadata", produces = "text/plain")
    public String getDownlinkConverterMetadata(@PathVariable IntegrationType integrationType,
                                               @PathVariable String vendorName,
                                               @PathVariable String model) {
        return converterLibraryService.getConverterMetadata(integrationType, "downlink", vendorName, model);
    }

    @ApiOperation(value = "Get downlink payload (getDownlinkPayload)",
            notes = "Returns payload example for the downlink converter for the vendor, integration type and model")
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @GetMapping(value = "/{integrationType}/{vendorName}/{model}/downlink/payload", produces = "text/plain")
    public String getDownlinkPayload(@PathVariable IntegrationType integrationType,
                                     @PathVariable String vendorName,
                                     @PathVariable String model) {
        return converterLibraryService.getPayload(integrationType, "downlink", vendorName, model);
    }

}
