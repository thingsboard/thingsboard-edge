/**
 * Thingsboard OÜ ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2017 Thingsboard OÜ. All Rights Reserved.
 *
 * NOTICE: All information contained herein is, and remains
 * the property of Thingsboard OÜ and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Thingsboard OÜ
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

import com.google.common.util.concurrent.ListenableFuture;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.thingsboard.server.common.data.EntitySubtype;
import org.thingsboard.server.common.data.converter.Converter;
import org.thingsboard.server.common.data.converter.ConverterSearchQuery;
import org.thingsboard.server.common.data.converter.ConverterType;
import org.thingsboard.server.common.data.id.ConverterId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.TextPageData;
import org.thingsboard.server.common.data.page.TextPageLink;
import org.thingsboard.server.exception.ThingsboardException;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class ConverterController extends BaseController {

    public static final String CONVERTER_ID = "converterId";

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/converter/{converterId}", method = RequestMethod.GET)
    @ResponseBody
    public Converter getConverterById(@PathVariable(CONVERTER_ID) String strConverterId) throws ThingsboardException {
        checkParameter(CONVERTER_ID, strConverterId);
        try {
            ConverterId converterId = new ConverterId(toUUID(strConverterId));
            return checkConverterId(converterId);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/converter", method = RequestMethod.POST)
    @ResponseBody
    public Converter saveConverter(@RequestBody Converter converter) throws ThingsboardException {
        try {
            converter.setTenantId(getCurrentUser().getTenantId());
            return checkNotNull(converterService.saveConverter(converter));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/converter/{converterId}", method = RequestMethod.DELETE)
    @ResponseStatus(value = HttpStatus.OK)
    public void deleteConverter(@PathVariable(CONVERTER_ID) String strConverterId) throws ThingsboardException {
        checkParameter(CONVERTER_ID, strConverterId);
        try {
            ConverterId converterId = new ConverterId(toUUID(strConverterId));
            checkConverterId(converterId);
            converterService.deleteConverter(converterId);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/tenant/converters", params = {"limit"}, method = RequestMethod.GET)
    @ResponseBody
    public TextPageData<Converter> getTenantConverters(
            @RequestParam int limit,
            @RequestParam(required = false) ConverterType type,
            @RequestParam(required = false) String textSearch,
            @RequestParam(required = false) String idOffset,
            @RequestParam(required = false) String textOffset) throws ThingsboardException {
        try {
            TenantId tenantId = getCurrentUser().getTenantId();
            TextPageLink pageLink = createPageLink(limit, textSearch, idOffset, textOffset);
            if (type != null && type.toString().trim().length() > 0) {
                return checkNotNull(converterService.findConvertersByTenantIdAndType(tenantId, type, pageLink));
            } else {
                return checkNotNull(converterService.findConvertersByTenantId(tenantId, pageLink));
            }
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/tenant/converters", params = {"converterName"}, method = RequestMethod.GET)
    @ResponseBody
    public Converter getTenantConverter(
            @RequestParam String converterName) throws ThingsboardException {
        try {
            TenantId tenantId = getCurrentUser().getTenantId();
            return checkNotNull(converterService.findConverterByTenantIdAndName(tenantId, converterName));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/converters", params = {"converterIds"}, method = RequestMethod.GET)
    @ResponseBody
    public List<Converter> getConvertersByIds(
            @RequestParam("converterIds") String[] strConverterIds) throws ThingsboardException {
        checkArrayParameter("converterIds", strConverterIds);
        try {
            SecurityUser user = getCurrentUser();
            TenantId tenantId = user.getTenantId();
            List<ConverterId> converterIds = new ArrayList<>();
            for (String strConverterId : strConverterIds) {
                converterIds.add(new ConverterId(toUUID(strConverterId)));
            }
            ListenableFuture<List<Converter>> converters;
            converters = converterService.findConvertersByTenantIdAndIdsAsync(tenantId, converterIds);
            return checkNotNull(converters.get());
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/converters", method = RequestMethod.POST)
    @ResponseBody
    public List<Converter> findByQuery(@RequestBody ConverterSearchQuery query) throws ThingsboardException {
        checkNotNull(query);
        checkNotNull(query.getParameters());
        checkNotNull(query.getConverterTypes());
        checkEntityId(query.getParameters().getEntityId());
        try {
            List<Converter> converters = checkNotNull(converterService.findConvertersByQuery(query).get());
            converters = converters.stream().filter(converter -> {
                try {
                    checkConverter(converter);
                    return true;
                } catch (ThingsboardException e) {
                    return false;
                }
            }).collect(Collectors.toList());
            return converters;
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/converter/types", method = RequestMethod.GET)
    @ResponseBody
    public List<EntitySubtype> getConverterTypes() throws ThingsboardException {
        try {
            SecurityUser user = getCurrentUser();
            TenantId tenantId = user.getTenantId();
            ListenableFuture<List<EntitySubtype>> converterTypes = converterService.findConverterTypesByTenantId(tenantId);
            return checkNotNull(converterTypes.get());
        } catch (Exception e) {
            throw handleException(e);
        }
    }
}
