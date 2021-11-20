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
package org.thingsboard.server.common.data.lwm2m;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@ApiModel
@Data
public class ServerSecurityConfig {
    @ApiModelProperty(position = 1, value = "Is Bootstrap Server", example = "true", readOnly = true)
    boolean bootstrapServerIs = true;
    @ApiModelProperty(position = 2, value = "Host for 'No Security' mode", example = "0.0.0.0", readOnly = true)
    String host;
    @ApiModelProperty(position = 3, value = "Port for 'No Security' mode", example = "5687", readOnly = true)
    Integer port;
    @ApiModelProperty(position = 4, value = "Host for 'Security' mode (DTLS)", example = "0.0.0.0", readOnly = true)
    String securityHost;
    @ApiModelProperty(position = 5, value = "Port for 'Security' mode (DTLS)", example = "5688", readOnly = true)
    Integer securityPort;
    @ApiModelProperty(position = 5, value = "Server short Id", example = "111", readOnly = true)
    Integer serverId = 111;
    @ApiModelProperty(position = 7, value = "Client Hold Off Time", example = "1", readOnly = true)
    Integer clientHoldOffTime = 1;
    @ApiModelProperty(position = 8, value = "Server Public Key (base64 encoded)", example = "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEAZ0pSaGKHk/GrDaUDnQZpeEdGwX7m3Ws+U/kiVat\n" +
            "+44sgk3c8g0LotfMpLlZJPhPwJ6ipXV+O1r7IZUjBs3LNA==", readOnly = true)
    String serverPublicKey;
    @ApiModelProperty(position = 9, value = "Bootstrap Server Account Timeout", example = "0", readOnly = true)
    Integer bootstrapServerAccountTimeout = 0;
}
