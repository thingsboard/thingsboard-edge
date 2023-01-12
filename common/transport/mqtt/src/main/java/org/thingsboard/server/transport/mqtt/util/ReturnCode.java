/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2022 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.transport.mqtt.util;

public enum ReturnCode {
    SUCCESS((byte) 0x00),
    //MQTT 3 codes
    UNACCEPTABLE_PROTOCOL_VERSION((byte) 0X01),
    IDENTIFIER_REJECTED((byte) 0x02),
    SERVER_UNAVAILABLE((byte) 0x03),
    BAD_USER_NAME_OR_PASSWORD((byte) 0x04),
    NOT_AUTHORIZED((byte) 0x05),
    //MQTT 5 codes
    NO_MATCHING_SUBSCRIBERS((byte) 0x10),
    NO_SUBSCRIPTION_EXISTED((byte) 0x11),
    CONTINUE_AUTHENTICATION((byte) 0x18),
    REAUTHENTICATE((byte) 0x19),
    UNSPECIFIED_ERROR((byte) 0x80),
    MALFORMED_PACKET((byte) 0x81),
    PROTOCOL_ERROR((byte) 0x82),
    IMPLEMENTATION_SPECIFIC((byte) 0x83),
    UNSUPPORTED_PROTOCOL_VERSION((byte) 0x84),
    CLIENT_IDENTIFIER_NOT_VALID((byte) 0x85),
    BAD_USERNAME_OR_PASSWORD((byte) 0x86),
    NOT_AUTHORIZED_5((byte) 0x87),
    SERVER_UNAVAILABLE_5((byte) 0x88),
    SERVER_BUSY((byte) 0x89),
    BANNED((byte) 0x8A),
    SERVER_SHUTTING_DOWN((byte) 0x8B),
    BAD_AUTHENTICATION_METHOD((byte) 0x8C),
    KEEP_ALIVE_TIMEOUT((byte) 0x8D),
    SESSION_TAKEN_OVER((byte) 0x8E),
    TOPIC_FILTER_INVALID((byte) 0x8F),
    TOPIC_NAME_INVALID((byte) 0x90),
    PACKET_IDENTIFIER_IN_USE((byte) 0x91),
    PACKET_IDENTIFIER_NOT_FOUND((byte) 0x92),
    RECEIVE_MAXIMUM_EXCEEDED((byte) 0x93),
    TOPIC_ALIAS_INVALID((byte) 0x94),
    PACKET_TOO_LARGE((byte) 0x95),
    MESSAGE_RATE_TOO_HIGH((byte) 0x96),
    QUOTA_EXCEEDED((byte) 0x97),
    ADMINISTRATIVE_ACTION((byte) 0x98),
    PAYLOAD_FORMAT_INVALID((byte) 0x99),
    RETAIN_NOT_SUPPORTED((byte) 0x9A),
    QOS_NOT_SUPPORTED((byte) 0x9B),
    USE_ANOTHER_SERVER((byte) 0x9C),
    SERVER_MOVED((byte) 0x9D),
    SHARED_SUBSCRIPTION_NOT_SUPPORTED((byte) 0x9E),
    CONNECTION_RATE_EXCEEDED((byte) 0x9F),
    MAXIMUM_CONNECT_TIME((byte) 0xA0),
    SUBSCRIPTION_IDENTIFIERS_NOT_SUPPORTED((byte) 0xA1),
    WILDCARD_SUBSCRIPTION_NOT_SUPPORTED((byte) 0xA2);

    private static final ReturnCode[] VALUES;

    static {
        ReturnCode[] values = values();
        VALUES = new ReturnCode[163];
        for (ReturnCode code : values) {
            final int unsignedByte = code.byteValue & 0xFF;
            // Suppress a warning about out of bounds access since the enum contains only correct values
            VALUES[unsignedByte] = code;    // lgtm [java/index-out-of-bounds]
        }
    }

    private final byte byteValue;

    ReturnCode(byte byteValue) {
        this.byteValue = byteValue;
    }

    public byte byteValue() {
        return byteValue;
    }

    public short shortValue(){return byteValue;}

    public static ReturnCode valueOf(byte b) {
        final int unsignedByte = b & 0xFF;
        ReturnCode mqttConnectReturnCode = null;
        try {
            mqttConnectReturnCode = VALUES[unsignedByte];
        } catch (ArrayIndexOutOfBoundsException ignored) {
            // no op
        }
        if (mqttConnectReturnCode == null) {
            throw new IllegalArgumentException("unknown connect return code: " + unsignedByte);
        }
        return mqttConnectReturnCode;
    }
}