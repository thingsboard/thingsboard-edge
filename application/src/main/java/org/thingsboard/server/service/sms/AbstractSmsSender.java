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
package org.thingsboard.server.service.sms;

import lombok.extern.slf4j.Slf4j;
import org.thingsboard.rule.engine.api.sms.SmsSender;
import org.thingsboard.rule.engine.api.sms.exception.SmsParseException;

import java.util.regex.Pattern;

@Slf4j
public abstract class AbstractSmsSender implements SmsSender {

    protected static final Pattern E_164_PHONE_NUMBER_PATTERN = Pattern.compile("^\\+[1-9]\\d{1,14}$");

    private static final int MAX_SMS_MESSAGE_LENGTH = 1600;
    private static final int MAX_SMS_SEGMENT_LENGTH = 70;

    protected String validatePhoneNumber(String phoneNumber) throws SmsParseException {
        phoneNumber = phoneNumber.trim();
        if (!E_164_PHONE_NUMBER_PATTERN.matcher(phoneNumber).matches()) {
            throw new SmsParseException("Invalid phone number format. Phone number must be in E.164 format.");
        }
        return phoneNumber;
    }

    protected String prepareMessage(String message) {
        message = message.replaceAll("^\"|\"$", "").replaceAll("\\\\n", "\n");
        if (message.length() > MAX_SMS_MESSAGE_LENGTH) {
            log.warn("SMS message exceeds maximum symbols length and will be truncated");
            message = message.substring(0, MAX_SMS_MESSAGE_LENGTH);
        }
        return message;
    }

    protected int countMessageSegments(String message) {
        return (int)Math.ceil((double) message.length() / (double) MAX_SMS_SEGMENT_LENGTH);
    }

}
