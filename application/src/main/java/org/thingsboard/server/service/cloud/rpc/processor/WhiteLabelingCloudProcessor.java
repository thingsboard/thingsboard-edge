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
package org.thingsboard.server.service.cloud.rpc.processor;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.translation.CustomTranslation;
import org.thingsboard.server.common.data.wl.Favicon;
import org.thingsboard.server.common.data.wl.LoginWhiteLabelingParams;
import org.thingsboard.server.common.data.wl.Palette;
import org.thingsboard.server.common.data.wl.PaletteSettings;
import org.thingsboard.server.common.data.wl.WhiteLabelingParams;
import org.thingsboard.server.dao.translation.CustomTranslationService;
import org.thingsboard.server.dao.wl.WhiteLabelingService;
import org.thingsboard.server.gen.edge.v1.CustomTranslationProto;
import org.thingsboard.server.gen.edge.v1.FaviconProto;
import org.thingsboard.server.gen.edge.v1.LoginWhiteLabelingParamsProto;
import org.thingsboard.server.gen.edge.v1.PaletteProto;
import org.thingsboard.server.gen.edge.v1.PaletteSettingsProto;
import org.thingsboard.server.gen.edge.v1.WhiteLabelingParamsProto;
import org.thingsboard.server.service.edge.rpc.processor.BaseEdgeProcessor;

@Component
@Slf4j
public class WhiteLabelingCloudProcessor extends BaseEdgeProcessor {

    @Autowired
    private CustomTranslationService customTranslationService;

    @Autowired
    private WhiteLabelingService whiteLabelingService;

    public ListenableFuture<Void> processCustomTranslationMsgFromCloud(TenantId tenantId, CustomTranslationProto customTranslationProto) {
        try {
            EntityId entityId = constructEntityId(customTranslationProto.getEntityType(), customTranslationProto.getEntityIdMSB(), customTranslationProto.getEntityIdLSB());
            CustomTranslation customTranslation = new CustomTranslation();
            if (!customTranslationProto.getTranslationMapMap().isEmpty()) {
                customTranslation.setTranslationMap(customTranslationProto.getTranslationMapMap());
            }
            switch (entityId.getEntityType()) {
                case TENANT:
                    if (EntityId.NULL_UUID.equals(entityId.getId())) {
                        customTranslationService.saveSystemCustomTranslation(customTranslation);
                    } else {
                        customTranslationService.saveTenantCustomTranslation(tenantId, customTranslation);
                    }
                    break;
                case CUSTOMER:
                    customTranslationService.saveCustomerCustomTranslation(tenantId, new CustomerId(entityId.getId()), customTranslation);
                    break;
            }
        } catch (Exception e) {
            String errMsg = "Exception during updating custom translation";
            log.error(errMsg, e);
            return Futures.immediateFailedFuture(new RuntimeException(errMsg, e));
        }
        return Futures.immediateFuture(null);
    }

    public ListenableFuture<Void> processLoginWhiteLabelingParamsMsgFromCloud(TenantId tenantId,
                                                                              LoginWhiteLabelingParamsProto loginWhiteLabelingParamsProto) {
        try {
            EntityId entityId = constructEntityId(loginWhiteLabelingParamsProto.getWhiteLabelingParams().getEntityType(),
                    loginWhiteLabelingParamsProto.getWhiteLabelingParams().getEntityIdMSB(),
                    loginWhiteLabelingParamsProto.getWhiteLabelingParams().getEntityIdLSB());
            LoginWhiteLabelingParams loginWhiteLabelingParams = constructLoginWhiteLabelingParams(loginWhiteLabelingParamsProto);
            switch (entityId.getEntityType()) {
                case TENANT:
                    if (EntityId.NULL_UUID.equals(entityId.getId())) {
                        whiteLabelingService.saveSystemLoginWhiteLabelingParams(loginWhiteLabelingParams);
                    } else {
                        whiteLabelingService.saveTenantLoginWhiteLabelingParams(tenantId, loginWhiteLabelingParams);
                    }
                    break;
                case CUSTOMER:
                    whiteLabelingService.saveCustomerLoginWhiteLabelingParams(tenantId, new CustomerId(entityId.getId()), loginWhiteLabelingParams);
                    break;
            }
        } catch (Exception e) {
            String errMsg = "Exception during updating login white labeling params";
            log.error(errMsg, e);
            return Futures.immediateFailedFuture(new RuntimeException(errMsg, e));
        }
        return Futures.immediateFuture(null);
    }

    private LoginWhiteLabelingParams constructLoginWhiteLabelingParams(LoginWhiteLabelingParamsProto loginWLPProto) {
        LoginWhiteLabelingParams loginWLP = new LoginWhiteLabelingParams();
        WhiteLabelingParams whiteLabelingParams = constructWhiteLabelingParams(loginWLPProto.getWhiteLabelingParams());
        loginWLP.merge(whiteLabelingParams);
        loginWLP.setPageBackgroundColor(loginWLPProto.hasPageBackgroundColor() ? loginWLPProto.getPageBackgroundColor() : null);
        loginWLP.setDarkForeground(loginWLPProto.getDarkForeground());
        loginWLP.setDomainName(loginWLPProto.hasDomainName() ? loginWLPProto.getDomainName() : null);
        loginWLP.setAdminSettingsId(loginWLPProto.hasAdminSettingsId() ? loginWLPProto.getAdminSettingsId() : null);
        loginWLP.setShowNameBottom(loginWLPProto.hasShowNameBottom() ? loginWLPProto.getShowNameBottom() : null);
        return loginWLP;
    }

    public ListenableFuture<Void> processWhiteLabelingParamsMsgFromCloud(TenantId tenantId, WhiteLabelingParamsProto wLPProto) {
        try {
            WhiteLabelingParams wLP = constructWhiteLabelingParams(wLPProto);
            EntityId entityId = constructEntityId(wLPProto.getEntityType(), wLPProto.getEntityIdMSB(), wLPProto.getEntityIdLSB());
            switch (entityId.getEntityType()) {
                case TENANT:
                    if (EntityId.NULL_UUID.equals(entityId.getId())) {
                        whiteLabelingService.saveSystemWhiteLabelingParams(wLP);
                    } else {
                        whiteLabelingService.saveTenantWhiteLabelingParams(tenantId, wLP);
                    }
                    break;
                case CUSTOMER:
                    whiteLabelingService.saveCustomerWhiteLabelingParams(tenantId, new CustomerId(entityId.getId()), wLP);
                    break;
            }
        } catch (Exception e) {
            String errMsg = "Exception during updating white labeling params";
            log.error(errMsg, e);
            return Futures.immediateFailedFuture(new RuntimeException(errMsg, e));
        }
        return Futures.immediateFuture(null);
    }

    private WhiteLabelingParams constructWhiteLabelingParams(WhiteLabelingParamsProto whiteLabelingParamsProto) {
        WhiteLabelingParams whiteLabelingParams = new WhiteLabelingParams();
        whiteLabelingParams.setLogoImageUrl(whiteLabelingParamsProto.hasLogoImageUrl() ? whiteLabelingParamsProto.getLogoImageUrl() : null);
        whiteLabelingParams.setLogoImageChecksum(whiteLabelingParamsProto.hasLogoImageChecksum() ? whiteLabelingParamsProto.getLogoImageChecksum() : null);
        whiteLabelingParams.setLogoImageHeight(whiteLabelingParamsProto.hasLogoImageHeight() ? (int) whiteLabelingParamsProto.getLogoImageHeight() : null);
        whiteLabelingParams.setAppTitle(whiteLabelingParamsProto.hasAppTitle() ? whiteLabelingParamsProto.getAppTitle() : null);
        whiteLabelingParams.setFavicon(constructFavicon(whiteLabelingParamsProto.getFavicon()));
        whiteLabelingParams.setFaviconChecksum(whiteLabelingParamsProto.hasFaviconChecksum() ? whiteLabelingParamsProto.getFaviconChecksum() : null);
        whiteLabelingParams.setPaletteSettings(constructPaletteSettings(whiteLabelingParamsProto.getPaletteSettings()));
        whiteLabelingParams.setHelpLinkBaseUrl(whiteLabelingParamsProto.hasHelpLinkBaseUrl() ? whiteLabelingParamsProto.getHelpLinkBaseUrl() : null);
        whiteLabelingParams.setEnableHelpLinks(whiteLabelingParamsProto.hasEnableHelpLinks() ? whiteLabelingParamsProto.getEnableHelpLinks() : null);
        whiteLabelingParams.setShowNameVersion(whiteLabelingParamsProto.hasShowNameVersion() ? whiteLabelingParamsProto.getShowNameVersion() : null);
        whiteLabelingParams.setPlatformName(whiteLabelingParamsProto.hasPlatformName() ? whiteLabelingParamsProto.getPlatformName() : null);
        whiteLabelingParams.setPlatformVersion(whiteLabelingParamsProto.hasPlatformVersion() ? whiteLabelingParamsProto.getPlatformVersion() : null);
        return whiteLabelingParams;
    }

    private Favicon constructFavicon(FaviconProto faviconProto) {
        if (!faviconProto.hasUrl() && !faviconProto.hasType()) {
            return null;
        }
        Favicon favicon = new Favicon();
        favicon.setUrl(faviconProto.hasUrl() ? faviconProto.getUrl() : null);
        favicon.setType(faviconProto.hasType() ? faviconProto.getType() : null);
        return favicon;
    }

    private PaletteSettings constructPaletteSettings(PaletteSettingsProto paletteSettingsProto) {
        Palette primaryPalette = constructPalette(paletteSettingsProto.getPrimaryPalette());
        Palette accentPalette = constructPalette(paletteSettingsProto.getAccentPalette());
        if (primaryPalette == null && accentPalette == null) {
            return null;
        }
        PaletteSettings paletteSettings = new PaletteSettings();
        paletteSettings.setPrimaryPalette(primaryPalette);
        paletteSettings.setAccentPalette(accentPalette);
        return paletteSettings;
    }

    private Palette constructPalette(PaletteProto paletteProto) {
        if (!paletteProto.hasType()
                && !paletteProto.hasExtendsPalette()
                && paletteProto.getColorsMap().isEmpty()) {
            return null;
        }
        Palette palette = new Palette();
        palette.setType(paletteProto.hasType() ? paletteProto.getType() : null);
        palette.setExtendsPalette(paletteProto.hasExtendsPalette() ? paletteProto.getExtendsPalette() : null);
        palette.setColors(!paletteProto.getColorsMap().isEmpty() ? paletteProto.getColorsMap() : null);
        return palette;
    }
}
