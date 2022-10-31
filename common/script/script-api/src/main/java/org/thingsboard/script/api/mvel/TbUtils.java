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

import org.mvel2.ExecutionContext;
import org.mvel2.ParserConfiguration;
import org.mvel2.execution.ExecutionArrayList;
import org.mvel2.util.MethodStub;

import java.io.UnsupportedEncodingException;
import java.util.Base64;
import java.util.List;

public class TbUtils {

    public static void register(ParserConfiguration parserConfig) throws Exception {
        parserConfig.addImport("btoa", new MethodStub(TbUtils.class.getMethod("btoa",
                String.class)));
        parserConfig.addImport("atob", new MethodStub(TbUtils.class.getMethod("atob",
                String.class)));
        parserConfig.addImport("bytesToString", new MethodStub(TbUtils.class.getMethod("bytesToString",
                List.class)));
        parserConfig.addImport("bytesToString", new MethodStub(TbUtils.class.getMethod("bytesToString",
                List.class, String.class)));
        parserConfig.addImport("stringToBytes", new MethodStub(TbUtils.class.getMethod("stringToBytes",
                ExecutionContext.class, String.class)));
        parserConfig.addImport("stringToBytes", new MethodStub(TbUtils.class.getMethod("stringToBytes",
                ExecutionContext.class, String.class, String.class)));
    }

    public static String btoa(String input) {
        return new String(Base64.getEncoder().encode(input.getBytes()));
    }

    public static String atob(String encoded) {
        return new String(Base64.getDecoder().decode(encoded));
    }

    public static String bytesToString(List<Byte> bytesList) {
        byte[] bytes = bytesFromList(bytesList);
        return new String(bytes);
    }

    public static String bytesToString(List<Byte> bytesList, String charsetName) throws UnsupportedEncodingException {
        byte[] bytes = bytesFromList(bytesList);
        return new String(bytes, charsetName);
    }

    public static List<Byte> stringToBytes(ExecutionContext ctx, String str) {
        byte[] bytes = str.getBytes();
        return bytesToList(ctx, bytes);
    }

    public static List<Byte> stringToBytes(ExecutionContext ctx, String str, String charsetName) throws UnsupportedEncodingException {
        byte[] bytes = str.getBytes(charsetName);
        return bytesToList(ctx, bytes);
    }

    private static byte[] bytesFromList(List<Byte> bytesList) {
        byte[] bytes = new byte[bytesList.size()];
        for (int i = 0; i < bytesList.size(); i++) {
            bytes[i] = bytesList.get(i);
        }
        return bytes;
    }

    private static List<Byte> bytesToList(ExecutionContext ctx, byte[] bytes) {
        List<Byte> list = new ExecutionArrayList<>(ctx);
        for (int i = 0; i < bytes.length; i++) {
            list.add(bytes[i]);
        }
        return list;
    }
}
