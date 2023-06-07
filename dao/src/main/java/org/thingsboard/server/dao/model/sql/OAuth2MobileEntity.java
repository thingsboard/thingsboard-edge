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
package org.thingsboard.server.dao.model.sql;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.id.OAuth2MobileId;
import org.thingsboard.server.common.data.id.OAuth2ParamsId;
import org.thingsboard.server.common.data.oauth2.OAuth2Mobile;
import org.thingsboard.server.dao.model.BaseSqlEntity;
import org.thingsboard.server.dao.model.ModelConstants;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = ModelConstants.OAUTH2_MOBILE_TABLE_NAME)
public class OAuth2MobileEntity extends BaseSqlEntity<OAuth2Mobile> {

    @Column(name = ModelConstants.OAUTH2_PARAMS_ID_PROPERTY)
    private UUID oauth2ParamsId;

    @Column(name = ModelConstants.OAUTH2_PKG_NAME_PROPERTY)
    private String pkgName;

    @Column(name = ModelConstants.OAUTH2_APP_SECRET_PROPERTY)
    private String appSecret;

    public OAuth2MobileEntity() {
        super();
    }

    public OAuth2MobileEntity(OAuth2Mobile mobile) {
        if (mobile.getId() != null) {
            this.setUuid(mobile.getId().getId());
        }
        this.setCreatedTime(mobile.getCreatedTime());
        if (mobile.getOauth2ParamsId() != null) {
            this.oauth2ParamsId = mobile.getOauth2ParamsId().getId();
        }
        this.pkgName = mobile.getPkgName();
        this.appSecret = mobile.getAppSecret();
    }

    @Override
    public OAuth2Mobile toData() {
        OAuth2Mobile mobile = new OAuth2Mobile();
        mobile.setId(new OAuth2MobileId(id));
        mobile.setCreatedTime(createdTime);
        mobile.setOauth2ParamsId(new OAuth2ParamsId(oauth2ParamsId));
        mobile.setPkgName(pkgName);
        mobile.setAppSecret(appSecret);
        return mobile;
    }
}
