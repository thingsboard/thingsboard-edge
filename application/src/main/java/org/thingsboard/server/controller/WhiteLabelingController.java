/**
 * Copyright © 2016-2017 The Thingsboard Authors
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

package org.thingsboard.server.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.thingsboard.server.common.data.AdminSettings;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.kv.*;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.settings.AdminSettingsService;
import org.thingsboard.server.exception.ThingsboardException;

import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api")
public class WhiteLabelingController extends BaseController {

    private static final String LOGO_IMAGE_CHECKSUM = "logoImageChecksum";
    private static final String LOGO_IMAGE = "logoImage";
    private static final String WHITE_LABEL_PARAMS = "whiteLabelParams";

    private static final String LOGO_IMAGE_URL = "logoImageUrl";

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private AdminSettingsService adminSettingsService;

    @Autowired
    private AttributesService attributesService;

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/whiteLabel/logoImageChecksum", method = RequestMethod.GET, produces = "text/plain")
    @ResponseBody
    public String getLogoImageChecksum(
            @RequestParam(required = false, defaultValue = "false") boolean parent) throws ThingsboardException {
        try {
            Authority authority = getCurrentUser().getAuthority();
            if (authority == Authority.SYS_ADMIN || parent && authority == Authority.TENANT_ADMIN) {
                return getSysAdminLogoImageChecksum();
            } else if (authority == Authority.TENANT_ADMIN || parent && authority == Authority.CUSTOMER_USER) {
                String value = getEntityLogoImageChecksum(getCurrentUser().getTenantId());
                if (StringUtils.isEmpty(value)) {
                    value = getSysAdminLogoImageChecksum();
                }
                return value;
            } else if (authority == Authority.CUSTOMER_USER) {
                String value = getEntityLogoImageChecksum(getCurrentUser().getCustomerId());
                if (StringUtils.isEmpty(value)) {
                    value = getEntityLogoImageChecksum(getCurrentUser().getTenantId());
                }
                if (StringUtils.isEmpty(value)) {
                    value = getSysAdminLogoImageChecksum();
                }
                return value;
            }
            return "";
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/whiteLabel/logoImage", method = RequestMethod.GET, produces = "text/plain")
    @ResponseBody
    public String getLogoImage(
            @RequestParam(required = false, defaultValue = "false") boolean parent) throws ThingsboardException {
        try {
            Authority authority = getCurrentUser().getAuthority();
            if (authority == Authority.SYS_ADMIN || parent && authority == Authority.TENANT_ADMIN) {
                return getSysAdminLogoImage();
            } else if (authority == Authority.TENANT_ADMIN || parent && authority == Authority.CUSTOMER_USER) {
                String value = getEntityLogoImage(getCurrentUser().getTenantId());
                if (StringUtils.isEmpty(value)) {
                    value = getSysAdminLogoImage();
                }
                return value;
            } else if (authority == Authority.CUSTOMER_USER) {
                String value = getEntityLogoImage(getCurrentUser().getCustomerId());
                if (StringUtils.isEmpty(value)) {
                    value = getEntityLogoImage(getCurrentUser().getTenantId());
                }
                if (StringUtils.isEmpty(value)) {
                    value = getSysAdminLogoImage();
                }
                return value;
            }
            return "";
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/whiteLabel/whiteLabelParams", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public JsonNode getWhiteLabelParams(
            @RequestParam(required = false, defaultValue = "false") boolean parent) throws ThingsboardException {
        try {
            Authority authority = getCurrentUser().getAuthority();
            JsonNode whiteLabelParams = null;
            if (authority == Authority.SYS_ADMIN || parent && authority == Authority.TENANT_ADMIN) {
                whiteLabelParams = getSysAdminWhiteLabelParams();
            } else if (authority == Authority.TENANT_ADMIN || parent && authority == Authority.CUSTOMER_USER) {
                whiteLabelParams = getEntityWhiteLabelParams(getCurrentUser().getTenantId());
                if (whiteLabelParams == null) {
                    whiteLabelParams = getSysAdminWhiteLabelParams();
                }
            } else if (authority == Authority.CUSTOMER_USER) {
                whiteLabelParams = getEntityWhiteLabelParams(getCurrentUser().getCustomerId());
                if (whiteLabelParams == null) {
                    whiteLabelParams = getEntityWhiteLabelParams(getCurrentUser().getTenantId());
                }
                if (whiteLabelParams == null) {
                    whiteLabelParams = getSysAdminWhiteLabelParams();
                }
            }
            return prepareWhiteLabelParams(whiteLabelParams);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/whiteLabel/currentWhiteLabelParams", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public JsonNode getCurrentWhiteLabelParams() throws ThingsboardException {
        try {
            Authority authority = getCurrentUser().getAuthority();
            JsonNode whiteLabelParams = null;
            String logoImage = null;
            if (authority == Authority.SYS_ADMIN) {
                whiteLabelParams = getSysAdminWhiteLabelParams();
                logoImage = getSysAdminLogoImage();
            } else if (authority == Authority.TENANT_ADMIN) {
                whiteLabelParams = getEntityWhiteLabelParams(getCurrentUser().getTenantId());
                logoImage = getEntityLogoImage(getCurrentUser().getTenantId());
            } else if (authority == Authority.CUSTOMER_USER) {
                whiteLabelParams = getEntityWhiteLabelParams(getCurrentUser().getCustomerId());
                logoImage = getEntityLogoImage(getCurrentUser().getCustomerId());
            }
            whiteLabelParams = prepareWhiteLabelParams(whiteLabelParams);
            ((ObjectNode)whiteLabelParams).put(LOGO_IMAGE_URL, logoImage);
            return whiteLabelParams;
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @RequestMapping(value = "/noauth/whiteLabel/systemWhiteLabelParams", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public JsonNode getSystemWhiteLabelParams() throws ThingsboardException {
        try {
            return prepareWhiteLabelParams(getSysAdminWhiteLabelParams());
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    private JsonNode prepareWhiteLabelParams(JsonNode whiteLabelParams) throws Exception {
        if (whiteLabelParams == null) {
            whiteLabelParams = objectMapper.createObjectNode();
        }
        return whiteLabelParams;
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/whiteLabel/logoImage", method = RequestMethod.POST, produces = "text/plain")
    @ResponseBody
    public String saveLogoImage(@RequestBody(required=false) String logoImageBase64) throws ThingsboardException {
        try {
            String logoImageChecksum = "";
            if (!StringUtils.isEmpty(logoImageBase64)) {
                logoImageChecksum = calculateSha1Checksum(logoImageBase64);
            } else {
                logoImageBase64 = "";
            }
            Authority authority = getCurrentUser().getAuthority();
            if (authority == Authority.SYS_ADMIN) {
                saveSysAdminLogoImage(logoImageBase64, logoImageChecksum);
            } else if (authority == Authority.TENANT_ADMIN) {
                saveEntityLogoImage(getCurrentUser().getTenantId(), logoImageBase64, logoImageChecksum);
            } else if (authority == Authority.CUSTOMER_USER) {
                saveEntityLogoImage(getCurrentUser().getCustomerId(), logoImageBase64, logoImageChecksum);
            }
            return logoImageChecksum;
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/whiteLabel/whiteLabelParams", method = RequestMethod.POST)
    @ResponseStatus(value = HttpStatus.OK)
    public void saveWhiteLabelParams(@RequestBody JsonNode whiteLabelParams) throws ThingsboardException {
        try {
            Authority authority = getCurrentUser().getAuthority();
            if (authority == Authority.SYS_ADMIN) {
                saveSysAdminWhiteLabelParams(whiteLabelParams);
            } else if (authority == Authority.TENANT_ADMIN) {
                saveEntityWhiteLabelParams(getCurrentUser().getTenantId(), whiteLabelParams);
            } else if (authority == Authority.CUSTOMER_USER) {
                saveEntityWhiteLabelParams(getCurrentUser().getCustomerId(), whiteLabelParams);
            }
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    private void saveSysAdminLogoImage(String logoImageBase64, String logoImageChecksum) throws ThingsboardException {
        AdminSettings logoImageSettings = getAdminSettings(LOGO_IMAGE);
        ((ObjectNode)logoImageSettings.getJsonValue()).put("value", logoImageBase64);
        adminSettingsService.saveAdminSettings(logoImageSettings);

        AdminSettings logoImageChecksumSettings = getAdminSettings(LOGO_IMAGE_CHECKSUM);
        ((ObjectNode)logoImageChecksumSettings.getJsonValue()).put("value", logoImageChecksum);
        adminSettingsService.saveAdminSettings(logoImageChecksumSettings);
    }

    private void saveEntityLogoImage(EntityId entityId, String logoImageBase64, String logoImageChecksum) throws Exception {
        saveEntityAttribute(entityId, LOGO_IMAGE, logoImageBase64);
        saveEntityAttribute(entityId, LOGO_IMAGE_CHECKSUM, logoImageChecksum);
    }

    private void saveSysAdminWhiteLabelParams(JsonNode whiteLabelParams) throws Exception {
        AdminSettings whiteLabelParamsSettings = getAdminSettings(WHITE_LABEL_PARAMS);
        ((ObjectNode)whiteLabelParamsSettings.getJsonValue()).put("value", objectMapper.writeValueAsString(whiteLabelParams));
        adminSettingsService.saveAdminSettings(whiteLabelParamsSettings);
    }

    private void saveEntityWhiteLabelParams(EntityId entityId, JsonNode whiteLabelParams) throws Exception {
        saveEntityAttribute(entityId, WHITE_LABEL_PARAMS, objectMapper.writeValueAsString(whiteLabelParams));
    }

    private String getSysAdminLogoImageChecksum() throws ThingsboardException {
        AdminSettings logoImageChecksumSettings = adminSettingsService.findAdminSettingsByKey(LOGO_IMAGE_CHECKSUM);
        if (logoImageChecksumSettings != null) {
            return logoImageChecksumSettings.getJsonValue().get("value").asText();
        } else {
            return "";
        }
    }

    private String getEntityLogoImageChecksum(EntityId entityId) throws Exception {
        return getEntityAttributeValue(entityId, LOGO_IMAGE_CHECKSUM);
    }

    private String getSysAdminLogoImage() throws ThingsboardException {
        AdminSettings logoImageSettings = adminSettingsService.findAdminSettingsByKey(LOGO_IMAGE);
        if (logoImageSettings != null) {
            return logoImageSettings.getJsonValue().get("value").asText();
        } else {
            return "";
        }
    }

    private String getEntityLogoImage(EntityId entityId) throws Exception {
        return getEntityAttributeValue(entityId, LOGO_IMAGE);
    }

    private JsonNode getSysAdminWhiteLabelParams() throws Exception {
        AdminSettings whiteLabelParamsSettings = adminSettingsService.findAdminSettingsByKey(WHITE_LABEL_PARAMS);
        if (whiteLabelParamsSettings != null) {
            String value = whiteLabelParamsSettings.getJsonValue().get("value").asText();
            if (!StringUtils.isEmpty(value)) {
                return objectMapper.readTree(value);
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    private JsonNode getEntityWhiteLabelParams(EntityId entityId) throws Exception {
        String value = getEntityAttributeValue(entityId, WHITE_LABEL_PARAMS);
        if (!StringUtils.isEmpty(value)) {
            return objectMapper.readTree(value);
        } else {
            return null;
        }
    }

    private AdminSettings getAdminSettings(String key) throws ThingsboardException {
        AdminSettings adminSettings = adminSettingsService.findAdminSettingsByKey(key);
        if (adminSettings == null) {
            adminSettings = new AdminSettings();
            adminSettings.setKey(key);
            ObjectNode node = objectMapper.createObjectNode();
            adminSettings.setJsonValue(node);
        }
        return adminSettings;
    }

    private String getEntityAttributeValue(EntityId entityId, String key) throws Exception {
        List<AttributeKvEntry> attributeKvEntries =
                attributesService.find(entityId, DataConstants.SERVER_SCOPE, Arrays.asList(key)).get();
        if (attributeKvEntries != null && !attributeKvEntries.isEmpty()) {
            AttributeKvEntry kvEntry = attributeKvEntries.get(0);
            return kvEntry.getValueAsString();
        } else {
            return "";
        }
    }

    private void saveEntityAttribute(EntityId entityId, String key, String value) throws Exception {
        List<AttributeKvEntry> attributes = new ArrayList<>();
        long ts = System.currentTimeMillis();
        attributes.add(new BaseAttributeKvEntry(new StringDataEntry(key, value), ts));
        attributesService.save(entityId, DataConstants.SERVER_SCOPE, attributes).get();
    }

    private String calculateSha1Checksum(String data) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA1");
        byte[] inputBytes = data.getBytes();
        byte[] hashBytes = digest.digest(inputBytes);
        StringBuffer sb = new StringBuffer("");
        for (int i = 0; i < hashBytes.length; i++) {
            sb.append(Integer.toString((hashBytes[i] & 0xff) + 0x100, 16).substring(1));
        }
        return sb.toString();
    }

}
