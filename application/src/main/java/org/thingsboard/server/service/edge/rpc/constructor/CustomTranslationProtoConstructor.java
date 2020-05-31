package org.thingsboard.server.service.edge.rpc.constructor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.translation.CustomTranslation;
import org.thingsboard.server.gen.edge.CustomTranslationProto;

@Component
@Slf4j
public class CustomTranslationProtoConstructor {

    public CustomTranslationProto constructCustomTranslationProto(CustomTranslation customTranslation) {
        CustomTranslationProto.Builder builder = CustomTranslationProto.newBuilder();
        builder.getTranslationMapMap().putAll(customTranslation.getTranslationMap());
        return builder.build();
    }
}
