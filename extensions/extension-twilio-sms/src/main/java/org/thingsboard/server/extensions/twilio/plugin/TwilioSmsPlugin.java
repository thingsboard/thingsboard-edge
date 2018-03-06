/**
 * Thingsboard OÜ ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2018 Thingsboard OÜ. All Rights Reserved.
 *
 * NOTICE: All information contained herein is, and remains
 * the property of Thingsboard OÜ and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Thingsboard OÜ
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
package org.thingsboard.server.extensions.twilio.plugin;

import com.twilio.Twilio;
import org.thingsboard.server.extensions.api.component.Plugin;
import org.thingsboard.server.extensions.api.plugins.AbstractPlugin;
import org.thingsboard.server.extensions.api.plugins.PluginContext;
import org.thingsboard.server.extensions.api.plugins.handlers.RuleMsgHandler;
import org.thingsboard.server.extensions.twilio.action.TwilioSmsPluginAction;

/**
 * Created by Valerii Sosliuk on 11/20/2017.
 */
@Plugin(name = "Twilio SMS Plugin", actions = {TwilioSmsPluginAction.class},
        descriptor = "TwilioSmsPluginDescriptor.json", configuration = TwilioSmsPluginConfiguration.class)
public class TwilioSmsPlugin extends AbstractPlugin<TwilioSmsPluginConfiguration> {

    private TwilioSmsMessageHandler smsMessageHandler;
    private TwilioSmsPluginConfiguration configuration;

    @Override
    public void init(TwilioSmsPluginConfiguration configuration) {
        this.configuration = configuration;
        this.smsMessageHandler = new TwilioSmsMessageHandler();
        init();
    }

    private void init() {
        Twilio.init(configuration.getAccountSid(), configuration.getAccountToken());
    }

    @Override
    protected RuleMsgHandler getRuleMsgHandler() {
        return smsMessageHandler;
    }

    @Override
    public void resume(PluginContext ctx) {
        init();
    }

    @Override
    public void suspend(PluginContext ctx) {
    }

    @Override
    public void stop(PluginContext ctx) {
    }


}
