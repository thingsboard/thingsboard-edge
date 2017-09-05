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
package org.thingsboard.server.extensions.core.filter;

import jdk.nashorn.api.scripting.NashornScriptEngineFactory;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.KvEntry;
import org.thingsboard.server.common.msg.core.UpdateAttributesRequest;
import org.thingsboard.server.extensions.api.device.DeviceAttributes;

import javax.script.*;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Andrew Shvayka
 */
@Slf4j
public class NashornJsEvaluator {

    public static final String CLIENT_SIDE = "cs";
    public static final String SERVER_SIDE = "ss";
    public static final String SHARED = "shared";
    private static NashornScriptEngineFactory factory = new NashornScriptEngineFactory();

    private CompiledScript engine;

    public NashornJsEvaluator(String script) {
        engine = compileScript(script);
    }

    private static CompiledScript compileScript(String script) {
        ScriptEngine engine = factory.getScriptEngine(new String[]{"--no-java"});
        Compilable compEngine = (Compilable) engine;
        try {
            return compEngine.compile(script);
        } catch (ScriptException e) {
            log.warn("Failed to compile filter script: {}", e.getMessage(), e);
            throw new IllegalArgumentException("Can't compile script: " + e.getMessage());
        }
    }

    public static Bindings convertListEntries(Bindings bindings, String attributesVarName, Collection<AttributeKvEntry> attributes) {
        Map<String, Object> attrMap = new HashMap<>();
        for (AttributeKvEntry attr : attributes) {
            if (!CLIENT_SIDE.equalsIgnoreCase(attr.getKey()) && !SERVER_SIDE.equalsIgnoreCase(attr.getKey())
                    && !SHARED.equalsIgnoreCase(attr.getKey())) {
                bindings.put(attr.getKey(), getValue(attr));
            }
            attrMap.put(attr.getKey(), getValue(attr));
        }
        bindings.put(attributesVarName, attrMap);
        return bindings;
    }

    public static Bindings updateBindings(Bindings bindings, UpdateAttributesRequest msg) {
        Map<String, Object> attrMap = (Map<String, Object>) bindings.get(CLIENT_SIDE);
        for (AttributeKvEntry attr : msg.getAttributes()) {
            if (!CLIENT_SIDE.equalsIgnoreCase(attr.getKey()) && !SERVER_SIDE.equalsIgnoreCase(attr.getKey())
                    && !SHARED.equalsIgnoreCase(attr.getKey())) {
                bindings.put(attr.getKey(), getValue(attr));
            }
            attrMap.put(attr.getKey(), getValue(attr));
        }
        bindings.put(CLIENT_SIDE, attrMap);
        return bindings;
    }

    protected static Object getValue(KvEntry attr) {
        switch (attr.getDataType()) {
            case STRING:
                return attr.getStrValue().get();
            case LONG:
                return attr.getLongValue().get();
            case DOUBLE:
                return attr.getDoubleValue().get();
            case BOOLEAN:
                return attr.getBooleanValue().get();
        }
        return null;
    }

    public static Bindings toBindings(List<KvEntry> entries) {
        return toBindings(new SimpleBindings(), entries);
    }

    public static Bindings toBindings(Bindings bindings, List<KvEntry> entries) {
        for (KvEntry entry : entries) {
            bindings.put(entry.getKey(), getValue(entry));
        }
        return bindings;
    }

    public static Bindings getAttributeBindings(DeviceAttributes attributes) {
        Bindings bindings = new SimpleBindings();
        convertListEntries(bindings, CLIENT_SIDE, attributes.getClientSideAttributes());
        convertListEntries(bindings, SERVER_SIDE, attributes.getServerSideAttributes());
        convertListEntries(bindings, SHARED, attributes.getServerSidePublicAttributes());
        return bindings;
    }

    public Boolean execute(Bindings bindings) throws ScriptException {
        Object eval = engine.eval(bindings);
        if (eval instanceof Boolean) {
            return (Boolean) eval;
        } else {
            log.warn("Wrong result type: {}", eval);
            throw new ScriptException("Wrong result type: " + eval);
        }
    }

    public void destroy() {
        engine = null;
    }
}
