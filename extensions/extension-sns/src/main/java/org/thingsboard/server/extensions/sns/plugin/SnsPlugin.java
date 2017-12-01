/**
 * The Thingsboard Authors ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2017 The Thingsboard Authors. All Rights Reserved.
 *
 * NOTICE: All information contained herein is, and remains
 * the property of The Thingsboard Authors and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to The Thingsboard Authors
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
package org.thingsboard.server.extensions.sns.plugin;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.AmazonSNSClient;
import org.thingsboard.server.extensions.api.component.Plugin;
import org.thingsboard.server.extensions.api.plugins.AbstractPlugin;
import org.thingsboard.server.extensions.api.plugins.PluginContext;
import org.thingsboard.server.extensions.api.plugins.handlers.RuleMsgHandler;
import org.thingsboard.server.extensions.sns.action.SnsTopicPluginAction;

/**
 * Created by Valerii Sosliuk on 11/15/2017.
 */
@Plugin(name = "SNS Plugin", actions = {SnsTopicPluginAction.class},
        descriptor = "SnsPluginDescriptor.json", configuration = SnsPluginConfiguration.class)
public class SnsPlugin extends AbstractPlugin<SnsPluginConfiguration> {

    private SnsMessageHandler snsMessageHandler;
    private SnsPluginConfiguration configuration;

    @Override
    public void init(SnsPluginConfiguration configuration) {
        this.configuration = configuration;
        init();
    }

    private void init() {
        AWSCredentials awsCredentials = new BasicAWSCredentials(configuration.getAccessKeyId(), configuration.getSecretAccessKey());
        AWSStaticCredentialsProvider credProvider = new AWSStaticCredentialsProvider(awsCredentials);
        AmazonSNS sns = AmazonSNSClient.builder()
                .withCredentials(credProvider)
                .withRegion(configuration.getRegion())
                .build();
        this.snsMessageHandler = new SnsMessageHandler(sns);

    }

    private void destroy() {
        this.snsMessageHandler = null;
    }

    @Override
    protected RuleMsgHandler getRuleMsgHandler() {
        return snsMessageHandler;
    }

    @Override
    public void resume(PluginContext ctx) {
        init();
    }

    @Override
    public void suspend(PluginContext ctx) {
        destroy();
    }

    @Override
    public void stop(PluginContext ctx) {
        destroy();
    }
}
