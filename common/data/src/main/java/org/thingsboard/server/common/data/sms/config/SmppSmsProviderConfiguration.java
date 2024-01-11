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
package org.thingsboard.server.common.data.sms.config;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class SmppSmsProviderConfiguration implements SmsProviderConfiguration {
    @Schema(description = "SMPP version", allowableValues = "3.3, 3.4", required = true)
    private String protocolVersion;

    @Schema(description = "SMPP host", required = true)
    private String host;
    @Schema(description = "SMPP port", required = true)
    private Integer port;

    @Schema(description = "System ID", required = true)
    private String systemId;
    @Schema(description = "Password", required = true)
    private String password;

    @Schema(description = "System type", required = false)
    private String systemType;
    @Schema(description = "TX - Transmitter, RX - Receiver, TRX - Transciever. By default TX is used", required = false)
    private SmppBindType bindType;
    @Schema(description = "Service type", required = false)
    private String serviceType;

    @Schema(description = "Source address", required = false)
    private String sourceAddress;
    @Schema(description = "Source TON (Type of Number). Needed is source address is set. 5 by default.\n" +
            "0 - Unknown\n" +
            "1 - International\n" +
            "2 - National\n" +
            "3 - Network Specific\n" +
            "4 - Subscriber Number\n" +
            "5 - Alphanumeric\n" +
            "6 - Abbreviated", required = false)
    private Byte sourceTon;
    @Schema(description = "Source NPI (Numbering Plan Identification). Needed is source address is set. 0 by default.\n" +
            "0 - Unknown\n" +
            "1 - ISDN/telephone numbering plan (E163/E164)\n" +
            "3 - Data numbering plan (X.121)\n" +
            "4 - Telex numbering plan (F.69)\n" +
            "6 - Land Mobile (E.212) =6\n" +
            "8 - National numbering plan\n" +
            "9 - Private numbering plan\n" +
            "10 - ERMES numbering plan (ETSI DE/PS 3 01-3)\n" +
            "13 - Internet (IP)\n" +
            "18 - WAP Client Id (to be defined by WAP Forum)", required = false)
    private Byte sourceNpi;

    @Schema(description = "Destination TON (Type of Number). 5 by default.\n" +
            "0 - Unknown\n" +
            "1 - International\n" +
            "2 - National\n" +
            "3 - Network Specific\n" +
            "4 - Subscriber Number\n" +
            "5 - Alphanumeric\n" +
            "6 - Abbreviated", required = false)
    private Byte destinationTon;
    @Schema(description = "Destination NPI (Numbering Plan Identification). 0 by default.\n" +
            "0 - Unknown\n" +
            "1 - ISDN/telephone numbering plan (E163/E164)\n" +
            "3 - Data numbering plan (X.121)\n" +
            "4 - Telex numbering plan (F.69)\n" +
            "6 - Land Mobile (E.212) =6\n" +
            "8 - National numbering plan\n" +
            "9 - Private numbering plan\n" +
            "10 - ERMES numbering plan (ETSI DE/PS 3 01-3)\n" +
            "13 - Internet (IP)\n" +
            "18 - WAP Client Id (to be defined by WAP Forum)", required = false)
    private Byte destinationNpi;

    @Schema(description = "Address range", required = false)
    private String addressRange;

    @Schema(allowableValues = "0-10,13-14",
            description = "0 - SMSC Default Alphabet (ASCII for short and long code and to GSM for toll-free, used as default)\n" +
                    "1 - IA5 (ASCII for short and long code, Latin 9 for toll-free (ISO-8859-9))\n" +
                    "2 - Octet Unspecified (8-bit binary)\n" +
                    "3 - Latin 1 (ISO-8859-1)\n" +
                    "4 - Octet Unspecified (8-bit binary)\n" +
                    "5 - JIS (X 0208-1990)\n" +
                    "6 - Cyrillic (ISO-8859-5)\n" +
                    "7 - Latin/Hebrew (ISO-8859-8)\n" +
                    "8 - UCS2/UTF-16 (ISO/IEC-10646)\n" +
                    "9 - Pictogram Encoding\n" +
                    "10 - Music Codes (ISO-2022-JP)\n" +
                    "13 - Extended Kanji JIS (X 0212-1990)\n" +
                    "14 - Korean Graphic Character Set (KS C 5601/KS X 1001)", required = false)
    private Byte codingScheme;

    @Override
    public SmsProviderType getType() {
        return SmsProviderType.SMPP;
    }

    public enum SmppBindType {
        TX, RX, TRX
    }

}
