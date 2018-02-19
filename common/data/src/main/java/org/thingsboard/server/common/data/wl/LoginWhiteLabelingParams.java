package org.thingsboard.server.common.data.wl;


import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode
public class LoginWhiteLabelingParams extends WhiteLabelingParams {

    private String pageBackgroundColor;
    private boolean darkForeground;

    public LoginWhiteLabelingParams merge(WhiteLabelingParams otherWlParams) {
        Integer prevLogoImageHeight = this.logoImageHeight;
        super.merge(otherWlParams);
        if (prevLogoImageHeight == null) {
            this.logoImageHeight = null;
        }
        return this;
    }
}
