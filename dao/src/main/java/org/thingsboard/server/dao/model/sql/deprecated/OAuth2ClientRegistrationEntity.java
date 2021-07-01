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
package org.thingsboard.server.dao.model.sql.deprecated;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.TypeDef;
import org.thingsboard.server.common.data.id.deprecated.OAuth2ClientRegistrationId;
import org.thingsboard.server.common.data.id.deprecated.OAuth2ClientRegistrationInfoId;
import org.thingsboard.server.common.data.oauth2.*;
import org.thingsboard.server.common.data.oauth2.deprecated.OAuth2ClientRegistration;
import org.thingsboard.server.dao.model.BaseSqlEntity;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.util.mapping.JsonStringType;

import javax.persistence.*;
import java.util.UUID;

@Deprecated
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@TypeDef(name = "json", typeClass = JsonStringType.class)
@Table(name = ModelConstants.OAUTH2_CLIENT_REGISTRATION_COLUMN_FAMILY_NAME)
public class OAuth2ClientRegistrationEntity extends BaseSqlEntity<OAuth2ClientRegistration> {

    @Column(name = ModelConstants.OAUTH2_CLIENT_REGISTRATION_INFO_ID_PROPERTY, columnDefinition = "uuid")
    private UUID clientRegistrationInfoId;

    @Column(name = ModelConstants.OAUTH2_DOMAIN_NAME_PROPERTY)
    private String domainName;

    @Enumerated(EnumType.STRING)
    @Column(name = ModelConstants.OAUTH2_DOMAIN_SCHEME_PROPERTY)
    private SchemeType domainScheme;

    public OAuth2ClientRegistrationEntity() {
        super();
    }

    public OAuth2ClientRegistrationEntity(OAuth2ClientRegistration clientRegistration) {
        if (clientRegistration.getId() != null) {
            this.setUuid(clientRegistration.getId().getId());
        }
        if (clientRegistration.getClientRegistrationId() != null){
            this.clientRegistrationInfoId = clientRegistration.getClientRegistrationId().getId();
        }
        this.createdTime = clientRegistration.getCreatedTime();
        this.domainName = clientRegistration.getDomainName();
        this.domainScheme = clientRegistration.getDomainScheme();
    }

    @Override
    public OAuth2ClientRegistration toData() {
        OAuth2ClientRegistration clientRegistration = new OAuth2ClientRegistration();
        clientRegistration.setId(new OAuth2ClientRegistrationId(id));
        clientRegistration.setClientRegistrationId(new OAuth2ClientRegistrationInfoId(clientRegistrationInfoId));
        clientRegistration.setCreatedTime(createdTime);
        clientRegistration.setDomainName(domainName);
        clientRegistration.setDomainScheme(domainScheme);
        return clientRegistration;
    }
}
