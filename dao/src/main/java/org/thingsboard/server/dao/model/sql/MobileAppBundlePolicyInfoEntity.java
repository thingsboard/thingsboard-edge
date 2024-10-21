package org.thingsboard.server.dao.model.sql;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.mobile.MobileAppBundlePolicyInfo;
import org.thingsboard.server.dao.model.ModelConstants;

import static org.thingsboard.server.dao.model.ModelConstants.MOBILE_APP_BUNDLE_TABLE_NAME;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = MOBILE_APP_BUNDLE_TABLE_NAME)
public final class MobileAppBundlePolicyInfoEntity extends AbstractMobileAppBundleEntity<MobileAppBundlePolicyInfo> {

    @Column(name = ModelConstants.MOBILE_APP_BUNDLE_TERMS_OF_USE_PROPERTY)
    private String termsOfUse;

    @Column(name = ModelConstants.MOBILE_APP_BUNDLE_PRIVACY_POLICY_PROPERTY)
    private String privacyPolicy;

    public MobileAppBundlePolicyInfoEntity() {
        super();
    }

    public MobileAppBundlePolicyInfoEntity(MobileAppBundlePolicyInfo mobileAppBundlePolicyInfo) {
        super(mobileAppBundlePolicyInfo);
        this.termsOfUse = mobileAppBundlePolicyInfo.getTermsOfUse();
        this.privacyPolicy = mobileAppBundlePolicyInfo.getPrivacyPolicy();
    }

    @Override
    public MobileAppBundlePolicyInfo toData() {
        return new MobileAppBundlePolicyInfo(super.toMobileAppBundle(), termsOfUse, privacyPolicy);
    }
}
