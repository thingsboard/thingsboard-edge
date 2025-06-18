/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2025 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.dao.sqlts.dictionary;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.dictionary.KeyDictionaryDao;
import org.thingsboard.server.dao.model.sqlts.dictionary.KeyDictionaryCompositeKey;
import org.thingsboard.server.dao.model.sqlts.dictionary.KeyDictionaryEntry;
import org.thingsboard.server.dao.sql.JpaAbstractDaoListeningExecutorService;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantLock;

@Component
@Slf4j
@RequiredArgsConstructor
public class JpaKeyDictionaryDao extends JpaAbstractDaoListeningExecutorService implements KeyDictionaryDao {

    private final KeyDictionaryRepository keyDictionaryRepository;

    private final ConcurrentMap<String, Integer> keyDictionaryMap = new ConcurrentHashMap<>();
    private static final ReentrantLock creationLock = new ReentrantLock();

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Override
    public Integer getOrSaveKeyId(String strKey) {
        Integer keyId = keyDictionaryMap.get(strKey);
        if (keyId == null) {
            Optional<KeyDictionaryEntry> tsKvDictionaryOptional;
            tsKvDictionaryOptional = keyDictionaryRepository.findById(new KeyDictionaryCompositeKey(strKey));
            if (tsKvDictionaryOptional.isEmpty()) {
                creationLock.lock();
                try {
                    keyId = keyDictionaryMap.get(strKey);
                    if (keyId != null) {
                        return keyId;
                    }
                    tsKvDictionaryOptional = keyDictionaryRepository.findById(new KeyDictionaryCompositeKey(strKey));
                    if (tsKvDictionaryOptional.isEmpty()) {
                        KeyDictionaryEntry keyDictionaryEntry = new KeyDictionaryEntry();
                        keyDictionaryEntry.setKey(strKey);
                        try {
                            KeyDictionaryEntry saved = keyDictionaryRepository.save(keyDictionaryEntry);
                            keyDictionaryMap.put(saved.getKey(), saved.getKeyId());
                            keyId = saved.getKeyId();
                        } catch (DataIntegrityViolationException | ConstraintViolationException e) {
                            tsKvDictionaryOptional = keyDictionaryRepository.findById(new KeyDictionaryCompositeKey(strKey));
                            KeyDictionaryEntry dictionary = tsKvDictionaryOptional.orElseThrow(() -> new RuntimeException("Failed to get KeyDictionaryEntry entity from DB!"));
                            keyDictionaryMap.put(dictionary.getKey(), dictionary.getKeyId());
                            keyId = dictionary.getKeyId();
                        }
                    } else {
                        keyId = tsKvDictionaryOptional.get().getKeyId();
                    }
                } finally {
                    creationLock.unlock();
                }
            } else {
                keyId = tsKvDictionaryOptional.get().getKeyId();
                keyDictionaryMap.put(strKey, keyId);
            }
        }
        return keyId;
    }

    @Override
    public String getKey(Integer keyId) {
        Optional<KeyDictionaryEntry> byKeyId = keyDictionaryRepository.findByKeyId(keyId);
        return byKeyId.map(KeyDictionaryEntry::getKey).orElse(null);
    }

    @Override
    public PageData<KeyDictionaryEntry> findAll(PageLink pageLink) {
        return DaoUtil.pageToPageData(keyDictionaryRepository.findAll(DaoUtil.toPageable(pageLink)));
    }

}
