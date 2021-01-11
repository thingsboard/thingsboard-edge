/**
 * Copyright © 2016-2021 ThingsBoard, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.rule.engine.mail;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.rule.engine.api.*;
import org.thingsboard.server.common.data.id.BlobEntityId;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.thingsboard.rule.engine.api.TbRelationTypes.SUCCESS;
import static org.thingsboard.rule.engine.mail.TbSendEmailNode.SEND_EMAIL_TYPE;

@Slf4j
@RuleNode(
        type = ComponentType.TRANSFORMATION,
        name = "to email",
        configClazz = TbMsgToEmailNodeConfiguration.class,
        nodeDescription = "Transforms message to email message",
        nodeDetails = "Transforms message to email message by populating email fields using values derived from message metadata. " +
                      "Set 'SEND_EMAIL' output message type.",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbTransformationNodeToEmailConfig",
        icon = "email"
)
public class TbMsgToEmailNode implements TbNode {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final String ATTACHMENTS = "attachments";
    private static final String EMAIL_TIMEZONE = "emailTimezone";

    private static final Pattern dateVarPattern = Pattern.compile("%d\\{([^\\}]*)\\}");

    private TbMsgToEmailNodeConfiguration config;

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, TbMsgToEmailNodeConfiguration.class);
    }

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) {
        try {
            EmailPojo email = convert(msg);
            TbMsg emailMsg = buildEmailMsg(ctx, msg, email);
            ctx.tellNext(emailMsg, SUCCESS);
        } catch (Exception ex) {
            log.warn("Can not convert message to email " + ex.getMessage());
            ctx.tellFailure(msg, ex);
        }
    }

    private TbMsg buildEmailMsg(TbContext ctx, TbMsg msg, EmailPojo email) throws JsonProcessingException {
        String emailJson = MAPPER.writeValueAsString(email);
        return ctx.transformMsg(msg, SEND_EMAIL_TYPE, msg.getOriginator(), msg.getMetaData().copy(), emailJson);
    }

    private EmailPojo convert(TbMsg msg) throws IOException {
        TimeZone tz = null;
        String emailTimezone = msg.getMetaData().getValue(EMAIL_TIMEZONE);
        if (!StringUtils.isEmpty(emailTimezone)) {
            tz = TimeZone.getTimeZone(emailTimezone);
        }
        Date currentDate = new Date();
        EmailPojo.EmailPojoBuilder builder = EmailPojo.builder();
        builder.from(fromTemplate(this.config.getFromTemplate(), msg.getMetaData()));
        builder.to(fromTemplate(this.config.getToTemplate(), msg.getMetaData()));
        builder.cc(fromTemplate(this.config.getCcTemplate(), msg.getMetaData()));
        builder.bcc(fromTemplate(this.config.getBccTemplate(), msg.getMetaData()));
        builder.subject(fromTemplateWithDate(this.config.getSubjectTemplate(), msg.getMetaData(), currentDate, tz));
        builder.body(fromTemplateWithDate(this.config.getBodyTemplate(), msg.getMetaData(), currentDate, tz));
        List<BlobEntityId> attachments = new ArrayList<>();
        String attachmentsStr = msg.getMetaData().getValue(ATTACHMENTS);
        if (!StringUtils.isEmpty(attachmentsStr)) {
            String[] attachmentsStrArray = attachmentsStr.split(",");
            for (String attachmentStr : attachmentsStrArray) {
                attachments.add(new BlobEntityId(UUID.fromString(attachmentStr)));
            }
        }
        builder.attachments(attachments);
        return builder.build();
    }

    private String fromTemplate(String template, TbMsgMetaData metaData) {
        if (!StringUtils.isEmpty(template)) {
            return TbNodeUtils.processPattern(template, metaData);
        } else {
            return null;
        }
    }

    private String fromTemplateWithDate(String template, TbMsgMetaData metaData, Date currentDate, TimeZone tz) {
        if (!StringUtils.isEmpty(template)) {
            return processDatePatterns(TbNodeUtils.processPattern(template, metaData), currentDate, tz);
        } else {
            return null;
        }
    }

    private String processDatePatterns(String datePattern, Date currentDate, TimeZone tz) {
        String result = datePattern;
        Matcher matcher = dateVarPattern.matcher(datePattern);
        while (matcher.find()) {
            String toReplace = matcher.group(0);
            SimpleDateFormat dateFormat = new SimpleDateFormat(matcher.group(1));
            if (tz != null) {
                dateFormat.setTimeZone(tz);
            }
            String replacement = dateFormat.format(currentDate);
            result = result.replace(toReplace, replacement);
        }
        return result;
    }

    @Override
    public void destroy() {

    }
}
