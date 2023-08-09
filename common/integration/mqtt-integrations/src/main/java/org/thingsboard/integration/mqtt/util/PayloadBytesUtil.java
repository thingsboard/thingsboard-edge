package org.thingsboard.integration.mqtt.util;

import com.fasterxml.jackson.databind.JsonNode;
import org.thingsboard.common.util.JacksonUtil;

public class PayloadBytesUtil {

    public static boolean isJson(byte[] bytes) {
        try {
            JsonNode node = JacksonUtil.fromBytes(bytes);
            return node != null;
        } catch (Exception ignored) {
        }
        return false;
    }

    public static boolean isText(byte[] bytes) {
        for (byte b : bytes) {
            if ((b < 0x20 || b > 0x7E) && (b != '\t' && b != '\n' && b != '\r')) {
                return false;
            }
        }
        return true;
    }
}
