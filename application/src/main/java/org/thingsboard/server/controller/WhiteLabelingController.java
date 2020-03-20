/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2020 ThingsBoard, Inc. All Rights Reserved.
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

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import io.bit3.jsass.Compiler;
import io.bit3.jsass.Options;
import io.bit3.jsass.Output;
import io.bit3.jsass.OutputStyle;
import io.bit3.jsass.importer.Import;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.common.data.permission.Resource;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.wl.LoginWhiteLabelingParams;
import org.thingsboard.server.common.data.wl.Palette;
import org.thingsboard.server.common.data.wl.PaletteSettings;
import org.thingsboard.server.common.data.wl.WhiteLabelingParams;
import org.thingsboard.server.dao.wl.WhiteLabelingService;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class WhiteLabelingController extends BaseController {

    private static final String SCSS_RESOURCES_PATH = "scss";
    private static final String SCSS_EXTENSION = ".scss";
    private static final String APP_THEME_SCSS = "app-theme.scss";
    private static final String LOGIN_THEME_SCSS = "login-theme.scss";

    @Autowired
    private WhiteLabelingService whiteLabelingService;

    private Map<String, Import> scssImportMap;
    private String scssAppTheme;
    private String scssLoginTheme;
    private Options saasOptions;
    private Compiler saasCompiler;

    @PostConstruct
    public void init() throws Exception {
        this.initSaasCompiler();
    }

    private void initSaasCompiler() throws Exception {
        this.scssImportMap = new HashMap<>();
        URL scssUrl = Resources.getResource(SCSS_RESOURCES_PATH);
        List<String> scssFiles = Resources.readLines(scssUrl, Charsets.UTF_8);
        for (String file : scssFiles) {
            if (file.endsWith(SCSS_EXTENSION)) {
                String scssFile = SCSS_RESOURCES_PATH + "/" + file;
                URL scssFileUrl = Resources.getResource(scssFile);
                String scssContent = Resources.toString(scssFileUrl, Charsets.UTF_8);
                if (file.equals(APP_THEME_SCSS)) {
                    this.scssAppTheme = scssContent;
                } else if (file.equals(LOGIN_THEME_SCSS)) {
                    this.scssLoginTheme = scssContent;
                } else {
                    URI scssFileUri = scssFileUrl.toURI();
                    final Import scssImport = new Import(scssFileUri, scssFileUri, scssContent);
                    String path = file.substring(0, file.length() - SCSS_EXTENSION.length());
                    scssImportMap.put(path, scssImport);
                }
            }
        }
        this.saasCompiler = new Compiler();
        this.saasOptions = new Options();
        this.saasOptions.setImporters(Collections.singleton(
                (url, previous) -> Collections.singletonList(this.scssImportMap.get(url))
        ));
        this.saasOptions.setOutputStyle(OutputStyle.COMPRESSED);
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/whiteLabel/whiteLabelParams", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public WhiteLabelingParams getWhiteLabelParams(
            @RequestParam(required = false) String logoImageChecksum,
            @RequestParam(required = false) String faviconChecksum) throws ThingsboardException {
        try {
            Authority authority = getCurrentUser().getAuthority();
            WhiteLabelingParams whiteLabelingParams = null;
            if (authority == Authority.SYS_ADMIN) {
                whiteLabelingParams = whiteLabelingService.getMergedSystemWhiteLabelingParams(TenantId.SYS_TENANT_ID, logoImageChecksum, faviconChecksum);
            } else if (authority == Authority.TENANT_ADMIN) {
                whiteLabelingParams = whiteLabelingService.getMergedTenantWhiteLabelingParams(getCurrentUser().getTenantId(),
                        logoImageChecksum, faviconChecksum);
            } else if (authority == Authority.CUSTOMER_USER) {
                whiteLabelingParams = whiteLabelingService.getMergedCustomerWhiteLabelingParams(getCurrentUser().getTenantId(),
                        getCurrentUser().getCustomerId(), logoImageChecksum, faviconChecksum);
            }
            return whiteLabelingParams;
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @RequestMapping(value = "/noauth/whiteLabel/loginWhiteLabelParams", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public LoginWhiteLabelingParams getLoginWhiteLabelParams(
            @RequestParam(required = false) String logoImageChecksum,
            @RequestParam(required = false) String faviconChecksum,
            HttpServletRequest request) throws ThingsboardException {
        try {
            return whiteLabelingService.getMergedLoginWhiteLabelingParams(TenantId.SYS_TENANT_ID, request.getServerName(), logoImageChecksum, faviconChecksum);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/whiteLabel/currentWhiteLabelParams", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public WhiteLabelingParams getCurrentWhiteLabelParams() throws ThingsboardException {
        try {
            Authority authority = getCurrentUser().getAuthority();
            checkWhiteLabelingPermissions(Operation.READ);
            WhiteLabelingParams whiteLabelingParams = null;
            if (authority == Authority.SYS_ADMIN) {
                whiteLabelingParams = whiteLabelingService.getSystemWhiteLabelingParams(TenantId.SYS_TENANT_ID);
            } else if (authority == Authority.TENANT_ADMIN) {
                whiteLabelingParams = whiteLabelingService.getTenantWhiteLabelingParams(getCurrentUser().getTenantId());
            } else if (authority == Authority.CUSTOMER_USER) {
                whiteLabelingParams = whiteLabelingService.getCustomerWhiteLabelingParams(getTenantId(), getCurrentUser().getCustomerId());
            }
            return whiteLabelingParams;
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/whiteLabel/currentLoginWhiteLabelParams", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public LoginWhiteLabelingParams getCurrentLoginWhiteLabelParams() throws ThingsboardException {
        try {
            Authority authority = getCurrentUser().getAuthority();
            checkWhiteLabelingPermissions(Operation.READ);
            LoginWhiteLabelingParams loginWhiteLabelingParams = null;
            if (authority == Authority.SYS_ADMIN) {
                loginWhiteLabelingParams = whiteLabelingService.getSystemLoginWhiteLabelingParams(TenantId.SYS_TENANT_ID);
            } else if (authority == Authority.TENANT_ADMIN) {
                loginWhiteLabelingParams = whiteLabelingService.getTenantLoginWhiteLabelingParams(getCurrentUser().getTenantId());
            } else if (authority == Authority.CUSTOMER_USER) {
                loginWhiteLabelingParams = whiteLabelingService.getCustomerLoginWhiteLabelingParams(getTenantId(), getCurrentUser().getCustomerId());
            }
            return loginWhiteLabelingParams;
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/whiteLabel/whiteLabelParams", method = RequestMethod.POST)
    @ResponseStatus(value = HttpStatus.OK)
    public WhiteLabelingParams saveWhiteLabelParams(@RequestBody WhiteLabelingParams whiteLabelingParams) throws ThingsboardException {
        try {
            Authority authority = getCurrentUser().getAuthority();
            checkWhiteLabelingPermissions(Operation.WRITE);
            WhiteLabelingParams savedWhiteLabelingParams = null;
            if (authority == Authority.SYS_ADMIN) {
                savedWhiteLabelingParams = whiteLabelingService.saveSystemWhiteLabelingParams(whiteLabelingParams);
            } else if (authority == Authority.TENANT_ADMIN) {
                savedWhiteLabelingParams = whiteLabelingService.saveTenantWhiteLabelingParams(getCurrentUser().getTenantId(), whiteLabelingParams);
            } else if (authority == Authority.CUSTOMER_USER) {
                savedWhiteLabelingParams = whiteLabelingService.saveCustomerWhiteLabelingParams(getTenantId(), getCurrentUser().getCustomerId(), whiteLabelingParams);
            }
            return savedWhiteLabelingParams;
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/whiteLabel/loginWhiteLabelParams", method = RequestMethod.POST)
    @ResponseStatus(value = HttpStatus.OK)
    public LoginWhiteLabelingParams saveLoginWhiteLabelParams(@RequestBody LoginWhiteLabelingParams loginWhiteLabelingParams) throws ThingsboardException {
        try {
            Authority authority = getCurrentUser().getAuthority();
            checkWhiteLabelingPermissions(Operation.WRITE);
            LoginWhiteLabelingParams savedLoginWhiteLabelingParams = null;
            if (authority == Authority.SYS_ADMIN) {
                savedLoginWhiteLabelingParams = whiteLabelingService.saveSystemLoginWhiteLabelingParams(loginWhiteLabelingParams);
            } else if (authority == Authority.TENANT_ADMIN) {
                savedLoginWhiteLabelingParams = whiteLabelingService.saveTenantLoginWhiteLabelingParams(getCurrentUser().getTenantId(), loginWhiteLabelingParams);
            } else if (authority == Authority.CUSTOMER_USER) {
                savedLoginWhiteLabelingParams = whiteLabelingService.saveCustomerLoginWhiteLabelingParams(getTenantId(), getCurrentUser().getCustomerId(), loginWhiteLabelingParams);
            }
            return savedLoginWhiteLabelingParams;
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/whiteLabel/previewWhiteLabelParams", method = RequestMethod.POST)
    @ResponseStatus(value = HttpStatus.OK)
    public WhiteLabelingParams previewWhiteLabelParams(@RequestBody WhiteLabelingParams whiteLabelingParams) throws ThingsboardException {
        try {
            Authority authority = getCurrentUser().getAuthority();
            checkWhiteLabelingPermissions(Operation.WRITE);
            WhiteLabelingParams mergedWhiteLabelingParams = null;
            if (authority == Authority.SYS_ADMIN) {
                mergedWhiteLabelingParams = whiteLabelingService.mergeSystemWhiteLabelingParams(whiteLabelingParams);
            } else if (authority == Authority.TENANT_ADMIN) {
                mergedWhiteLabelingParams = whiteLabelingService.mergeTenantWhiteLabelingParams(getTenantId(), whiteLabelingParams);
            } else if (authority == Authority.CUSTOMER_USER) {
                mergedWhiteLabelingParams = whiteLabelingService.mergeCustomerWhiteLabelingParams(getCurrentUser().getTenantId(), whiteLabelingParams);
            }
            return mergedWhiteLabelingParams;
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/whiteLabel/isWhiteLabelingAllowed", method = RequestMethod.GET)
    @ResponseBody
    public Boolean isWhiteLabelingAllowed() throws ThingsboardException {
        try {
            Authority authority = getCurrentUser().getAuthority();
            EntityId entityId;
            if (authority == Authority.TENANT_ADMIN) {
                entityId = getCurrentUser().getTenantId();
            } else {
                entityId = getCurrentUser().getCustomerId();
            }
            return whiteLabelingService.isWhiteLabelingAllowed(getTenantId(), entityId);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/whiteLabel/isCustomerWhiteLabelingAllowed", method = RequestMethod.GET)
    @ResponseBody
    public Boolean isCustomerWhiteLabelingAllowed() throws ThingsboardException {
        try {
            return whiteLabelingService.isCustomerWhiteLabelingAllowed(getCurrentUser().getTenantId());
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @RequestMapping(value = "/noauth/whiteLabel/loginThemeCss", method = RequestMethod.POST, produces = "plain/text")
    @ResponseStatus(value = HttpStatus.OK)
    public String getLoginThemeCss(@RequestBody PaletteSettings paletteSettings,
                                   @RequestParam(name = "darkForeground", required = false) boolean darkForeground) throws ThingsboardException {
        try {
            return this.generateThemeCss(paletteSettings, true, darkForeground);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/whiteLabel/appThemeCss", method = RequestMethod.POST, produces = "plain/text")
    @ResponseStatus(value = HttpStatus.OK)
    public String getAppThemeCss(@RequestBody PaletteSettings paletteSettings) throws ThingsboardException {
        try {
            return this.generateThemeCss(paletteSettings, false, false);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    private String generateThemeCss(PaletteSettings paletteSettings,
                                    boolean loginTheme,
                                    boolean darkForeground) throws Exception {
        String primaryPaletteName = getPaletteName(paletteSettings.getPrimaryPalette(), true);
        String primaryColors = getPaletteColors(paletteSettings.getPrimaryPalette());
        String accentPaletteName = getPaletteName(paletteSettings.getAccentPalette(), false);
        String accentColors = getPaletteColors(paletteSettings.getAccentPalette());
        String targetTheme = loginTheme ? this.scssLoginTheme : this.scssAppTheme;

        targetTheme = targetTheme.replaceAll("##primary-palette##", primaryPaletteName);
        targetTheme = targetTheme.replaceAll("##primary-colors##", primaryColors);
        targetTheme = targetTheme.replaceAll("##accent-palette##", accentPaletteName);
        targetTheme = targetTheme.replaceAll("##accent-colors##", accentColors);

        if (loginTheme) {
            targetTheme = targetTheme.replaceAll("##dark-foreground##", darkForeground ? "true" : "false");
        }
        Output output = this.saasCompiler.compileString(targetTheme, this.saasOptions);
        return output.getCss();
    }

    private String getPaletteName(Palette palette, boolean primary) {
        if (palette == null) {
            return primary ? "tb-primary" : "tb-accent";
        }
        if (palette.getType().equals("custom")) {
            return palette.getExtendsPalette();
        } else {
            return palette.getType();
        }
    }

    private String getPaletteColors(Palette palette) {
        if (palette != null && palette.getColors() != null && !palette.getColors().isEmpty()) {
            List<String> colorsList = new ArrayList<>();
            palette.getColors().forEach(
                    (hue, hex) -> {
                        colorsList.add(String.format("%s: %s", hue, hex));
                    }
            );
            return "\n"+String.join(",\n", colorsList)+"\n";
        } else {
            return "";
        }
    }

    private void checkWhiteLabelingPermissions(Operation operation) throws ThingsboardException {
        accessControlService.checkPermission(getCurrentUser(), Resource.WHITE_LABELING, operation);
    }
}
