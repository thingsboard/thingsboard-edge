/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2025 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.dao.model.sql;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.Data;
import org.thingsboard.server.common.data.id.MobileAppBundleId;
import org.thingsboard.server.common.data.id.OAuth2ClientId;
import org.thingsboard.server.common.data.mobile.bundle.MobileAppBundleOauth2Client;
import org.thingsboard.server.dao.model.ToData;

import java.util.UUID;

import static org.thingsboard.server.dao.model.ModelConstants.MOBILE_APP_BUNDLE_OAUTH2_CLIENT_CLIENT_ID_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.MOBILE_APP_BUNDLE_OAUTH2_CLIENT_MOBILE_APP_BUNDLE_ID_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.MOBILE_APP_BUNDLE_OAUTH2_CLIENT_TABLE_NAME;


@Data
@Entity
@Table(name = MOBILE_APP_BUNDLE_OAUTH2_CLIENT_TABLE_NAME)
@IdClass(MobileAppOauth2ClientCompositeKey.class)
public final class MobileAppBundleOauth2ClientEntity implements ToData<MobileAppBundleOauth2Client> {

    @Id
    @Column(name = MOBILE_APP_BUNDLE_OAUTH2_CLIENT_MOBILE_APP_BUNDLE_ID_PROPERTY, columnDefinition = "uuid")
    private UUID mobileAppBundleId;

    @Id
    @Column(name = MOBILE_APP_BUNDLE_OAUTH2_CLIENT_CLIENT_ID_PROPERTY, columnDefinition = "uuid")
    private UUID oauth2ClientId;

    public MobileAppBundleOauth2ClientEntity() {
        super();
    }

    public MobileAppBundleOauth2ClientEntity(MobileAppBundleOauth2Client mobileAppBundleOauth2Client) {
        mobileAppBundleId = mobileAppBundleOauth2Client.getMobileAppBundleId().getId();
        oauth2ClientId = mobileAppBundleOauth2Client.getOAuth2ClientId().getId();
    }

    @Override
    public MobileAppBundleOauth2Client toData() {
        MobileAppBundleOauth2Client result = new MobileAppBundleOauth2Client();
        result.setMobileAppBundleId(new MobileAppBundleId(mobileAppBundleId));
        result.setOAuth2ClientId(new OAuth2ClientId(oauth2ClientId));
        return result;
    }
}
