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
package org.thingsboard.server.common.data.secret;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.SecretType;
import org.thingsboard.server.common.data.id.SecretId;

import java.io.Serial;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;

@Schema
@Data
@EqualsAndHashCode(callSuper = true)
public class Secret extends SecretInfo {

    @Serial
    private static final long serialVersionUID = 3671364019778017637L;

    @JsonIgnore
    private byte[] value;

    @Schema(description = "Secret text value for TEXT secret type.", requiredMode = Schema.RequiredMode.REQUIRED, example = "Value")
    @JsonSetter("text")
    public void setText(String value) {
        this.value = Optional.ofNullable(value).map(v -> Base64.getEncoder().encode(v.getBytes(StandardCharsets.UTF_8))).orElse(null);
    }

    @JsonGetter("simpleValue")
    public String getText() {
        if (SecretType.FILE.equals(getType())) {
            return Optional.ofNullable(value).map(Base64.getEncoder()::encodeToString).orElse(null);
        } else {
            return Optional.ofNullable(value).map(v -> new String(v, StandardCharsets.UTF_8)).orElse(null);
        }
    }

    public Secret() {
        super();
    }

    public Secret(SecretId id) {
        super(id);
    }

    public Secret(Secret secret) {
        super(secret);
        this.value = secret.getValue();
    }

    public Secret(SecretInfo secretInfo) {
        super(secretInfo);
        this.value = null;
    }

    public Secret(SecretInfo secretInfo, byte[] rawValue) {
        super(secretInfo);
        this.value = rawValue;
    }

    @Override
    public String toString() {
        return super.toString();
    }

}
