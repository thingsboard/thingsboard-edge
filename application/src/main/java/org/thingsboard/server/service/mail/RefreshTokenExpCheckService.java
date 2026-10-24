// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.mail;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.server.queue.util.TbCoreComponent;

@TbCoreComponent
@Service
@Slf4j
@RequiredArgsConstructor
public class RefreshTokenExpCheckService {

    public static final int AZURE_DEFAULT_REFRESH_TOKEN_LIFETIME_IN_DAYS = 90;

    // Disabled on the Edge. The Edge is a thin mail client: the Cloud sends all mail and owns
    // refreshing the mail OAuth2 (Office 365) refresh token. Mail settings are no longer synced to the Edge,
    // and refreshing the token here too would race the Cloud and could invalidate the single-use refresh
    // token. Token refresh runs on the Cloud only.

}
