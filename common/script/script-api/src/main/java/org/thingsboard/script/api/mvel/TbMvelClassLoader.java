/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2022 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.script.api.mvel;

import org.mvel2.compiler.AbstractParser;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashSet;
import java.util.Set;

public class TbMvelClassLoader extends URLClassLoader {

    private static final Set<String> allowedClasses = new HashSet<>();
    private static final Set<String> allowedPackages = new HashSet<>();

    static {

        AbstractParser.LITERALS.remove("System");
        AbstractParser.LITERALS.remove("Runtime");
        AbstractParser.LITERALS.remove("Class");
        AbstractParser.LITERALS.remove("ClassLoader");
        AbstractParser.LITERALS.remove("Thread");
        AbstractParser.LITERALS.remove("Compiler");
        AbstractParser.LITERALS.remove("ThreadLocal");
        AbstractParser.LITERALS.remove("SecurityManager");

        AbstractParser.CLASS_LITERALS.remove("System");
        AbstractParser.CLASS_LITERALS.remove("Runtime");
        AbstractParser.CLASS_LITERALS.remove("Class");
        AbstractParser.CLASS_LITERALS.remove("ClassLoader");
        AbstractParser.CLASS_LITERALS.remove("Thread");
        AbstractParser.CLASS_LITERALS.remove("Compiler");
        AbstractParser.CLASS_LITERALS.remove("ThreadLocal");
        AbstractParser.CLASS_LITERALS.remove("SecurityManager");
        AbstractParser.CLASS_LITERALS.put("JSON", TbJson.class);
        AbstractParser.LITERALS.put("JSON", TbJson.class);

        AbstractParser.CLASS_LITERALS.values().forEach(val -> allowedClasses.add(((Class) val).getName()));
    }

    static {
        allowedPackages.add("org.mvel2");
        allowedPackages.add("java.util");
    }

    public TbMvelClassLoader() {
        super(new URL[0], Thread.currentThread().getContextClassLoader());
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        if (!classNameAllowed(name)) {
            throw new ClassNotFoundException();
        }
        return super.loadClass(name, resolve);
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        if (!classNameAllowed(name)) {
            throw new ClassNotFoundException();
        }
        return super.loadClass(name);
    }

    private boolean classNameAllowed(String name) {
        if (allowedClasses.contains(name)) {
            return true;
        }
        for (String pkgName : allowedPackages) {
            if (name.startsWith(pkgName)) {
                return true;
            }
        }
        return false;
    }

}
