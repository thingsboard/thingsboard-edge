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
package org.thingsboard.server.service.mail;

import com.fasterxml.jackson.databind.JsonNode;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.thingsboard.server.dao.exception.IncorrectParameterException;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Map;

public class MailTemplates {

    public static final String TEST = "test";
    public static final String ACTIVATION = "activation";
    public static final String ACCOUNT_ACTIVATED = "accountActivated";
    public static final String ACCOUNT_LOCKOUT = "accountLockout";
    public static final String API_USAGE_STATE_ENABLED = "apiUsageStateEnabled";
    public static final String API_USAGE_STATE_WARNING = "apiUsageStateWarning";
    public static final String API_USAGE_STATE_DISABLED = "apiUsageStateDisabled";
    public static final String RESET_PASSWORD = "resetPassword"; //NOSONAR, used as constant defining key for mail template
    public static final String PASSWORD_WAS_RESET = "passwordWasReset"; //NOSONAR, used as constant defining key for mail template
    public static final String USER_ACTIVATED = "userActivated";
    public static final String USER_REGISTERED = "userRegistered";
    public static final String TWO_FA_VERIFICATION = "twoFaVerification";

    private static final String SUBJECT = "subject";
    private static final String BODY = "body";

    public static String subject(JsonNode config, String template) {
        JsonNode templateNode = getTemplate(config, template);
        if (templateNode.has(SUBJECT)) {
            return templateNode.get(SUBJECT).asText();
        } else {
            throw new IncorrectParameterException("Template '"+template+"' doesn't have subject field.");
        }
    }

    public static String body(JsonNode config, String template, Map<String, Object> model) throws IOException, TemplateException {
        JsonNode templateNode = getTemplate(config, template);
        if (templateNode.has(BODY)) {
            String bodyTemplate = templateNode.get(BODY).asText();
            Template freeMakerTemplate = new Template(template, new StringReader(bodyTemplate),
                    new Configuration(Configuration.DEFAULT_INCOMPATIBLE_IMPROVEMENTS));
            StringWriter out = new StringWriter();
            freeMakerTemplate.process(model, out);
            return out.toString();
        } else {
            throw new IncorrectParameterException("Template '"+template+"' doesn't have body field.");
        }
    }

    private static JsonNode getTemplate(JsonNode config, String template) {
        JsonNode templateNode = config.get(template);
        if (templateNode == null) {
            throw new IncorrectParameterException("Can't find template with name '"+template+"'.");
        }
        return templateNode;
    }

}
