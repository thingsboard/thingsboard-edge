/**
 * Copyright © 2016-2023 The Thingsboard Authors
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
package org.thingsboard.script.api.tbel;

import com.google.common.primitives.Bytes;
import org.apache.commons.lang3.ArrayUtils;
import org.mvel2.ExecutionContext;
import org.mvel2.ParserConfiguration;
import org.mvel2.execution.ExecutionArrayList;
import org.mvel2.execution.ExecutionHashMap;
import org.mvel2.util.MethodStub;
import org.thingsboard.server.common.data.StringUtils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TbUtils {

    private static final byte[] HEX_ARRAY = "0123456789ABCDEF".getBytes(StandardCharsets.US_ASCII);

    public static void register(ParserConfiguration parserConfig) throws Exception {
        parserConfig.addImport("btoa", new MethodStub(TbUtils.class.getMethod("btoa",
                String.class)));
        parserConfig.addImport("atob", new MethodStub(TbUtils.class.getMethod("atob",
                String.class)));
        parserConfig.addImport("bytesToString", new MethodStub(TbUtils.class.getMethod("bytesToString",
                List.class)));
        parserConfig.addImport("bytesToString", new MethodStub(TbUtils.class.getMethod("bytesToString",
                List.class, String.class)));
        parserConfig.addImport("decodeToString", new MethodStub(TbUtils.class.getMethod("bytesToString",
                List.class)));
        parserConfig.addImport("decodeToJson", new MethodStub(TbUtils.class.getMethod("decodeToJson",
                ExecutionContext.class, List.class)));
        parserConfig.addImport("decodeToJson", new MethodStub(TbUtils.class.getMethod("decodeToJson",
                ExecutionContext.class, String.class)));
        parserConfig.addImport("stringToBytes", new MethodStub(TbUtils.class.getMethod("stringToBytes",
                ExecutionContext.class, Object.class)));
        parserConfig.addImport("stringToBytes", new MethodStub(TbUtils.class.getMethod("stringToBytes",
                ExecutionContext.class, Object.class, String.class)));
        parserConfig.registerNonConvertableMethods(TbUtils.class, Collections.singleton("stringToBytes"));
        parserConfig.addImport("parseInt", new MethodStub(TbUtils.class.getMethod("parseInt",
                String.class)));
        parserConfig.addImport("parseInt", new MethodStub(TbUtils.class.getMethod("parseInt",
                String.class, int.class)));
        parserConfig.addImport("parseLong", new MethodStub(TbUtils.class.getMethod("parseLong",
                String.class)));
        parserConfig.addImport("parseLong", new MethodStub(TbUtils.class.getMethod("parseLong",
                String.class, int.class)));
        parserConfig.addImport("parseFloat", new MethodStub(TbUtils.class.getMethod("parseFloat",
                String.class)));
        parserConfig.addImport("parseDouble", new MethodStub(TbUtils.class.getMethod("parseDouble",
                String.class)));
        parserConfig.addImport("parseLittleEndianHexToInt", new MethodStub(TbUtils.class.getMethod("parseLittleEndianHexToInt",
                String.class)));
        parserConfig.addImport("parseBigEndianHexToInt", new MethodStub(TbUtils.class.getMethod("parseBigEndianHexToInt",
                String.class)));
        parserConfig.addImport("parseHexToInt", new MethodStub(TbUtils.class.getMethod("parseHexToInt",
                String.class)));
        parserConfig.addImport("parseHexToInt", new MethodStub(TbUtils.class.getMethod("parseHexToInt",
                String.class, boolean.class)));
        parserConfig.addImport("parseBytesToInt", new MethodStub(TbUtils.class.getMethod("parseBytesToInt",
                List.class, int.class, int.class)));
        parserConfig.addImport("parseBytesToInt", new MethodStub(TbUtils.class.getMethod("parseBytesToInt",
                List.class, int.class, int.class, boolean.class)));
        parserConfig.addImport("parseBytesToInt", new MethodStub(TbUtils.class.getMethod("parseBytesToInt",
                byte[].class, int.class, int.class)));
        parserConfig.addImport("parseBytesToInt", new MethodStub(TbUtils.class.getMethod("parseBytesToInt",
                byte[].class, int.class, int.class, boolean.class)));
        parserConfig.addImport("parseLittleEndianHexToLong", new MethodStub(TbUtils.class.getMethod("parseLittleEndianHexToLong",
                String.class)));
        parserConfig.addImport("parseBigEndianHexToLong", new MethodStub(TbUtils.class.getMethod("parseBigEndianHexToLong",
                String.class)));
        parserConfig.addImport("parseHexToLong", new MethodStub(TbUtils.class.getMethod("parseHexToLong",
                String.class)));
        parserConfig.addImport("parseHexToLong", new MethodStub(TbUtils.class.getMethod("parseHexToLong",
                String.class, boolean.class)));
        parserConfig.addImport("parseBytesToLong", new MethodStub(TbUtils.class.getMethod("parseBytesToLong",
                List.class, int.class, int.class)));
        parserConfig.addImport("parseBytesToLong", new MethodStub(TbUtils.class.getMethod("parseBytesToLong",
                List.class, int.class, int.class, boolean.class)));
        parserConfig.addImport("parseBytesToLong", new MethodStub(TbUtils.class.getMethod("parseBytesToLong",
                byte[].class, int.class, int.class)));
        parserConfig.addImport("parseBytesToLong", new MethodStub(TbUtils.class.getMethod("parseBytesToLong",
                byte[].class, int.class, int.class, boolean.class)));
        parserConfig.addImport("parseLittleEndianHexToFloat", new MethodStub(TbUtils.class.getMethod("parseLittleEndianHexToFloat",
                String.class)));
        parserConfig.addImport("parseBigEndianHexToFloat", new MethodStub(TbUtils.class.getMethod("parseBigEndianHexToFloat",
                String.class)));
        parserConfig.addImport("parseHexToFloat", new MethodStub(TbUtils.class.getMethod("parseHexToFloat",
                String.class)));
        parserConfig.addImport("parseHexToFloat", new MethodStub(TbUtils.class.getMethod("parseHexToFloat",
                String.class, boolean.class)));
        parserConfig.addImport("parseBytesToFloat", new MethodStub(TbUtils.class.getMethod("parseBytesToFloat",
                byte[].class, int.class, boolean.class)));
        parserConfig.addImport("parseBytesToFloat", new MethodStub(TbUtils.class.getMethod("parseBytesToFloat",
                byte[].class, int.class)));
        parserConfig.addImport("parseBytesToFloat", new MethodStub(TbUtils.class.getMethod("parseBytesToFloat",
                List.class, int.class, boolean.class)));
        parserConfig.addImport("parseBytesToFloat", new MethodStub(TbUtils.class.getMethod("parseBytesToFloat",
                List.class, int.class)));
        parserConfig.addImport("parseLittleEndianHexToDouble", new MethodStub(TbUtils.class.getMethod("parseLittleEndianHexToDouble",
                String.class)));
        parserConfig.addImport("parseBigEndianHexToDouble", new MethodStub(TbUtils.class.getMethod("parseBigEndianHexToDouble",
                String.class)));
        parserConfig.addImport("parseHexToDouble", new MethodStub(TbUtils.class.getMethod("parseHexToDouble",
                String.class)));
        parserConfig.addImport("parseHexToDouble", new MethodStub(TbUtils.class.getMethod("parseHexToDouble",
                String.class, boolean.class)));
        parserConfig.addImport("parseBytesToDouble", new MethodStub(TbUtils.class.getMethod("parseBytesToDouble",
                byte[].class, int.class)));
        parserConfig.addImport("parseBytesToDouble", new MethodStub(TbUtils.class.getMethod("parseBytesToDouble",
                byte[].class, int.class, boolean.class)));
        parserConfig.addImport("parseBytesToDouble", new MethodStub(TbUtils.class.getMethod("parseBytesToDouble",
                List.class, int.class)));
        parserConfig.addImport("parseBytesToDouble", new MethodStub(TbUtils.class.getMethod("parseBytesToDouble",
                List.class, int.class, boolean.class)));
        parserConfig.addImport("toFixed", new MethodStub(TbUtils.class.getMethod("toFixed",
                double.class, int.class)));
        parserConfig.addImport("toFixed", new MethodStub(TbUtils.class.getMethod("toFixed",
                float.class, int.class)));
        parserConfig.addImport("hexToBytes", new MethodStub(TbUtils.class.getMethod("hexToBytes",
                ExecutionContext.class, String.class)));
        parserConfig.addImport("base64ToHex", new MethodStub(TbUtils.class.getMethod("base64ToHex",
                String.class)));
        parserConfig.addImport("base64ToBytes", new MethodStub(TbUtils.class.getMethod("base64ToBytes",
                String.class)));
        parserConfig.addImport("bytesToBase64", new MethodStub(TbUtils.class.getMethod("bytesToBase64",
                byte[].class)));
        parserConfig.addImport("bytesToHex", new MethodStub(TbUtils.class.getMethod("bytesToHex",
                byte[].class)));
        parserConfig.addImport("bytesToHex", new MethodStub(TbUtils.class.getMethod("bytesToHex",
                ExecutionArrayList.class)));
        parserConfig.addImport("toFlatMap", new MethodStub(TbUtils.class.getMethod("toFlatMap",
                ExecutionContext.class, Map.class)));
        parserConfig.addImport("toFlatMap", new MethodStub(TbUtils.class.getMethod("toFlatMap",
                ExecutionContext.class, Map.class, boolean.class)));
        parserConfig.addImport("toFlatMap", new MethodStub(TbUtils.class.getMethod("toFlatMap",
                ExecutionContext.class, Map.class, List.class)));
        parserConfig.addImport("toFlatMap", new MethodStub(TbUtils.class.getMethod("toFlatMap",
                ExecutionContext.class, Map.class, List.class, boolean.class)));
    }

    public static String btoa(String input) {
        return new String(Base64.getEncoder().encode(input.getBytes()));
    }

    public static String atob(String encoded) {
        return new String(Base64.getDecoder().decode(encoded));
    }

    public static Object decodeToJson(ExecutionContext ctx, List<Byte> bytesList) throws IOException {
        return TbJson.parse(ctx, bytesToString(bytesList));
    }
    public static Object decodeToJson(ExecutionContext ctx, String jsonStr) throws IOException {
        return TbJson.parse(ctx, jsonStr);
    }

    public static String bytesToString(List<Byte> bytesList) {
        byte[] bytes = bytesFromList(bytesList);
        return new String(bytes);
    }

    public static String bytesToString(List<Byte> bytesList, String charsetName) throws UnsupportedEncodingException {
        byte[] bytes = bytesFromList(bytesList);
        return new String(bytes, charsetName);
    }

    public static List<Byte> stringToBytes(ExecutionContext ctx, Object str) throws IllegalAccessException {
        if (str instanceof String) {
            byte[] bytes = str.toString().getBytes();
            return bytesToList(ctx, bytes);
        } else {
            throw new IllegalAccessException("Invalid type parameter [" + str.getClass().getSimpleName() + "]. Expected 'String'");
        }
    }

    public static List<Byte> stringToBytes(ExecutionContext ctx, Object str, String charsetName) throws UnsupportedEncodingException, IllegalAccessException {
        if (str instanceof String) {
            byte[] bytes = str.toString().getBytes(charsetName);
            return bytesToList(ctx, bytes);
        } else {
            throw new IllegalAccessException("Invalid type parameter [" + str.getClass().getSimpleName() + "]. Expected 'String'");
        }
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
        for (byte aByte : bytes) {
            list.add(aByte);
        }
        return list;
    }

    public static Integer parseInt(String value) {
        int radix = getRadix(value);
        return parseInt(value, radix);
    }

    public static Integer parseInt(String value, int radix) {
        if (StringUtils.isNotBlank(value)) {
            try {
                String valueP = prepareNumberString(value);
                isValidRadix(valueP, radix);
                try {
                    return Integer.parseInt(valueP, radix);
                } catch (NumberFormatException e) {
                    BigInteger bi = new BigInteger(valueP, radix);
                    if (bi.compareTo(BigInteger.valueOf(Integer.MAX_VALUE)) > 0)
                        throw new NumberFormatException("Value \"" + value + "\" is greater than the maximum Integer value " + Integer.MAX_VALUE + " !");
                    if (bi.compareTo(BigInteger.valueOf(Integer.MIN_VALUE)) < 0)
                        throw new NumberFormatException("Value \"" + value + "\" is less than the minimum Integer value " + Integer.MIN_VALUE + " !");
                    Float f = parseFloat(valueP);
                    if (f != null) {
                        return f.intValue();
                    } else {
                        throw new NumberFormatException(e.getMessage());
                    }
                }
            } catch (NumberFormatException e) {
                throw new NumberFormatException(e.getMessage());
            }
        }
        return null;
    }

    public static Long parseLong(String value) {
        int radix = getRadix(value);
        return parseLong(value, radix);
    }

    public static Long parseLong(String value, int radix) {
        if (StringUtils.isNotBlank(value)) {
            try {
                String valueP = prepareNumberString(value);
                isValidRadix(valueP, radix);
                try {
                    return Long.parseLong(valueP, radix);
                } catch (NumberFormatException e) {
                    BigInteger bi = new BigInteger(valueP, radix);
                    if (bi.compareTo(BigInteger.valueOf(Long.MAX_VALUE)) > 0)
                        throw new NumberFormatException("Value \"" + value + "\"is greater than the maximum Long value " + Long.MAX_VALUE + " !");
                    if (bi.compareTo(BigInteger.valueOf(Long.MIN_VALUE)) < 0)
                        throw new NumberFormatException("Value \"" + value + "\" is  less than the minimum Long value " + Long.MIN_VALUE + " !");
                    Double dd = parseDouble(valueP);
                    if (dd != null) {
                        return dd.longValue();
                    } else {
                        throw new NumberFormatException(e.getMessage());
                    }
                }
            } catch (NumberFormatException e) {
                throw new NumberFormatException(e.getMessage());
            }
        }
        return null;
    }

    private static int getRadix(String value, int... radixS) {
        return radixS.length > 0 ? radixS[0] : isHexadecimal(value) ? 16 : 10;
    }

    public static Float parseFloat(String value) {
        if (value != null) {
            try {
                return Float.parseFloat(prepareNumberString(value));
            } catch (NumberFormatException e) {
            }
        }
        return null;
    }

    public static Double parseDouble(String value) {
        if (value != null) {
            try {
                return Double.parseDouble(prepareNumberString(value));
            } catch (NumberFormatException e) {
            }
        }
        return null;
    }

    public static int parseLittleEndianHexToInt(String hex) {
        return parseHexToInt(hex, false);
    }

    public static int parseBigEndianHexToInt(String hex) {
        return parseHexToInt(hex, true);
    }

    public static int parseHexToInt(String hex) {
        return parseHexToInt(hex, true);
    }

    public static int parseHexToInt(String hex, boolean bigEndian) {
        byte[] data = prepareHexToBytesNumber(hex, 8);
        return parseBytesToInt(data, 0, data.length, bigEndian);
    }

    public static long parseLittleEndianHexToLong(String hex) {
        return parseHexToLong(hex, false);
    }

    public static long parseBigEndianHexToLong(String hex) {
        return parseHexToLong(hex, true);
    }

    public static long parseHexToLong(String hex) {
        return parseHexToLong(hex, true);
    }

    public static long parseHexToLong(String hex, boolean bigEndian) {
        byte[] data = prepareHexToBytesNumber(hex, 16);
        return parseBytesToLong(data, 0, data.length, bigEndian);
    }

    public static float parseLittleEndianHexToFloat(String hex) {
        return parseHexToFloat(hex, false);
    }

    public static float parseBigEndianHexToFloat(String hex) {
        return parseHexToFloat(hex, true);
    }

    public static float parseHexToFloat(String hex) {
        return parseHexToFloat(hex, true);
    }

    public static float parseHexToFloat(String hex, boolean bigEndian) {
        byte[] data = prepareHexToBytesNumber(hex, 8);
        return parseBytesToFloat(data, 0, bigEndian);
    }

    public static double parseLittleEndianHexToDouble(String hex) {
        return parseHexToDouble(hex, false);
    }

    public static double parseBigEndianHexToDouble(String hex) {
        return parseHexToDouble(hex, true);
    }

    public static double parseHexToDouble(String hex) {
        return parseHexToDouble(hex, true);
    }

    public static double parseHexToDouble(String hex, boolean bigEndian) {
        byte[] data = prepareHexToBytesNumber(hex, 16);
        return parseBytesToDouble(data, 0, bigEndian);
    }

    private static byte[] prepareHexToBytesNumber(String hex, int len) {
        int length = hex.length();
        if (length > len) {
            throw new IllegalArgumentException("Hex string is too large. Maximum 8 symbols allowed.");
        }
        if (length % 2 > 0) {
            throw new IllegalArgumentException("Hex string must be even-length.");
        }
        byte[] data = new byte[length / 2];
        for (int i = 0; i < length; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4) + Character.digit(hex.charAt(i + 1), 16));
        }
        return data;
    }

    public static ExecutionArrayList<Byte> hexToBytes(ExecutionContext ctx, String hex) {
        int len = hex.length();
        if (len % 2 > 0) {
            throw new IllegalArgumentException("Hex string must be even-length.");
        }
        ExecutionArrayList<Byte> data = new ExecutionArrayList<>(ctx);
        for (int i = 0; i < len; i += 2) {
            data.add((byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i + 1), 16)));
        }
        return data;
    }

    public static String base64ToHex(String base64) {
        return bytesToHex(Base64.getDecoder().decode(base64));
    }

    public static String bytesToBase64(byte[] bytes) {
        return Base64.getEncoder().encodeToString(bytes);
    }

    public static byte[] base64ToBytes(String input) {
        return Base64.getDecoder().decode(input);
    }

    public static int parseBytesToInt(List<Byte> data, int offset, int length) {
        return parseBytesToInt(data, offset, length, true);
    }

    public static int parseBytesToInt(List<Byte> data, int offset, int length, boolean bigEndian) {
        final byte[] bytes = new byte[data.size()];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = data.get(i);
        }
        return parseBytesToInt(bytes, offset, length, bigEndian);
    }

    public static int parseBytesToInt(byte[] data, int offset, int length) {
        return parseBytesToInt(data, offset, length, true);
    }

    public static int parseBytesToInt(byte[] data, int offset, int length, boolean bigEndian) {
        if (offset > data.length) {
            throw new IllegalArgumentException("Offset: " + offset + " is out of bounds for array with length: " + data.length + "!");
        }
        if (length > 4) {
            throw new IllegalArgumentException("Length: " + length + " is too large. Maximum 4 bytes is allowed!");
        }
        if (offset + length > data.length) {
            throw new IllegalArgumentException("Offset: " + offset + " and Length: " + length + " is out of bounds for array with length: " + data.length + "!");
        }
        var bb = ByteBuffer.allocate(4);
        if (!bigEndian) {
            bb.order(ByteOrder.LITTLE_ENDIAN);
        }
        bb.position(bigEndian ? 4 - length : 0);
        bb.put(data, offset, length);
        bb.position(0);
        return bb.getInt();
    }

    public static long parseBytesToLong(List<Byte> data, int offset, int length) {
        return parseBytesToLong(data, offset, length, true);
    }

    public static long parseBytesToLong(List<Byte> data, int offset, int length, boolean bigEndian) {
        final byte[] bytes = new byte[data.size()];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = data.get(i);
        }
        return parseBytesToLong(bytes, offset, length, bigEndian);
    }

    public static long parseBytesToLong(byte[] data, int offset, int length) {
        return parseBytesToLong(data, offset, length, true);
    }

    public static long parseBytesToLong(byte[] data, int offset, int length, boolean bigEndian) {
        if (offset > data.length) {
            throw new IllegalArgumentException("Offset: " + offset + " is out of bounds for array with length: " + data.length + "!");
        }
        if (length > 8) {
            throw new IllegalArgumentException("Length: " + length + " is too large. Maximum 4 bytes is allowed!");
        }
        if (offset + length > data.length) {
            throw new IllegalArgumentException("Offset: " + offset + " and Length: " + length + " is out of bounds for array with length: " + data.length + "!");
        }
        var bb = ByteBuffer.allocate(8);
        if (!bigEndian) {
            bb.order(ByteOrder.LITTLE_ENDIAN);
        }
        bb.position(bigEndian ? 8 - length : 0);
        bb.put(data, offset, length);
        bb.position(0);
        return bb.getLong();
    }

    public static float parseBytesToFloat(byte[] data, int offset) {
        return parseBytesToFloat(data, offset, true);
    }

    public static float parseBytesToFloat(List data, int offset) {
        return parseBytesToFloat(data, offset, true);
    }

    public static float parseBytesToFloat(List data, int offset, boolean bigEndian) {
        return parseBytesToFloat(Bytes.toArray(data), offset, bigEndian);
    }

    public static float parseBytesToFloat(byte[] data, int offset, boolean bigEndian) {
        byte[] bytesToNumber = prepareBytesToNumber(data, offset, 4, bigEndian);
        return ByteBuffer.wrap(bytesToNumber).getFloat();
    }


    public static double parseBytesToDouble(byte[] data, int offset) {
        return parseBytesToDouble(data, offset, true);
    }

    public static double parseBytesToDouble(List data, int offset) {
        return parseBytesToDouble(data, offset, true);
    }

    public static double parseBytesToDouble(List data, int offset, boolean bigEndian) {
        return parseBytesToDouble(Bytes.toArray(data), offset, bigEndian);
    }

    public static double parseBytesToDouble(byte[] data, int offset, boolean bigEndian) {
        byte[] bytesToNumber = prepareBytesToNumber(data, offset, 8, bigEndian);
        return ByteBuffer.wrap(bytesToNumber).getDouble();
    }

    private static byte[] prepareBytesToNumber(byte[] data, int offset, int length, boolean bigEndian) {
        if (offset > data.length) {
            throw new IllegalArgumentException("Offset: " + offset + " is out of bounds for array with length: " + data.length + "!");
        }
        if ((offset + length) > data.length) {
            throw new IllegalArgumentException("Default length is always " + length + " bytes. Offset: " + offset + " and Length: " + length + " is out of bounds for array with length: " + data.length + "!");
        }
        byte[] dataBytesArray = Arrays.copyOfRange(data, offset, (offset + length));
        if (!bigEndian) {
            ArrayUtils.reverse(dataBytesArray);
        }
        return dataBytesArray;
    }

    public static String bytesToHex(ExecutionArrayList<?> bytesList) {
        byte[] bytes = new byte[bytesList.size()];
        for (int i = 0; i < bytesList.size(); i++) {
            bytes[i] = Byte.parseByte(bytesList.get(i).toString());
        }
        return bytesToHex(bytes);
    }

    public static String bytesToHex(byte[] bytes) {
        byte[] hexChars = new byte[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars, StandardCharsets.UTF_8);
    }

    public static double toFixed(double value, int precision) {
        return BigDecimal.valueOf(value).setScale(precision, RoundingMode.HALF_UP).doubleValue();
    }

    public static float toFixed(float value, int precision) {
        return BigDecimal.valueOf(value).setScale(precision, RoundingMode.HALF_UP).floatValue();
    }

    private static boolean isHexadecimal(String value) {
        return value != null && (value.contains("0x") || value.contains("0X"));
    }

    private static String prepareNumberString(String value) {
        if (value != null) {
            value = value.trim();
            if (isHexadecimal(value)) {
                value = value.replace("0x", "");
                value = value.replace("0X", "");
            }
            value = value.replace(",", ".");
        }
        return value;
    }

    public static ExecutionHashMap<String, Object> toFlatMap(ExecutionContext ctx, Map<String, Object> json) {
        return toFlatMap(ctx, json, new ArrayList<>(), true);
    }

    public static ExecutionHashMap<String, Object> toFlatMap(ExecutionContext ctx, Map<String, Object> json, boolean pathInKey) {
        return toFlatMap(ctx, json, new ArrayList<>(), pathInKey);
    }

    public static ExecutionHashMap<String, Object> toFlatMap(ExecutionContext ctx, Map<String, Object> json, List<String> excludeList) {
        return toFlatMap(ctx, json, excludeList, true);
    }

    public static ExecutionHashMap<String, Object> toFlatMap(ExecutionContext ctx, Map<String, Object> json, List<String> excludeList, boolean pathInKey) {
        ExecutionHashMap<String, Object> map = new ExecutionHashMap<>(16, ctx);
        parseRecursive(json, map, excludeList, "", pathInKey);
        return map;
    }

    private static void parseRecursive(Object json, Map<String, Object> map, List<String> excludeList, String path, boolean pathInKey) {
        if (json instanceof Map.Entry) {
            Map.Entry<?, ?> entry = (Map.Entry<?, ?>) json;
            if (StringUtils.isNotBlank(path)) {
                path += ".";
            }
            if (excludeList.contains(entry.getKey())) {
                return;
            }
            path += entry.getKey();
            json = entry.getValue();
        }
        if (json instanceof Set || json instanceof List) {
            String arrayPath = path + ".";
            Object[] collection = ((Collection<?>) json).toArray();
            for (int index = 0; index < collection.length; index++) {
                parseRecursive(collection[index], map, excludeList, arrayPath + index, pathInKey);
            }
        } else if (json instanceof Map) {
            Map<?, ?> node = (Map<?, ?>) json;
            for (Map.Entry<?, ?> entry : node.entrySet()) {
                parseRecursive(entry, map, excludeList, path, pathInKey);
            }
        } else {
            if (pathInKey) {
                map.put(path, json);
            } else {
                String key = path.substring(path.lastIndexOf('.') + 1);
                if (StringUtils.isNumeric(key)) {
                    int pos = path.length();
                    for (int i = 0; i < 2; i++) {
                        pos = path.lastIndexOf('.', pos - 1);
                    }
                    key = path.substring(pos + 1);
                }
                map.put(key, json);
            }
        }
    }

    public static boolean isValidRadix(String value, int radix) {
        for (int i = 0; i < value.length(); i++) {
            if (i == 0 && value.charAt(i) == '-') {
                if (value.length() == 1)
                    throw new NumberFormatException("Failed radix [" + radix + "] for value: \"" + value + "\"!");
                else
                    continue;
            }
            if (Character.digit(value.charAt(i), radix) < 0)
                throw new NumberFormatException("Failed radix: [" + radix + "] for value: \"" + value + "\"!");
        }
        return true;
    }

}
