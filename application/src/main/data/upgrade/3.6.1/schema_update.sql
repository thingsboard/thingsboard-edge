--
-- ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
--
-- Copyright © 2016-2023 ThingsBoard, Inc. All Rights Reserved.
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

-- RESOURCES UPDATE START

DO
$$
    BEGIN
        IF NOT EXISTS(SELECT 1 FROM information_schema.columns WHERE table_name = 'resource' AND column_name = 'data' AND data_type = 'bytea') THEN
            ALTER TABLE resource RENAME COLUMN data TO base64_data;
            ALTER TABLE resource ADD COLUMN data bytea;
            UPDATE resource SET data = decode(base64_data, 'base64') WHERE base64_data IS NOT NULL;
            ALTER TABLE resource DROP COLUMN base64_data;
        END IF;
    END;
$$;

ALTER TABLE resource ADD COLUMN IF NOT EXISTS descriptor varchar;
ALTER TABLE resource ADD COLUMN IF NOT EXISTS preview bytea;
ALTER TABLE resource ADD COLUMN IF NOT EXISTS external_id uuid;
ALTER TABLE resource ADD COLUMN IF NOT EXISTS customer_id uuid;

CREATE INDEX IF NOT EXISTS idx_resource_etag ON resource(tenant_id, etag);

-- RESOURCES UPDATE END

-- WL UPDATE START

UPDATE white_labeling w
SET domain_name = LOWER(w.domain_name)
WHERE type = 'LOGIN'
  AND w.domain_name IS NOT NULL
  AND w.domain_name != LOWER(w.domain_name)
  AND NOT EXISTS(
        SELECT 1
        FROM white_labeling wl
        WHERE type = 'LOGIN'
          AND LOWER(wl.domain_name) = LOWER(w.domain_name)
          AND wl.entity_id != w.entity_id
);

DO
$$
    BEGIN
        IF NOT EXISTS(SELECT 1 FROM information_schema.columns WHERE table_name = 'white_labeling' AND column_name = 'tenant_id') THEN
            ALTER TABLE white_labeling ADD COLUMN tenant_id uuid not null default '13814000-1dd2-11b2-8080-808080808080';
            ALTER TABLE white_labeling ADD COLUMN customer_id uuid not null default '13814000-1dd2-11b2-8080-808080808080';

            UPDATE white_labeling SET tenant_id = entity_id WHERE entity_type = 'TENANT';
            UPDATE white_labeling
            SET tenant_id   = (select tenant_id from customer where id = entity_id),
                customer_id = entity_id
            WHERE entity_type = 'CUSTOMER';
            ALTER TABLE white_labeling DROP CONSTRAINT white_labeling_pkey;
            ALTER TABLE white_labeling DROP COLUMN entity_id;
            ALTER TABLE white_labeling DROP COLUMN entity_type;
            ALTER TABLE white_labeling ADD CONSTRAINT white_labeling_pkey PRIMARY KEY (tenant_id, customer_id, type);
        END IF;
    END;
$$;


-- WL UPDATE END