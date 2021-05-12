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
package org.thingsboard.server.service.cloud.processor.downlink;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.translation.CustomTranslation;
import org.thingsboard.server.common.data.wl.Favicon;
import org.thingsboard.server.common.data.wl.LoginWhiteLabelingParams;
import org.thingsboard.server.common.data.wl.Palette;
import org.thingsboard.server.common.data.wl.PaletteSettings;
import org.thingsboard.server.common.data.wl.WhiteLabelingParams;
import org.thingsboard.server.dao.translation.CustomTranslationService;
import org.thingsboard.server.dao.wl.WhiteLabelingService;
import org.thingsboard.server.gen.edge.CustomTranslationProto;
import org.thingsboard.server.gen.edge.FaviconProto;
import org.thingsboard.server.gen.edge.LoginWhiteLabelingParamsProto;
import org.thingsboard.server.gen.edge.PaletteProto;
import org.thingsboard.server.gen.edge.PaletteSettingsProto;
import org.thingsboard.server.gen.edge.WhiteLabelingParamsProto;

@Component
@Slf4j
public class WhiteLabelingProcessor extends BaseProcessor {

    @Autowired
    private CustomTranslationService customTranslationService;

    @Autowired
    private WhiteLabelingService whiteLabelingService;

    public ListenableFuture<Void> onCustomTranslationUpdate(TenantId tenantId, CustomTranslationProto customTranslationProto) {
        try {
            CustomTranslation customTranslation = new CustomTranslation();
            customTranslation.setTranslationMap(customTranslationProto.getTranslationMapMap());
            customTranslationService.saveTenantCustomTranslation(tenantId, customTranslation);
        } catch (Exception e) {
            log.error("Exception during updating custom translation", e);
            return Futures.immediateFailedFuture(new RuntimeException("Exception during updating custom translation", e));
        }
        return Futures.immediateFuture(null);
    }

    public ListenableFuture<Void> onLoginWhiteLabelingParamsUpdate(TenantId tenantId, LoginWhiteLabelingParamsProto loginWhiteLabelingParamsProto) {
        try {
            LoginWhiteLabelingParams loginWhiteLabelingParams = constructLoginWhiteLabelingParams(loginWhiteLabelingParamsProto);
            whiteLabelingService.saveSystemLoginWhiteLabelingParams(loginWhiteLabelingParams);
        } catch (Exception e) {
            log.error("Exception during updating login white labeling params", e);
            return Futures.immediateFailedFuture(new RuntimeException("Exception during updating login white labeling params", e));
        }
        return Futures.immediateFuture(null);
    }

    private LoginWhiteLabelingParams constructLoginWhiteLabelingParams(LoginWhiteLabelingParamsProto loginWLPProto) {
        LoginWhiteLabelingParams loginWLP = new LoginWhiteLabelingParams();
        loginWLP.setLogoImageUrl(loginWLPProto.getLogoImageUrl());
        loginWLP.setLogoImageChecksum(loginWLPProto.getLogoImageChecksum());
        loginWLP.setLogoImageHeight((int) loginWLPProto.getLogoImageHeight());
        loginWLP.setAppTitle(loginWLPProto.getAppTitle());
        loginWLP.setFavicon(constructFavicon(loginWLPProto.getFavicon()));
        loginWLP.setFaviconChecksum(loginWLPProto.getFaviconChecksum());
        loginWLP.setPaletteSettings(constructPaletteSettings(loginWLPProto.getPaletteSettings()));
        loginWLP.setHelpLinkBaseUrl(loginWLPProto.getHelpLinkBaseUrl());
        loginWLP.setEnableHelpLinks(loginWLPProto.getEnableHelpLinks());
        loginWLP.setShowNameVersion(loginWLPProto.getShowNameVersion());
        loginWLP.setPlatformName(loginWLPProto.getPlatformName());
        loginWLP.setPlatformVersion(loginWLPProto.getPlatformVersion());

        loginWLP.setPageBackgroundColor(loginWLPProto.getPageBackgroundColor());
        loginWLP.setDarkForeground(loginWLPProto.getDarkForeground());
        loginWLP.setDomainName(loginWLPProto.getDomainName());
        loginWLP.setAdminSettingsId(loginWLPProto.getAdminSettingsId());
        loginWLP.setShowNameBottom(loginWLPProto.getShowNameBottom());

        return loginWLP;
    }

    public ListenableFuture<Void> onWhiteLabelingParamsUpdate(TenantId tenantId, WhiteLabelingParamsProto wLPProto) {
        try {
            WhiteLabelingParams wLP = constructWhiteLabelingParams(wLPProto);
            whiteLabelingService.saveTenantWhiteLabelingParams(tenantId, wLP);
        } catch (Exception e) {
            log.error("Exception during updating white labeling params", e);
            return Futures.immediateFailedFuture(new RuntimeException("Exception during updating white labeling params", e));
        }
        return Futures.immediateFuture(null);
    }

    private WhiteLabelingParams constructWhiteLabelingParams(WhiteLabelingParamsProto whiteLabelingParamsProto) {
        WhiteLabelingParams whiteLabelingParams = new WhiteLabelingParams();
        whiteLabelingParams.setLogoImageUrl(whiteLabelingParamsProto.getLogoImageUrl());
        whiteLabelingParams.setLogoImageChecksum(whiteLabelingParamsProto.getLogoImageChecksum());
        whiteLabelingParams.setLogoImageHeight((int) whiteLabelingParamsProto.getLogoImageHeight());
        whiteLabelingParams.setAppTitle(whiteLabelingParamsProto.getAppTitle());
        whiteLabelingParams.setFavicon(constructFavicon(whiteLabelingParamsProto.getFavicon()));
        whiteLabelingParams.setFaviconChecksum(whiteLabelingParamsProto.getFaviconChecksum());
        whiteLabelingParams.setPaletteSettings(constructPaletteSettings(whiteLabelingParamsProto.getPaletteSettings()));
        whiteLabelingParams.setHelpLinkBaseUrl(whiteLabelingParamsProto.getHelpLinkBaseUrl());
        whiteLabelingParams.setEnableHelpLinks(whiteLabelingParamsProto.getEnableHelpLinks());
        whiteLabelingParams.setShowNameVersion(whiteLabelingParamsProto.getShowNameVersion());
        whiteLabelingParams.setPlatformName(whiteLabelingParamsProto.getPlatformName());
        whiteLabelingParams.setPlatformVersion(whiteLabelingParamsProto.getPlatformVersion());
        return whiteLabelingParams;
    }

    private Favicon constructFavicon(FaviconProto faviconProto) {
        Favicon favicon = new Favicon();
        favicon.setUrl(faviconProto.getUrl());
        favicon.setType(faviconProto.getType());
        return favicon;
    }

    private PaletteSettings constructPaletteSettings(PaletteSettingsProto paletteSettingsProto) {
        PaletteSettings paletteSettings = new PaletteSettings();
        paletteSettings.setPrimaryPalette(constructPalette(paletteSettingsProto.getPrimaryPalette()));
        paletteSettings.setAccentPalette(constructPalette(paletteSettingsProto.getAccentPalette()));
        return paletteSettings;
    }

    private Palette constructPalette(PaletteProto paletteProto) {
        Palette palette = new Palette();
        palette.setType(paletteProto.getType());
        palette.setExtendsPalette(paletteProto.getExtendsPalette());
        palette.setColors(paletteProto.getColorsMap());
        return palette;
    }
}
