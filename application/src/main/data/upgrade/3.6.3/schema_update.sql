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
    locale_code VARCHAR(5),
    value VARCHAR(1000000),
    CONSTRAINT custom_translation_pkey PRIMARY KEY (tenant_id, customer_id, locale_code));

-- move system settings

DO
$$
DECLARE
    insert_record  RECORD;
    insert_cursor CURSOR FOR SELECT json_data.key AS key,
                                    json_data.value AS value
                             FROM admin_settings,
                                  json_each_text(((trim('"' FROM admin_settings.json_value::json ->> 'value'))::json ->> 'translationMap')::json) AS json_data
                             WHERE  admin_settings.tenant_id = '13814000-1dd2-11b2-8080-808080808080' AND  admin_settings.key = 'customTranslation';
BEGIN
    OPEN insert_cursor;
    LOOP
        FETCH insert_cursor INTO insert_record;
        EXIT WHEN NOT FOUND;
        INSERT INTO custom_translation(tenant_id, customer_id, locale_code, value)
            VALUES ('13814000-1dd2-11b2-8080-808080808080', '13814000-1dd2-11b2-8080-808080808080', insert_record.key, insert_record.value);
    END LOOP;
    CLOSE insert_cursor;
END;
$$;

--move tenant attributes

DO
$$
DECLARE
    insert_record  RECORD;
    insert_cursor CURSOR FOR SELECT attribute_kv.entity_id AS tenant_id, json_data.key AS key,
                                    json_data.value AS value
                             FROM attribute_kv,
                                  json_each_text((attribute_kv.str_v::json ->> 'translationMap')::json) AS json_data
                             WHERE  attribute_kv.attribute_key = 'customTranslation' and attribute_kv.entity_type = 'TENANT';
BEGIN
    OPEN insert_cursor;
    LOOP
        FETCH insert_cursor INTO insert_record;
        EXIT WHEN NOT FOUND;
        INSERT INTO custom_translation(tenant_id, customer_id, locale_code, value)
           VALUES (insert_record.tenant_id, '13814000-1dd2-11b2-8080-808080808080', insert_record.key, insert_record.value);
    END LOOP;
    CLOSE insert_cursor;
END;
$$;

--move custom attributes

DO
$$
DECLARE
    tenant_id uuid;
    insert_record  RECORD;
    insert_cursor CURSOR FOR SELECT attribute_kv.entity_id AS customer_id, json_data.key AS key,
                                    json_data.value AS value
                             FROM attribute_kv,
                                 json_each_text((attribute_kv.str_v::json ->> 'translationMap')::json) AS json_data
                             WHERE  attribute_kv.attribute_key = 'customTranslation' and attribute_kv.entity_type = 'CUSTOMER';
BEGIN
    OPEN insert_cursor;
    LOOP
        FETCH insert_cursor INTO insert_record;
        EXIT WHEN NOT FOUND;
        SELECT tenant_id INTO tenant_id FROM customer where id = insert_record.customer_id;
        INSERT INTO custom_translation(tenant_id, customer_id, locale_code, value)
            VALUES (tenant_id, insert_record.customer_id, insert_record.key, insert_record.value);
    END LOOP;
    CLOSE insert_cursor;
END;
$$;

--CUSTOM TRANSLATION MIGRATION END
