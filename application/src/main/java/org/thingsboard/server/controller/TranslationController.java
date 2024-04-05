/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2024 ThingsBoard, Inc. All Rights Reserved.
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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.hash.Hashing;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.common.data.permission.Resource;
import org.thingsboard.server.common.data.translation.TranslationInfo;
import org.thingsboard.server.config.annotations.ApiOperation;
import org.thingsboard.server.dao.translation.TranslationCacheKey;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.translation.TbTranslationService;

import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import static org.thingsboard.server.controller.ControllerConstants.MARKDOWN_CODE_BLOCK_END;
import static org.thingsboard.server.controller.ControllerConstants.MARKDOWN_CODE_BLOCK_START;

@RestController
@TbCoreComponent
@RequestMapping("/api")
@RequiredArgsConstructor
public class TranslationController extends BaseController {

    public static final List<String> LOCALE_CODES_TO_EXCLUDE = Arrays.asList("ru_UA", "ccp_IN");

    private static final String CUSTOM_TRANSLATION_INFO_EXAMPLE = "\n\n" +
            MARKDOWN_CODE_BLOCK_START +
            "[\n" +
            "  {\n" +
            "    \"localeCode\": \"uk_UA\",\n" +
            "    \"language\": \"Ukrainian (українська)\",\n" +
            "    \"country\": \"Україна\",\n" +
            "    \"progress\": 32\n" +
            "  },\n" +
            "  {\n" +
            "    \"localeCode\": \"es_ES\",\n" +
            "    \"language\": \"Spanish (español)\",\n" +
            "    \"country\": \"España\",\n" +
            "    \"progress\": 79\n" +
            "  }" +
            "]" +
            MARKDOWN_CODE_BLOCK_END;

    private final TbTranslationService tbTranslationService;

    @ApiOperation(value = "Get Translation info (getTranslationInfos)",
            notes = "Fetch the list of customized locales and corresponding details such as language display name," +
                    " country display name and translation progress percentage." +
                    "\n\n Response example: " + CUSTOM_TRANSLATION_INFO_EXAMPLE +
                    ControllerConstants.WL_READ_CHECK)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value = "/translation/info")
    @ResponseBody
    public List<TranslationInfo> getTranslationInfos() throws ThingsboardException {
        checkWhiteLabelingPermissions(Operation.READ);
        return tbTranslationService.getTranslationInfos(getCurrentUser().getTenantId(), getCurrentUser().getCustomerId());
    }

    @ApiOperation(value = "Get list of available system locales (getAvailableLocales)",
            notes = "The result is map where key is locale code and value is locale language and country")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value = "/translation/availableLocales")
    @ResponseBody
    public JsonNode getAvailableLocales() {
        ObjectNode result = JacksonUtil.newObjectNode();
        List<Locale> availableLocales = Arrays.stream(DateFormat.getAvailableLocales())
                .filter(availableLocale -> !availableLocale.toString().isBlank() && availableLocale.toString().contains("_")
                        && availableLocale.getScript().isBlank() && !LOCALE_CODES_TO_EXCLUDE.contains(availableLocale.toString()))
                .toList();
        for (Locale availableLocale : availableLocales) {
            String displayLanguage = availableLocale.getDisplayLanguage(availableLocale);
            String displayCountry = availableLocale.getDisplayCountry(availableLocale).isBlank() ? "" : " (" + availableLocale.getDisplayCountry(availableLocale) + ")";
            result.put(availableLocale.toString(), displayLanguage + displayCountry);
        }
        return result;
    }

    @ApiOperation(value = "Get system translation for login page",
            notes = "Fetch the end-user translation for specified locale.")
    @RequestMapping(value = "/noauth/translation/login/{localeCode}", method = RequestMethod.GET)
    @ResponseBody
    public JsonNode getLoginPageTranslation(
            @Parameter(description = "Locale code (e.g. 'en_US').")
            @PathVariable("localeCode") String localeCode) {
        return tbTranslationService.getLoginTranslation(localeCode);
    }

    @ApiOperation(value = "Get end-user all-to-one translation (getFullTranslation)",
            notes = "Fetch the end-user translation for specified locale. The result is the merge of user custom translation, " +
                    "system language translation and default locale translation.")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value = "/translation/full/{localeCode}")
    @ResponseBody
    public ResponseEntity<JsonNode> getFullTranslation(
            @Parameter(description = "Locale code (e.g. 'en_US').")
            @PathVariable("localeCode") String localeCode,
            @RequestHeader(name = HttpHeaders.IF_NONE_MATCH, required = false) String etag) throws Exception {
        checkWhiteLabelingPermissions(Operation.READ);
        TenantId tenantId = getCurrentUser().getTenantId();
        CustomerId customerId = getCurrentUser().getCustomerId();

        TranslationCacheKey cacheKey = TranslationCacheKey.forLocale(tenantId, customerId, localeCode);
        if (StringUtils.isNotEmpty(etag)) {
            etag = StringUtils.remove(etag, '\"'); // etag is wrapped in double quotes due to HTTP specification
            if (etag.equals(tbTranslationService.getETag(cacheKey))) {
                return ResponseEntity.status(HttpStatus.NOT_MODIFIED).build();
            }
        }

        JsonNode fullTranslation = tbTranslationService.getFullTranslation(tenantId, customerId, localeCode);
        String calculatedEtag = calculateTranslationEtag(fullTranslation);
        tbTranslationService.putETag(cacheKey, calculatedEtag);
        return ResponseEntity.ok()
                .header("Content-Type", MediaType.APPLICATION_JSON.toString())
                .eTag(calculatedEtag)
                .cacheControl(CacheControl.noCache())
                .body(fullTranslation);
    }

    @ApiOperation(value = "Get end-user translated only part of translation (getTranslatedOnlyTranslation)")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value = "/translation/edit/basic/{localeCode}")
    @ResponseBody
    public JsonNode getTranslationForBasicEdit(@PathVariable("localeCode") String localeCode) throws Exception {
        checkWhiteLabelingPermissions(Operation.READ);
        TenantId tenantId = getCurrentUser().getTenantId();
        CustomerId customerId = getCurrentUser().getCustomerId();

        return tbTranslationService.getTranslationForBasicEdit(tenantId, customerId, localeCode);
    }

    @ApiOperation(value = "Download end-user all-to-one translation (downloadFullTranslation)",
            notes = "Fetch the end-user translation for the specified locale. The result is a json file with merged user custom translation, " +
                    "system language translation and default locale translation.")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value = "/translation/full/{localeCode}/download")
    @ResponseBody
    public ResponseEntity<ByteArrayResource> downloadFullTranslation(@Parameter(description = "Locale code (e.g. 'en_US').")
                                                                     @PathVariable("localeCode") String localeCode) throws Exception {
        checkWhiteLabelingPermissions(Operation.READ);
        TenantId tenantId = getCurrentUser().getTenantId();
        CustomerId customerId = getCurrentUser().getCustomerId();

        JsonNode fullSystemTranslation = checkNotNull(tbTranslationService.getFullTranslation(tenantId, customerId, localeCode));

        String fileName = localeCode + "_custom_translation.json";
        ByteArrayResource translation = new ByteArrayResource(fullSystemTranslation.toString().getBytes());

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=" + fileName)
                .header("x-filename", fileName)
                .contentLength(translation.contentLength())
                .header("Content-Type", MediaType.APPLICATION_JSON.toString())
                .body(translation);
    }

    protected String calculateTranslationEtag(JsonNode translation) {
        return Hashing.sha256().hashString(translation.toString(), StandardCharsets.UTF_8).toString();
    }

    private void checkWhiteLabelingPermissions(Operation operation) throws ThingsboardException {
        accessControlService.checkPermission(getCurrentUser(), Resource.WHITE_LABELING, operation);
    }

}