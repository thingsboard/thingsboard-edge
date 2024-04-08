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
package org.thingsboard.server.service.translation;

import com.fasterxml.jackson.databind.JsonNode;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.translation.CustomTranslation;
import org.thingsboard.server.common.data.translation.TranslationInfo;
import org.thingsboard.server.dao.translation.TranslationCacheKey;

import java.util.List;

public interface TbTranslationService {

    List<TranslationInfo> getTranslationInfos(TenantId tenantId, CustomerId customerId);

    JsonNode getLoginTranslation(String localeCode);

    JsonNode getFullTranslation(TenantId tenantId, CustomerId customerId, String localeCode);

    JsonNode getTranslationForBasicEdit(TenantId tenantId, CustomerId customerId, String localeCode);

    void saveCustomTranslation(CustomTranslation customTranslation);

    void patchCustomTranslation(TenantId tenantId, CustomerId customerId, String localeCode, JsonNode customTranslation);

    void deleteCustomTranslationKey(TenantId tenantId, CustomerId customerId, String localeCode, String key);

    void deleteCustomTranslation(TenantId tenantId, CustomerId customerId, String localeCode);

    String getETag(TranslationCacheKey translationCacheKey);

    void putETag(TranslationCacheKey translationCacheKey, String etag);

    void evictETags(TenantId tenantId);

}
