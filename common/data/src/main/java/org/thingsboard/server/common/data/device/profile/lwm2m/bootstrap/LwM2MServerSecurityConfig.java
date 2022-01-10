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
package org.thingsboard.server.common.data.device.profile.lwm2m.bootstrap;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@ApiModel
@Data
public class LwM2MServerSecurityConfig {

    @ApiModelProperty(position = 1, value = "Server short Id. Used as link to associate server Object Instance. This identifier uniquely identifies each LwM2M Server configured for the LwM2M Client. " +
            "This Resource MUST be set when the Bootstrap-Server Resource has a value of 'false'. " +
            "The values ID:0 and ID:65535 values MUST NOT be used for identifying the LwM2M Server.", example = "123", readOnly = true)
    protected Integer shortServerId = 123;
    /** Security -> ObjectId = 0 'LWM2M Security' */
    @ApiModelProperty(position = 2, value = "Is Bootstrap Server or Lwm2m Server. " +
            "The LwM2M Client MAY be configured to use one or more LwM2M Server Account(s). " +
            "The LwM2M Client MUST have at most one LwM2M Bootstrap-Server Account. " +
            "(*) The LwM2M client MUST have at least one LwM2M server account after completing the boot sequence specified.", example = "true or false", readOnly = true)
    protected boolean bootstrapServerIs = false;
    @ApiModelProperty(position = 3, value = "Host for 'No Security' mode", example = "0.0.0.0", readOnly = true)
    protected String host;
    @ApiModelProperty(position = 4, value = "Port for  Lwm2m Server: 'No Security' mode: Lwm2m Server or Bootstrap Server", example = "'5685' or '5687'", readOnly = true)
    protected Integer port;
    @ApiModelProperty(position = 7, value = "Client Hold Off Time. The number of seconds to wait before initiating a Client Initiated Bootstrap once the LwM2M Client has determined it should initiate this bootstrap mode. (This information is relevant for use with a Bootstrap-Server only.)", example = "1", readOnly = true)
    protected Integer clientHoldOffTime = 1;
    @ApiModelProperty(position = 8, value = "Server Public Key for 'Security' mode (DTLS): RPK or X509. Format: base64 encoded", example = "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEAZ0pSaGKHk/GrDaUDnQZpeEdGwX7m3Ws+U/kiVat\n" +
            "+44sgk3c8g0LotfMpLlZJPhPwJ6ipXV+O1r7IZUjBs3LNA==", readOnly = true)
    protected String serverPublicKey;
    @ApiModelProperty(position = 9, value = "Bootstrap Server Account Timeout (If the value is set to 0, or if this resource is not instantiated, the Bootstrap-Server Account lifetime is infinite.)", example = "0", readOnly = true)
    Integer bootstrapServerAccountTimeout = 0;

    /** Config -> ObjectId = 1 'LwM2M Server' */
    @ApiModelProperty(position = 10, value = "Specify the lifetime of the registration in seconds.", example = "300", readOnly = true)
    private Integer lifetime = 300;
    @ApiModelProperty(position = 11, value = "The default value the LwM2M Client should use for the Minimum Period of an Observation in the absence of this parameter being included in an Observation. " +
            "If this Resource doesn’t exist, the default value is 0.", example = "1", readOnly = true)
    private Integer defaultMinPeriod = 1;
    /** ResourceID=6 'Notification Storing When Disabled or Offline' */
    @ApiModelProperty(position = 12, value = "If true, the LwM2M Client stores “Notify” operations to the LwM2M Server while the LwM2M Server account is disabled or the LwM2M Client is offline. After the LwM2M Server account is enabled or the LwM2M Client is online, the LwM2M Client reports the stored “Notify” operations to the Server. " +
            "If false, the LwM2M Client discards all the “Notify” operations or temporarily disables the Observe function while the LwM2M Server is disabled or the LwM2M Client is offline. " +
            "The default value is true.", example = "true", readOnly = true)
    private boolean notifIfDisabled = true;
    @ApiModelProperty(position = 14, value = "This Resource defines the transport binding configured for the LwM2M Client. " +
            "If the LwM2M Client supports the binding specified in this Resource, the LwM2M Client MUST use that transport for the Current Binding Mode.", example = "U", readOnly = true)
    private String binding = "U";
}
