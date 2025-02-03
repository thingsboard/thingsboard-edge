package org.thingsboard.integration.api.converter.wrapper;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.integration.api.converter.DedicatedConverterConfig;
import org.thingsboard.integration.api.data.ContentType;
import org.thingsboard.integration.api.data.UplinkMetaData;
import org.thingsboard.server.common.data.util.TbPair;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class LoriotConverterWrapper implements ConverterWrapper {

    @Getter
    public final Set<String> keys = Set.of("cmd", "seqno", "eui", "ts", "fCnt", "fPort", "frequency", "rssi", "snr",
            "toa", "dr", "ack", "bat", "offline", "data");

    @Override
    public TbPair<byte[], UplinkMetaData> wrap(DedicatedConverterConfig config, byte[] payload, UplinkMetaData metadata) {
        JsonNode payloadJson = JacksonUtil.fromBytes(payload);
        Map<String, String> kvMap = keys.stream()
                .filter(payloadJson::has)
                .collect(Collectors.toMap(k -> k, k -> payloadJson.get(k).asText()));

        if (payloadJson.has("EUI")) {
            kvMap.put("eui", payloadJson.get("EUI").asText());
        }

        kvMap.putAll(metadata.getKvMap());
        UplinkMetaData mergedMetadata = new UplinkMetaData(ContentType.TEXT, kvMap);

        var data = payloadJson.get("data").textValue();
        return TbPair.of(data.getBytes(StandardCharsets.UTF_8), mergedMetadata);
    }

}
