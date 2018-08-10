package org.thingsboard.server.common.data.localization;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Map;

@Data
@EqualsAndHashCode
public class CustomLocalization {

    private Map<String, String> localizationMap;

    public CustomLocalization merge(CustomLocalization otherCustomLocalization) {
        // TODO: implement merge
        return this;
    }
}
