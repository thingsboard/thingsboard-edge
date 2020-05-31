package org.thingsboard.server.service.edge.rpc.constructor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.wl.Favicon;
import org.thingsboard.server.common.data.wl.LoginWhiteLabelingParams;
import org.thingsboard.server.common.data.wl.Palette;
import org.thingsboard.server.common.data.wl.PaletteSettings;
import org.thingsboard.server.common.data.wl.WhiteLabelingParams;
import org.thingsboard.server.gen.edge.FaviconProto;
import org.thingsboard.server.gen.edge.LoginWhiteLabelingParamsProto;
import org.thingsboard.server.gen.edge.PaletteProto;
import org.thingsboard.server.gen.edge.PaletteSettingsProto;
import org.thingsboard.server.gen.edge.WhiteLabelingParamsProto;

@Component
@Slf4j
public class WhiteLabelingParamsProtoConstructor {

    public LoginWhiteLabelingParamsProto constructLoginWhiteLabelingParamsProto(LoginWhiteLabelingParams loginWhiteLabelingParams) {
        LoginWhiteLabelingParamsProto.Builder builder = LoginWhiteLabelingParamsProto.newBuilder();
        builder.setPageBackgroundColor(loginWhiteLabelingParams.getPageBackgroundColor())
                .setDarkForeground(loginWhiteLabelingParams.isDarkForeground())
                .setDomainName(loginWhiteLabelingParams.getDomainName())
                .setAdminSettingsId(loginWhiteLabelingParams.getAdminSettingsId())
                .setShowNameBottom(loginWhiteLabelingParams.getShowNameBottom())
                .setLogoImageUrl(loginWhiteLabelingParams.getLogoImageUrl())
                .setLogoImageChecksum(loginWhiteLabelingParams.getLogoImageChecksum())
                .setLogoImageHeight(loginWhiteLabelingParams.getLogoImageHeight())
                .setAppTitle(loginWhiteLabelingParams.getAppTitle())
                .setFavicon(constructFaviconProto(loginWhiteLabelingParams.getFavicon()))
                .setFaviconChecksum(loginWhiteLabelingParams.getFaviconChecksum())
                .setPaletteSettings(constructPaletteSettingsProto(loginWhiteLabelingParams.getPaletteSettings()))
                .setHelpLinkBaseUrl(loginWhiteLabelingParams.getHelpLinkBaseUrl())
                .setEnableHelpLinks(loginWhiteLabelingParams.getEnableHelpLinks())
                .setWhiteLabelingEnabled(loginWhiteLabelingParams.isWhiteLabelingEnabled())
                .setShowNameVersion(loginWhiteLabelingParams.getShowNameVersion())
                .setPlatformName(loginWhiteLabelingParams.getPlatformName())
                .setPlatformVersion(loginWhiteLabelingParams.getPlatformVersion());
        return builder.build();
    }

    public WhiteLabelingParamsProto constructWhiteLabelingParamsProto(WhiteLabelingParams whiteLabelingParams) {
        WhiteLabelingParamsProto.Builder builder = WhiteLabelingParamsProto.newBuilder();
        builder.setLogoImageUrl(whiteLabelingParams.getLogoImageUrl())
                .setLogoImageChecksum(whiteLabelingParams.getLogoImageChecksum())
                .setLogoImageHeight(whiteLabelingParams.getLogoImageHeight())
                .setAppTitle(whiteLabelingParams.getAppTitle())
                .setFavicon(constructFaviconProto(whiteLabelingParams.getFavicon()))
                .setFaviconChecksum(whiteLabelingParams.getFaviconChecksum())
                .setPaletteSettings(constructPaletteSettingsProto(whiteLabelingParams.getPaletteSettings()))
                .setHelpLinkBaseUrl(whiteLabelingParams.getHelpLinkBaseUrl())
                .setEnableHelpLinks(whiteLabelingParams.getEnableHelpLinks())
                .setWhiteLabelingEnabled(whiteLabelingParams.isWhiteLabelingEnabled())
                .setShowNameVersion(whiteLabelingParams.getShowNameVersion())
                .setPlatformName(whiteLabelingParams.getPlatformName())
                .setPlatformVersion(whiteLabelingParams.getPlatformVersion());
        return builder.build();
    }

    private FaviconProto constructFaviconProto(Favicon favicon) {
        FaviconProto.Builder builder = FaviconProto.newBuilder()
                .setUrl(favicon.getUrl())
                .setType(favicon.getType());
        return builder.build();
    }

    private PaletteSettingsProto constructPaletteSettingsProto(PaletteSettings paletteSettings) {
        PaletteSettingsProto.Builder builder = PaletteSettingsProto.newBuilder()
                .setPrimaryPalette(constructPaletteProto(paletteSettings.getPrimaryPalette()))
                .setAccentPalette(constructPaletteProto(paletteSettings.getAccentPalette()));
        return builder.build();
    }

    private PaletteProto constructPaletteProto(Palette palette) {
        PaletteProto.Builder builder = PaletteProto.newBuilder()
                .setType(palette.getType())
                .setExtendsPalette(palette.getExtendsPalette());
        builder.getColorsMap().putAll(palette.getColors());
        return builder.build();
    }
}
