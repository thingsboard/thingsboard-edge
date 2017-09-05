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
package org.thingsboard.server.extensions.core.utils;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.runtime.RuntimeServices;
import org.apache.velocity.runtime.RuntimeSingleton;
import org.apache.velocity.runtime.parser.ParseException;
import org.apache.velocity.runtime.parser.node.SimpleNode;
import org.apache.velocity.tools.generic.DateTool;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.BasicTsKvEntry;
import org.thingsboard.server.common.msg.core.TelemetryUploadRequest;
import org.thingsboard.server.common.msg.session.FromDeviceMsg;
import org.thingsboard.server.extensions.api.device.DeviceAttributes;
import org.thingsboard.server.extensions.api.device.DeviceMetaData;
import org.thingsboard.server.extensions.api.rules.RuleProcessingMetaData;
import org.thingsboard.server.extensions.core.filter.NashornJsEvaluator;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Andrew Shvayka
 */
public class VelocityUtils {

    public static Template create(String source, String templateName) throws ParseException {
        RuntimeServices runtimeServices = RuntimeSingleton.getRuntimeServices();
        StringReader reader = new StringReader(source);
        SimpleNode node = runtimeServices.parse(reader, templateName);
        Template template = new Template();
        template.setRuntimeServices(runtimeServices);
        template.setData(node);
        template.initDocument();
        return template;
    }

    public static String merge(Template template, VelocityContext context) {
        StringWriter writer = new StringWriter();
        template.merge(context, writer);
        return writer.toString();
    }

    public static VelocityContext createContext(RuleProcessingMetaData metadata) {
        VelocityContext context = new VelocityContext();
        metadata.getValues().forEach((k, v) -> context.put(k, v));
        return context;
    }

    public static VelocityContext createContext(DeviceMetaData deviceMetaData, FromDeviceMsg payload) {
        VelocityContext context = new VelocityContext();
        context.put("date", new DateTool());
        DeviceAttributes deviceAttributes = deviceMetaData.getDeviceAttributes();

        pushAttributes(context, deviceAttributes.getClientSideAttributes(), NashornJsEvaluator.CLIENT_SIDE);
        pushAttributes(context, deviceAttributes.getServerSideAttributes(), NashornJsEvaluator.SERVER_SIDE);
        pushAttributes(context, deviceAttributes.getServerSidePublicAttributes(), NashornJsEvaluator.SHARED);

        switch (payload.getMsgType()) {
            case POST_TELEMETRY_REQUEST:
                pushTsEntries(context, (TelemetryUploadRequest) payload);
                break;
        }

        context.put("deviceId", deviceMetaData.getDeviceId().getId().toString());
        context.put("deviceName", deviceMetaData.getDeviceName());
        context.put("deviceType", deviceMetaData.getDeviceType());

        return context;
    }

    private static void pushTsEntries(VelocityContext context, TelemetryUploadRequest payload) {
        payload.getData().forEach((k, vList) -> {
            vList.forEach(v -> {
                context.put(v.getKey(), new BasicTsKvEntry(k, v));
            });
        });
    }

    private static void pushAttributes(VelocityContext context, Collection<AttributeKvEntry> deviceAttributes, String prefix) {
        Map<String, String> values = new HashMap<>();
        deviceAttributes.forEach(v -> values.put(v.getKey(), v.getValueAsString()));
        context.put(prefix, values);
    }
}
