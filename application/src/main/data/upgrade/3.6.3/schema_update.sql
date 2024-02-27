--
-- ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
--
-- Copyright © 2016-2024 ThingsBoard, Inc. All Rights Reserved.
--
-- NOTICE: All information contained herein is, and remains
-- the property of ThingsBoard, Inc. and its suppliers,
-- if any.  The intellectual and technical concepts contained
-- herein are proprietary to ThingsBoard, Inc.
-- and its suppliers and may be covered by U.S. and Foreign Patents,
-- patents in process, and are protected by trade secret or copyright law.
--
-- Dissemination of this information or reproduction of this material is strictly forbidden
-- unless prior written permission is obtained from COMPANY.
--
-- Access to the source code contained herein is hereby forbidden to anyone except current COMPANY employees,
-- managers or contractors who have executed Confidentiality and Non-disclosure agreements
-- explicitly covering such access.
--
-- The copyright notice above does not evidence any actual or intended publication
-- or disclosure  of  this source code, which includes
-- information that is confidential and/or proprietary, and is a trade secret, of  COMPANY.
-- ANY REPRODUCTION, MODIFICATION, DISTRIBUTION, PUBLIC  PERFORMANCE,
-- OR PUBLIC DISPLAY OF OR THROUGH USE  OF THIS  SOURCE CODE  WITHOUT
-- THE EXPRESS WRITTEN CONSENT OF COMPANY IS STRICTLY PROHIBITED,
-- AND IN VIOLATION OF APPLICABLE LAWS AND INTERNATIONAL TREATIES.
-- THE RECEIPT OR POSSESSION OF THIS SOURCE CODE AND/OR RELATED INFORMATION
-- DOES NOT CONVEY OR IMPLY ANY RIGHTS TO REPRODUCE, DISCLOSE OR DISTRIBUTE ITS CONTENTS,
-- OR TO MANUFACTURE, USE, OR SELL ANYTHING THAT IT  MAY DESCRIBE, IN WHOLE OR IN PART.
--

-- CUSTOM TRANSLATION MIGRATION START

CREATE TABLE IF NOT EXISTS custom_translation (
    tenant_id UUID NOT NULL,
    customer_id UUID NOT NULL default '13814000-1dd2-11b2-8080-808080808080',
    locale_code VARCHAR(10),
    value VARCHAR(1000000),
    CONSTRAINT custom_translation_pkey PRIMARY KEY (tenant_id, customer_id, locale_code));

-- move system settings

DO
$$
DECLARE
    insert_record  RECORD;
    insert_cursor CURSOR FOR SELECT json_data.key AS locale,
                                    json_data.value AS value
                             FROM admin_settings a,
                                  json_each_text(((trim('"' FROM a.json_value::json ->> 'value'))::json ->> 'translationMap')::json) AS json_data
                             WHERE  a.tenant_id = '13814000-1dd2-11b2-8080-808080808080' AND  a.key = 'customTranslation';
BEGIN
    OPEN insert_cursor;
    LOOP
        FETCH insert_cursor INTO insert_record;
        EXIT WHEN NOT FOUND;
        INSERT INTO custom_translation(tenant_id, customer_id, locale_code, value)
            VALUES ('13814000-1dd2-11b2-8080-808080808080', '13814000-1dd2-11b2-8080-808080808080', insert_record.locale, insert_record.value);
    END LOOP;
    CLOSE insert_cursor;
END;
$$;

--move tenant attributes

DO
$$
DECLARE
    insert_record  RECORD;
    insert_cursor CURSOR FOR SELECT a.entity_id AS tenant_id, json_data.key AS locale,
                                    json_data.value AS value
                             FROM attribute_kv a,
                                  json_each_text((a.str_v::json ->> 'translationMap')::json) AS json_data
                             WHERE  a.attribute_key = 'customTranslation' and a.entity_type = 'TENANT';
BEGIN
    OPEN insert_cursor;
    LOOP
        FETCH insert_cursor INTO insert_record;
        EXIT WHEN NOT FOUND;
        INSERT INTO custom_translation(tenant_id, customer_id, locale_code, value)
           VALUES (insert_record.tenant_id, '13814000-1dd2-11b2-8080-808080808080', insert_record.locale, insert_record.value);
    END LOOP;
    CLOSE insert_cursor;
END;
$$;

--move custom attributes

DO
$$
DECLARE
    tenantId uuid;
    insert_record  RECORD;
    insert_cursor CURSOR FOR SELECT a.entity_id AS customer_id, json_data.key AS locale,
                                    json_data.value AS value
                             FROM attribute_kv a,
                                 json_each_text((a.str_v::json ->> 'translationMap')::json) AS json_data
                             WHERE  a.attribute_key = 'customTranslation' and a.entity_type = 'CUSTOMER';
BEGIN
    OPEN insert_cursor;
    LOOP
        FETCH insert_cursor INTO insert_record;
        EXIT WHEN NOT FOUND;
        SELECT tenant_id INTO tenantId FROM customer where id = insert_record.customer_id;
        INSERT INTO custom_translation(tenant_id, customer_id, locale_code, value)
            VALUES (tenantId, insert_record.customer_id, insert_record.locale, insert_record.value);
    END LOOP;
    CLOSE insert_cursor;
END;
$$;

-- delete settings and attributes

DELETE FROM admin_settings WHERE key = 'customTranslation';

DELETE FROM attribute_kv WHERE entity_type = 'TENANT' AND entity_id IN (SELECT id FROM TENANT)
                           AND attribute_type = 'SERVER_SCOPE' AND  attribute_key = 'customTranslation';

DELETE FROM attribute_kv WHERE entity_type = 'CUSTOMER' AND entity_id IN (SELECT id FROM CUSTOMER)
                           AND attribute_type = 'SERVER_SCOPE' AND  attribute_key = 'customTranslation';

--CUSTOM TRANSLATION MIGRATION END
