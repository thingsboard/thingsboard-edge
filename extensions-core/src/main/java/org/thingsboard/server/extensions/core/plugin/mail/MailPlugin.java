/**
 * Thingsboard OÜ ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2017 Thingsboard OÜ. All Rights Reserved.
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
package org.thingsboard.server.extensions.core.plugin.mail;

import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.util.StringUtils;
import org.thingsboard.server.common.data.id.RuleId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.extensions.api.component.Plugin;
import org.thingsboard.server.extensions.api.plugins.AbstractPlugin;
import org.thingsboard.server.extensions.api.plugins.PluginContext;
import org.thingsboard.server.extensions.api.plugins.handlers.RuleMsgHandler;
import org.thingsboard.server.extensions.api.plugins.msg.RuleToPluginMsg;
import org.thingsboard.server.extensions.api.rules.RuleException;
import org.thingsboard.server.extensions.core.action.mail.SendMailAction;
import org.thingsboard.server.extensions.core.action.mail.SendMailActionMsg;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.util.Properties;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author Andrew Shvayka
 */
@Plugin(name = "Mail Plugin", actions = {SendMailAction.class}, descriptor = "MailPluginDescriptor.json", configuration = MailPluginConfiguration.class)
@Slf4j
public class MailPlugin extends AbstractPlugin<MailPluginConfiguration> implements RuleMsgHandler {

    //TODO: Add logic to close this executor on shutdown.
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();

    private MailPluginConfiguration configuration;
    private JavaMailSenderImpl mailSender;

    @Override
    public void init(MailPluginConfiguration configuration) {
        log.info("Initializing plugin using configuration {}", configuration);
        this.configuration = configuration;
        initMailSender(configuration);
    }

    @Override
    public void resume(PluginContext ctx) {
        initMailSender(configuration);
    }

    @Override
    public void suspend(PluginContext ctx) {
        mailSender = null;
    }

    @Override
    public void stop(PluginContext ctx) {
        mailSender = null;
    }

    private void initMailSender(MailPluginConfiguration configuration) {
        JavaMailSenderImpl mail = new JavaMailSenderImpl();
        mail.setHost(configuration.getHost());
        mail.setPort(configuration.getPort());
        mail.setUsername(configuration.getUsername());
        mail.setPassword(configuration.getPassword());
        if (configuration.getOtherProperties() != null) {
            Properties mailProperties = new Properties();
            configuration.getOtherProperties()
                    .forEach(p -> mailProperties.put(p.getKey(), p.getValue()));
            mail.setJavaMailProperties(mailProperties);
        }
        mailSender = mail;
    }

    @Override
    public void process(PluginContext ctx, TenantId tenantId, RuleId ruleId, RuleToPluginMsg<?> msg) throws RuleException {
        if (msg.getPayload() instanceof SendMailActionMsg) {
            executor.submit(() -> {
                try {
                    sendMail((SendMailActionMsg) msg.getPayload());
                } catch (Exception e) {
                    log.warn("[{}] Failed to send email", ctx.getPluginId(), e);
                    ctx.persistError("Failed to send email", e);
                }
            });
        } else {
            throw new RuntimeException("Not supported msg type: " + msg.getPayload().getClass() + "!");
        }
    }

    private void sendMail(SendMailActionMsg msg) throws MessagingException {
        log.debug("Sending mail {}", msg);
        MimeMessage mailMsg = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mailMsg, "UTF-8");
        helper.setFrom(msg.getFrom());
        helper.setTo(msg.getTo());
        if (!StringUtils.isEmpty(msg.getCc())) {
            helper.setCc(msg.getCc());
        }
        if (!StringUtils.isEmpty(msg.getBcc())) {
            helper.setBcc(msg.getBcc());
        }
        helper.setSubject(msg.getSubject());
        helper.setText(msg.getBody());
        mailSender.send(helper.getMimeMessage());
        log.debug("Mail sent {}", msg);
    }

    @Override
    protected RuleMsgHandler getRuleMsgHandler() {
        return this;
    }

}
