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
package org.thingsboard.server.service.edge.rpc.constructor.wl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.wl.Favicon;
import org.thingsboard.server.common.data.wl.LoginWhiteLabelingParams;
import org.thingsboard.server.common.data.wl.Palette;
import org.thingsboard.server.common.data.wl.PaletteSettings;
import org.thingsboard.server.common.data.wl.WhiteLabeling;
import org.thingsboard.server.common.data.wl.WhiteLabelingParams;
import org.thingsboard.server.dao.resource.ImageService;
import org.thingsboard.server.gen.edge.v1.FaviconProto;
import org.thingsboard.server.gen.edge.v1.LoginWhiteLabelingParamsProto;
import org.thingsboard.server.gen.edge.v1.PaletteProto;
import org.thingsboard.server.gen.edge.v1.PaletteSettingsProto;
import org.thingsboard.server.gen.edge.v1.WhiteLabelingParamsProto;
import org.thingsboard.server.gen.edge.v1.WhiteLabelingProto;

@Component
@Slf4j
public class WhiteLabelingParamsProtoConstructor {

    @Autowired
    private ImageService imageService;

    public WhiteLabelingProto constructWhiteLabeling(WhiteLabeling whiteLabeling, boolean isEdgeOlderThan_3_6_2) {
        if (isEdgeOlderThan_3_6_2) {
            imageService.inlineImagesForEdge(whiteLabeling.getTenantId(), whiteLabeling.getSettings());
            JsonNode jsonNode = JacksonUtil.valueToTree(whiteLabeling);
            if (jsonNode != null) {
                JsonNode entityId =  JacksonUtil.valueToTree(getEntityIdForEvent(whiteLabeling));
                ((ObjectNode) jsonNode).set("entityId", entityId);
                return WhiteLabelingProto.newBuilder().setEntity(jsonNode.toString()).build();
            }
        }
        return WhiteLabelingProto.newBuilder().setEntity(JacksonUtil.toString(whiteLabeling)).build();
    }

    public LoginWhiteLabelingParamsProto constructLoginWhiteLabelingParamsProto(TenantId tenantId, LoginWhiteLabelingParams loginWhiteLabelingParams, EntityId entityId) {
        JsonNode settings = JacksonUtil.valueToTree(loginWhiteLabelingParams);
        imageService.inlineImagesForEdge(tenantId, settings);
        loginWhiteLabelingParams = JacksonUtil.treeToValue(settings, LoginWhiteLabelingParams.class);
        LoginWhiteLabelingParamsProto.Builder builder = LoginWhiteLabelingParamsProto.newBuilder();
        if (loginWhiteLabelingParams.getPageBackgroundColor() != null) {
            builder.setPageBackgroundColor(loginWhiteLabelingParams.getPageBackgroundColor());
        }
        builder.setDarkForeground(loginWhiteLabelingParams.isDarkForeground());
        if (loginWhiteLabelingParams.getDomainName() != null) {
            builder.setDomainName(loginWhiteLabelingParams.getDomainName());
        }
        if (loginWhiteLabelingParams.getShowNameBottom() != null) {
            builder.setShowNameBottom(loginWhiteLabelingParams.getShowNameBottom());
        }
        if (loginWhiteLabelingParams.getAdminSettingsId() != null) {
            builder.setAdminSettingsId(loginWhiteLabelingParams.getAdminSettingsId());
        }
        builder.setWhiteLabelingParams(constructWhiteLabelingParamsProto(tenantId, loginWhiteLabelingParams, entityId));
        return builder.build();
    }

    public WhiteLabelingParamsProto constructWhiteLabelingParamsProto(TenantId tenantId, WhiteLabelingParams whiteLabelingParams, EntityId entityId) {
        JsonNode settings = JacksonUtil.valueToTree(whiteLabelingParams);
        imageService.inlineImagesForEdge(tenantId, settings);
        whiteLabelingParams = JacksonUtil.treeToValue(settings, WhiteLabelingParams.class);
        WhiteLabelingParamsProto.Builder builder = WhiteLabelingParamsProto.newBuilder();
        builder.setEntityIdMSB(entityId.getId().getMostSignificantBits())
                .setEntityIdLSB(entityId.getId().getLeastSignificantBits())
                .setEntityType(entityId.getEntityType().name());
        if (whiteLabelingParams.getLogoImageUrl() != null) {
            builder.setLogoImageUrl(whiteLabelingParams.getLogoImageUrl());
        }
        if (whiteLabelingParams.getLogoImageHeight() != null) {
            builder.setLogoImageHeight(whiteLabelingParams.getLogoImageHeight().longValue());
        }
        if (whiteLabelingParams.getAppTitle() != null) {
            builder.setAppTitle(whiteLabelingParams.getAppTitle());
        }
        if (whiteLabelingParams.getFavicon() != null) {
            builder.setFavicon(constructFaviconProto(whiteLabelingParams.getFavicon()));
        }
        if (whiteLabelingParams.getPaletteSettings() != null) {
            builder.setPaletteSettings(constructPaletteSettingsProto(whiteLabelingParams.getPaletteSettings()));
        }
        if (whiteLabelingParams.getHelpLinkBaseUrl() != null) {
            builder.setHelpLinkBaseUrl(whiteLabelingParams.getHelpLinkBaseUrl());
        }
        if (whiteLabelingParams.getUiHelpBaseUrl() != null) {
            builder.setUiHelpBaseUrl(whiteLabelingParams.getUiHelpBaseUrl());
        }
        if (whiteLabelingParams.getEnableHelpLinks() != null) {
            builder.setEnableHelpLinks(whiteLabelingParams.getEnableHelpLinks());
        }
        if (whiteLabelingParams.getShowNameVersion() != null) {
            builder.setShowNameVersion(whiteLabelingParams.getShowNameVersion());
        }
        if (whiteLabelingParams.getPlatformName() != null) {
            builder.setPlatformName(whiteLabelingParams.getPlatformName());
        }
        if (whiteLabelingParams.getPlatformVersion() != null) {
            builder.setPlatformVersion(whiteLabelingParams.getPlatformVersion());
        }
        return builder.build();
    }

    private FaviconProto constructFaviconProto(Favicon favicon) {
        FaviconProto.Builder builder = FaviconProto.newBuilder();
        if (favicon.getUrl() != null) {
            builder.setUrl(favicon.getUrl());
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
            builder.setType(palette.getType());
        }
        if (palette.getExtendsPalette() != null) {
            builder.setExtendsPalette(palette.getExtendsPalette());
        }
        if (palette.getColors() != null && !palette.getColors().isEmpty()) {
            builder.putAllColors(palette.getColors());
        }
        return builder.build();
    }

    private static EntityId getEntityIdForEvent(WhiteLabeling whiteLabeling) {
        return whiteLabeling.getCustomerId() != null && !whiteLabeling.getCustomerId().isNullUid() ? whiteLabeling.getCustomerId() : whiteLabeling.getTenantId();
    }
}
