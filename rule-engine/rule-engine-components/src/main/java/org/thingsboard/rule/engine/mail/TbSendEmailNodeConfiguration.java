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
package org.thingsboard.rule.engine.mail;

import lombok.Data;
import org.thingsboard.rule.engine.api.NodeConfiguration;

@Data
public class TbSendEmailNodeConfiguration implements NodeConfiguration {

    private boolean useSystemSmtpSettings;
    private String smtpHost;
    private int smtpPort;
    private String username;
    private String password;
    private String smtpProtocol;
    private int timeout;
    private boolean enableTls;
    private String tlsVersion;
    private boolean enableProxy;
    private String proxyHost;
    private String proxyPort;
    private String proxyUser;
    private String proxyPassword;

    @Override
    public TbSendEmailNodeConfiguration defaultConfiguration() {
        TbSendEmailNodeConfiguration configuration = new TbSendEmailNodeConfiguration();
        configuration.setUseSystemSmtpSettings(true);
        configuration.setSmtpHost("localhost");
        configuration.setSmtpProtocol("smtp");
        configuration.setSmtpPort(25);
        configuration.setTimeout(10000);
        configuration.setEnableTls(false);
        configuration.setTlsVersion("TLSv1.2");
        configuration.setEnableProxy(false);
        return configuration;
    }
}
