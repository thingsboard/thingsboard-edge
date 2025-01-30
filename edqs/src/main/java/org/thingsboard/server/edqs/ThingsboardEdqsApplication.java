/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2024 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.edqs;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.thingsboard.server.edqs.repo.EdqRepository;

import java.util.Arrays;

@SpringBootConfiguration
@EnableAsync
@EnableScheduling
@ComponentScan({"org.thingsboard.server.edqs", "org.thingsboard.server.queue.edqs", "org.thingsboard.server.queue.discovery", "org.thingsboard.server.queue.kafka",
        "org.thingsboard.server.queue.settings", "org.thingsboard.server.queue.environment", "org.thingsboard.server.common.stats"})
@Slf4j
public class ThingsboardEdqsApplication {

    private static final String SPRING_CONFIG_NAME_KEY = "--spring.config.name";
    private static final String DEFAULT_SPRING_CONFIG_PARAM = SPRING_CONFIG_NAME_KEY + "=" + "edqs";

    public static void main(String[] args) {
        SpringApplication.run(ThingsboardEdqsApplication.class, updateArguments(args));
    }

    //    @Bean
//    public ApplicationRunner runner(CSVLoader loader, EdqRepository edqRepository) {
//        return args -> {
//            long startTs = System.currentTimeMillis();
//        var loader = new TenantRepoLoader(new TenantRepo(TenantId.fromUUID(UUID.fromString("2a209df0-c7ff-11ea-a3e0-f321b0429d60"))));
//            loader.load();
//            log.info("Loaded all in {} ms", System.currentTimeMillis() - startTs);



//            log.info("Compressed {} strings/json, Before: {}, After: {}",
//                    CompressedStringDataPoint.cnt.get(),
//                    CompressedStringDataPoint.uncompressedLength.get(),
//                    CompressedStringDataPoint.compressedLength.get());
//
//            log.info("Deduplicated {} short and {} long strings",
//                    TbStringPool.size(), TbBytePool.size());
//
//            var tenantId = TenantId.fromUUID(UUID.fromString("2a209df0-c7ff-11ea-a3e0-f321b0429d60"));
//            var customerId = new CustomerId(UUID.fromString("fcbf2f50-d0d9-11ea-bea3-177755191a6e"));
//            System.gc();
//
//            while (true) {
//                EntityTypeFilter filter = new EntityTypeFilter();
//                filter.setEntityType(EntityType.DEVICE);
//                var pageLink = new EntityDataPageLink(20, 0, null, new EntityDataSortOrder(new EntityKey(EntityKeyType.TIME_SERIES, "state"), EntityDataSortOrder.Direction.DESC), false);
//
//                var entityFields = Arrays.asList(new EntityKey(EntityKeyType.ENTITY_FIELD, "name"), new EntityKey(EntityKeyType.ENTITY_FIELD, "createdTime"));
//                var latestValues = Arrays.asList(new EntityKey(EntityKeyType.TIME_SERIES, "state"));
//                KeyFilter nameFilter = new KeyFilter();
//                nameFilter.setKey(new EntityKey(EntityKeyType.ENTITY_FIELD, "name"));
//                var predicate = new StringFilterPredicate();
//                predicate.setIgnoreCase(false);
//                predicate.setOperation(StringFilterPredicate.StringOperation.CONTAINS);
//                predicate.setValue(new FilterPredicateValue<>("LoRa-"));
//                nameFilter.setPredicate(predicate);
//                nameFilter.setValueType(EntityKeyValueType.STRING);
//
//                EntityDataQuery edq = new EntityDataQuery(filter, pageLink, entityFields, latestValues, Arrays.asList(nameFilter));
//                var result = edqRepository.findEntityDataByQuery(tenantId, customerId, RepositoryUtils.ALL_READ_PERMISSIONS, edq, false);
//                log.info("Device count: {}", result.getTotalElements());
//                log.info("First: {}", result.getData().get(0).getEntityId());
//                log.info("Last: {}", result.getData().get(19).getEntityId());
//
//                pageLink.setSortOrder(new EntityDataSortOrder(new EntityKey(EntityKeyType.TIME_SERIES, "state"), EntityDataSortOrder.Direction.ASC));
//                result = edqRepository.findEntityDataByQuery(tenantId, customerId, RepositoryUtils.ALL_READ_PERMISSIONS, edq, false);
//                log.info("Device count: {}", result.getTotalElements());
//                log.info("First: {}", result.getData().get(0).getEntityId());
//                log.info("Last: {}", result.getData().get(19).getEntityId());
//
//                result.getData().forEach(data -> {
//                    System.err.println(data.getEntityId() + ":");
//                    data.getLatest().forEach((type, values) -> {
//                        System.err.println(type);
//                        values.forEach((key, tsValue) -> {
//                            System.err.println(key + " = " + tsValue.getValue());
//                        });
//                    });
//                    System.err.println();
//                });
//                Thread.sleep(5000);
//            }
//        };
//    }

    private static String[] updateArguments(String[] args) {
        if (Arrays.stream(args).noneMatch(arg -> arg.startsWith(SPRING_CONFIG_NAME_KEY))) {
            String[] modifiedArgs = new String[args.length + 1];
            System.arraycopy(args, 0, modifiedArgs, 0, args.length);
            modifiedArgs[args.length] = DEFAULT_SPRING_CONFIG_PARAM;
            return modifiedArgs;
        }
        return args;
    }

}
