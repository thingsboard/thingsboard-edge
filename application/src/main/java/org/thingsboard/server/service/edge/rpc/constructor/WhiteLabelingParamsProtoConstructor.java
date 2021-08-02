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
package org.thingsboard.server.service.edge.rpc.constructor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.wl.Favicon;
import org.thingsboard.server.common.data.wl.LoginWhiteLabelingParams;
import org.thingsboard.server.common.data.wl.Palette;
import org.thingsboard.server.common.data.wl.PaletteSettings;
import org.thingsboard.server.common.data.wl.WhiteLabelingParams;
import org.thingsboard.server.gen.edge.v1.FaviconProto;
import org.thingsboard.server.gen.edge.v1.LoginWhiteLabelingParamsProto;
import org.thingsboard.server.gen.edge.v1.PaletteProto;
import org.thingsboard.server.gen.edge.v1.PaletteSettingsProto;
import org.thingsboard.server.gen.edge.v1.WhiteLabelingParamsProto;

import static org.thingsboard.server.service.edge.rpc.EdgeProtoUtils.getBoolValue;
import static org.thingsboard.server.service.edge.rpc.EdgeProtoUtils.getInt64Value;
import static org.thingsboard.server.service.edge.rpc.EdgeProtoUtils.getStringValue;

@Component
@Slf4j
public class WhiteLabelingParamsProtoConstructor {

    public LoginWhiteLabelingParamsProto constructLoginWhiteLabelingParamsProto(LoginWhiteLabelingParams loginWhiteLabelingParams) {
        LoginWhiteLabelingParamsProto.Builder builder = LoginWhiteLabelingParamsProto.newBuilder();
        if (loginWhiteLabelingParams.getPageBackgroundColor() != null) {
            builder.setPageBackgroundColor(getStringValue(loginWhiteLabelingParams.getPageBackgroundColor()));
        }
        builder.setDarkForeground(loginWhiteLabelingParams.isDarkForeground());
        if (loginWhiteLabelingParams.getDomainName() != null) {
            builder.setDomainName(getStringValue(loginWhiteLabelingParams.getDomainName()));
        }
        builder.setShowNameBottom(getBoolValue(loginWhiteLabelingParams.getShowNameBottom()));
        if (loginWhiteLabelingParams.getAdminSettingsId() != null) {
            builder.setAdminSettingsId(getStringValue(loginWhiteLabelingParams.getAdminSettingsId()));
        }
        builder.setWhiteLabelingParams(constructWhiteLabelingParamsProto(loginWhiteLabelingParams));
        return builder.build();
    }

    public WhiteLabelingParamsProto constructWhiteLabelingParamsProto(WhiteLabelingParams whiteLabelingParams) {
        WhiteLabelingParamsProto.Builder builder = WhiteLabelingParamsProto.newBuilder();
        if (whiteLabelingParams.getLogoImageUrl() != null) {
            builder.setLogoImageUrl(getStringValue(whiteLabelingParams.getLogoImageUrl()));
        }
        if (whiteLabelingParams.getLogoImageChecksum() != null) {
            builder.setLogoImageChecksum(getStringValue(whiteLabelingParams.getLogoImageChecksum()));
        }
        if (whiteLabelingParams.getLogoImageHeight() != null) {
            builder.setLogoImageHeight(getInt64Value(whiteLabelingParams.getLogoImageHeight().longValue()));
        }
        if (whiteLabelingParams.getAppTitle() != null) {
            builder.setAppTitle(getStringValue(whiteLabelingParams.getAppTitle()));
        }
        if (whiteLabelingParams.getFavicon() != null) {
            builder.setFavicon(constructFaviconProto(whiteLabelingParams.getFavicon()));
        }
        if (whiteLabelingParams.getFaviconChecksum() != null) {
            builder.setFaviconChecksum(getStringValue(whiteLabelingParams.getFaviconChecksum()));
        }
        if (whiteLabelingParams.getPaletteSettings() != null) {
            builder.setPaletteSettings(constructPaletteSettingsProto(whiteLabelingParams.getPaletteSettings()));
        }
        if (whiteLabelingParams.getHelpLinkBaseUrl() != null) {
            builder.setHelpLinkBaseUrl(getStringValue(whiteLabelingParams.getHelpLinkBaseUrl()));
        }
        if (whiteLabelingParams.getEnableHelpLinks() != null) {
            builder.setEnableHelpLinks(getBoolValue(whiteLabelingParams.getEnableHelpLinks()));
        }
        if (whiteLabelingParams.getShowNameVersion() != null) {
            builder.setShowNameVersion(getBoolValue(whiteLabelingParams.getShowNameVersion()));
        }
        if (whiteLabelingParams.getPlatformName() != null) {
            builder.setPlatformName(getStringValue(whiteLabelingParams.getPlatformName()));
        }
        if (whiteLabelingParams.getPlatformVersion() != null) {
            builder.setPlatformVersion(getStringValue(whiteLabelingParams.getPlatformVersion()));
        }
        return builder.build();
    }

    private FaviconProto constructFaviconProto(Favicon favicon) {
        FaviconProto.Builder builder = FaviconProto.newBuilder();
        if (favicon.getUrl() != null) {
            builder.setUrl(getStringValue(favicon.getUrl()));
        }
        if (favicon.getType() != null) {
            builder.setType(getStringValue(favicon.getType()));
        }
        return builder.build();
    }

    private PaletteSettingsProto constructPaletteSettingsProto(PaletteSettings paletteSettings) {
        PaletteSettingsProto.Builder builder = PaletteSettingsProto.newBuilder();
        if (paletteSettings.getPrimaryPalette() != null) {
            builder.setPrimaryPalette(constructPaletteProto(paletteSettings.getPrimaryPalette()));
        }
        if (paletteSettings.getAccentPalette() != null) {
            builder.setAccentPalette(constructPaletteProto(paletteSettings.getAccentPalette()));
        }
        return builder.build();
    }

    private PaletteProto constructPaletteProto(Palette palette) {
        PaletteProto.Builder builder = PaletteProto.newBuilder();
        if (palette.getType() != null) {
            builder.setType(getStringValue(palette.getType()));
        }
        if (palette.getExtendsPalette() != null) {
            builder.setExtendsPalette(getStringValue(palette.getExtendsPalette()));
        }
        if (palette.getColors() != null && !palette.getColors().isEmpty()) {
            builder.putAllColors(palette.getColors());
        }
        return builder.build();
    }
}
