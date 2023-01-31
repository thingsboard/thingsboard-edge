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

import org.springframework.stereotype.Component;
import org.thingsboard.rule.engine.api.sms.SmsSender;
import org.thingsboard.rule.engine.api.sms.SmsSenderFactory;
import org.thingsboard.server.common.data.sms.config.AwsSnsSmsProviderConfiguration;
import org.thingsboard.server.common.data.sms.config.SmppSmsProviderConfiguration;
import org.thingsboard.server.common.data.sms.config.SmsProviderConfiguration;
import org.thingsboard.server.common.data.sms.config.TwilioSmsProviderConfiguration;
import org.thingsboard.server.service.sms.aws.AwsSmsSender;
import org.thingsboard.server.service.sms.smpp.SmppSmsSender;
import org.thingsboard.server.service.sms.twilio.TwilioSmsSender;

@Component
public class DefaultSmsSenderFactory implements SmsSenderFactory {

    @Override
    public SmsSender createSmsSender(SmsProviderConfiguration config) {
        switch (config.getType()) {
            case AWS_SNS:
                return new AwsSmsSender((AwsSnsSmsProviderConfiguration)config);
            case TWILIO:
                return new TwilioSmsSender((TwilioSmsProviderConfiguration)config);
            case SMPP:
                return new SmppSmsSender((SmppSmsProviderConfiguration) config);
            default:
                throw new RuntimeException("Unknown SMS provider type " + config.getType());
        }
    }

}
