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
package org.thingsboard.server.common.transport.config.ssl;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.PostConstruct;

@Slf4j
@Data
public class SslCredentialsConfig {

    private boolean enabled = true;
    private SslCredentialsType type;
    private PemSslCredentials pem;
    private KeystoreSslCredentials keystore;

    private SslCredentials credentials;

    private final String name;
    private final boolean trustsOnly;

    public SslCredentialsConfig(String name, boolean trustsOnly) {
        this.name = name;
        this.trustsOnly = trustsOnly;
    }

    @PostConstruct
    public void init() {
        if (this.enabled) {
            log.info("{}: Initializing SSL credentials.", name);
            if (SslCredentialsType.PEM.equals(type) && pem.canUse()) {
                this.credentials = this.pem;
            } else if (keystore.canUse()) {
                if (SslCredentialsType.PEM.equals(type)) {
                    log.warn("{}: Specified PEM configuration is not valid. Using SSL keystore configuration as fallback.", name);
                }
                this.credentials = this.keystore;
            } else {
                throw new RuntimeException(name + ": Invalid SSL credentials configuration. None of the PEM or KEYSTORE configurations can be used!");
            }
            try {
                this.credentials.init(this.trustsOnly);
            } catch (Exception e) {
                throw new RuntimeException(name + ": Failed to init SSL credentials configuration.", e);
            }
        } else {
            log.info("{}: Skipping initialization of disabled SSL credentials.", name);
        }
    }

}
