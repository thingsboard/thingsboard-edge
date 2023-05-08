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
package org.thingsboard.server.service.sync.vc.repository;

import org.springframework.stereotype.Service;
import org.thingsboard.server.cache.TbTransactionalCache;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.sync.vc.RepositoryAuthMethod;
import org.thingsboard.server.common.data.sync.vc.RepositorySettings;
import org.thingsboard.server.dao.settings.AdminSettingsService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.sync.vc.TbAbstractVersionControlSettingsService;

@Service
@TbCoreComponent
public class DefaultTbRepositorySettingsService extends TbAbstractVersionControlSettingsService<RepositorySettings> implements TbRepositorySettingsService {

    public static final String SETTINGS_KEY = "entitiesVersionControl";

    public DefaultTbRepositorySettingsService(AdminSettingsService adminSettingsService, TbTransactionalCache<TenantId, RepositorySettings> cache) {
        super(adminSettingsService, cache, RepositorySettings.class, SETTINGS_KEY);
    }

    @Override
    public RepositorySettings restore(TenantId tenantId, RepositorySettings settings) {
        RepositorySettings storedSettings = get(tenantId);
        if (storedSettings != null) {
            RepositoryAuthMethod authMethod = settings.getAuthMethod();
            if (RepositoryAuthMethod.USERNAME_PASSWORD.equals(authMethod) && settings.getPassword() == null) {
                settings.setPassword(storedSettings.getPassword());
            } else if (RepositoryAuthMethod.PRIVATE_KEY.equals(authMethod) && settings.getPrivateKey() == null) {
                settings.setPrivateKey(storedSettings.getPrivateKey());
                if (settings.getPrivateKeyPassword() == null) {
                    settings.setPrivateKeyPassword(storedSettings.getPrivateKeyPassword());
                }
            }
        }
        return settings;
    }

    @Override
    public RepositorySettings get(TenantId tenantId) {
        RepositorySettings settings = super.get(tenantId);
        if (settings != null) {
            settings = new RepositorySettings(settings);
        }
        return settings;
    }

}
