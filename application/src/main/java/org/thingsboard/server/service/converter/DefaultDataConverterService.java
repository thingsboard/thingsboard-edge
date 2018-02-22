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
package org.thingsboard.server.service.converter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.converter.Converter;
import org.thingsboard.server.common.data.id.ConverterId;
import org.thingsboard.server.dao.converter.ConverterService;
import org.thingsboard.server.service.converter.js.JSDownlinkDataConverter;
import org.thingsboard.server.service.converter.js.JSUplinkDataConverter;

import javax.annotation.PreDestroy;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Created by ashvayka on 02.12.17.
 */
@Service
public class DefaultDataConverterService implements DataConverterService {

    @Autowired
    private ConverterService converterService;

    private final ConcurrentMap<ConverterId, TBDataConverter> convertersByIdMap = new ConcurrentHashMap<>();

    @PreDestroy
    public void destroy() {
        convertersByIdMap.values().forEach(TBDataConverter::destroy);
    }

    @Override
    public TBDataConverter createConverter(Converter converter) {
        // TODO: This still may cause converter to initialize multiple times, even if one converter will be in the map. Need to improve this later.
        return convertersByIdMap.computeIfAbsent(converter.getId(), c -> initConverter(converter));
    }

    @Override
    public TBDataConverter updateConverter(Converter configuration) {
        TBDataConverter converter = convertersByIdMap.get(configuration.getId());
        if (converter != null) {
            converter.update(configuration);
            return converter;
        } else {
            return createConverter(configuration);
        }
    }

    @Override
    public void deleteConverter(ConverterId converterId) {
        TBDataConverter converter = convertersByIdMap.remove(converterId);
        if (converter != null) {
            converter.destroy();
        }
    }

    @Override
    public Optional<TBUplinkDataConverter> getUplinkConverterById(ConverterId converterId) {
        return Optional.of((TBUplinkDataConverter) getConverterById(converterId));
    }

    @Override
    public Optional<TBDownlinkDataConverter> getDownlinkConverterById(ConverterId converterId) {
        return Optional.ofNullable((TBDownlinkDataConverter) getConverterById(converterId));
    }

    private TBDataConverter getConverterById(ConverterId converterId) {
        if (converterId == null) return null;
        TBDataConverter converter = convertersByIdMap.get(converterId);
        if (converter == null) {
            Converter configuration = converterService.findConverterById(converterId);
            if (configuration != null) {
                converter = createConverter(configuration);
            }
        }
        return converter;
    }

    private TBDataConverter initConverter(Converter converter) {
        switch (converter.getType()) {
            case CUSTOM:
                JSUplinkDataConverter uplink = new JSUplinkDataConverter();
                uplink.init(converter);
                return uplink;
            case DOWNLINK:
                JSDownlinkDataConverter downlink = new JSDownlinkDataConverter();
                downlink.init(converter);
                return downlink;
            default:
                throw new RuntimeException("Not Implemented!");
        }
    }
}
