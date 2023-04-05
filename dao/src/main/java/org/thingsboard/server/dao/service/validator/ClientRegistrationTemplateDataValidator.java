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
package org.thingsboard.server.dao.service.validator;

import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.oauth2.OAuth2ClientRegistrationTemplate;
import org.thingsboard.server.exception.DataValidationException;
import org.thingsboard.server.dao.service.DataValidator;

@Component
public class ClientRegistrationTemplateDataValidator extends DataValidator<OAuth2ClientRegistrationTemplate> {

    @Override
    protected void validateCreate(TenantId tenantId, OAuth2ClientRegistrationTemplate clientRegistrationTemplate) {
    }

    @Override
    protected OAuth2ClientRegistrationTemplate validateUpdate(TenantId tenantId, OAuth2ClientRegistrationTemplate clientRegistrationTemplate) {
        return null;
    }

    @Override
    protected void validateDataImpl(TenantId tenantId, OAuth2ClientRegistrationTemplate clientRegistrationTemplate) {
        if (StringUtils.isEmpty(clientRegistrationTemplate.getProviderId())) {
            throw new DataValidationException("Provider ID should be specified!");
        }
        if (clientRegistrationTemplate.getMapperConfig() == null) {
            throw new DataValidationException("Mapper config should be specified!");
        }
        if (clientRegistrationTemplate.getMapperConfig().getType() == null) {
            throw new DataValidationException("Mapper type should be specified!");
        }
        if (clientRegistrationTemplate.getMapperConfig().getBasic() == null) {
            throw new DataValidationException("Basic mapper config should be specified!");
        }
    }
}
