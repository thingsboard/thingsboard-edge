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
import org.thingsboard.server.common.data.id.OAuth2DomainId;
import org.thingsboard.server.common.data.id.OAuth2ParamsId;
import org.thingsboard.server.common.data.oauth2.OAuth2Domain;
import org.thingsboard.server.common.data.oauth2.SchemeType;
import org.thingsboard.server.dao.model.BaseSqlEntity;
import org.thingsboard.server.dao.model.ModelConstants;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Table;
import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = ModelConstants.OAUTH2_DOMAIN_COLUMN_FAMILY_NAME)
public class OAuth2DomainEntity extends BaseSqlEntity<OAuth2Domain> {

    @Column(name = ModelConstants.OAUTH2_PARAMS_ID_PROPERTY)
    private UUID oauth2ParamsId;

    @Column(name = ModelConstants.OAUTH2_DOMAIN_NAME_PROPERTY)
    private String domainName;

    @Enumerated(EnumType.STRING)
    @Column(name = ModelConstants.OAUTH2_DOMAIN_SCHEME_PROPERTY)
    private SchemeType domainScheme;

    public OAuth2DomainEntity() {
        super();
    }

    public OAuth2DomainEntity(OAuth2Domain domain) {
        if (domain.getId() != null) {
            this.setUuid(domain.getId().getId());
        }
        this.setCreatedTime(domain.getCreatedTime());
        if (domain.getOauth2ParamsId() != null) {
            this.oauth2ParamsId = domain.getOauth2ParamsId().getId();
        }
        this.domainName = domain.getDomainName();
        this.domainScheme = domain.getDomainScheme();
    }

    @Override
    public OAuth2Domain toData() {
        OAuth2Domain domain = new OAuth2Domain();
        domain.setId(new OAuth2DomainId(id));
        domain.setCreatedTime(createdTime);
        domain.setOauth2ParamsId(new OAuth2ParamsId(oauth2ParamsId));
        domain.setDomainName(domainName);
        domain.setDomainScheme(domainScheme);
        return domain;
    }
}
