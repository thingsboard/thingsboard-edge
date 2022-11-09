--
-- ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
--
-- Copyright © 2016-2022 ThingsBoard, Inc. All Rights Reserved.
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

CREATE TABLE IF NOT EXISTS asset_profile (
    id uuid NOT NULL CONSTRAINT asset_profile_pkey PRIMARY KEY,
    created_time bigint NOT NULL,
    name varchar(255),
    image varchar(1000000),
    description varchar,
    search_text varchar(255),
    is_default boolean,
    tenant_id uuid,
    default_rule_chain_id uuid,
    default_dashboard_id uuid,
    default_queue_name varchar(255),
    external_id uuid,
    CONSTRAINT asset_profile_name_unq_key UNIQUE (tenant_id, name),
    CONSTRAINT asset_profile_external_id_unq_key UNIQUE (tenant_id, external_id),
    CONSTRAINT fk_default_rule_chain_asset_profile FOREIGN KEY (default_rule_chain_id) REFERENCES rule_chain(id),
    CONSTRAINT fk_default_dashboard_asset_profile FOREIGN KEY (default_dashboard_id) REFERENCES dashboard(id)
    );

CREATE OR REPLACE PROCEDURE update_asset_profiles()
    LANGUAGE plpgsql AS
$$
BEGIN
    UPDATE asset a SET asset_profile_id = COALESCE(
            (SELECT id from asset_profile p WHERE p.tenant_id = a.tenant_id AND a.type = p.name),
            (SELECT id from asset_profile p WHERE p.tenant_id = a.tenant_id AND p.name = 'default')
        )
    WHERE a.asset_profile_id IS NULL;
END;
$$;
