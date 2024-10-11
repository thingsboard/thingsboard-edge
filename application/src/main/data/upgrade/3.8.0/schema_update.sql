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

-- CREATE MOBILE APP BUNDLES FROM EXISTING APPS

CREATE TABLE IF NOT EXISTS mobile_app_bundle (
    id uuid NOT NULL CONSTRAINT mobile_app_bundle_pkey PRIMARY KEY,
    created_time bigint NOT NULL,
    tenant_id uuid,
    title varchar(255),
    description varchar(1024),
    android_app_id uuid UNIQUE,
    ios_app_id uuid UNIQUE,
    layout_config varchar(16384),
    self_registration_config varchar(16384),
    oauth2_enabled boolean,
    CONSTRAINT fk_android_app_id FOREIGN KEY (android_app_id) REFERENCES mobile_app(id),
    CONSTRAINT fk_ios_app_id FOREIGN KEY (ios_app_id) REFERENCES mobile_app(id)
);

ALTER TABLE mobile_app ADD COLUMN IF NOT EXISTS platform_type varchar(32),
    ADD COLUMN IF NOT EXISTS status varchar(32),
    ADD COLUMN IF NOT EXISTS version_info varchar(16384),
    ADD COLUMN IF NOT EXISTS store_info varchar(16384),
    DROP CONSTRAINT IF EXISTS mobile_app_pkg_name_key;

-- rename mobile_app_oauth2_client to mobile_app_bundle_oauth2_client
DO
$$
    BEGIN
        -- in case of running the upgrade script a second time
        IF EXISTS(SELECT * FROM information_schema.tables WHERE table_name = 'mobile_app_oauth2_client') THEN
            ALTER TABLE mobile_app_oauth2_client RENAME TO mobile_app_bundle_oauth2_client;
            ALTER TABLE mobile_app_bundle_oauth2_client DROP CONSTRAINT IF EXISTS fk_domain;
            ALTER TABLE mobile_app_bundle_oauth2_client RENAME COLUMN mobile_app_id TO mobile_app_bundle_id;
        END IF;
    END;
$$;


CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- duplicate each mobile app and create mobile app bundle for the pair of android and ios app
DO
$$
    DECLARE
        generatedBundleId uuid;
        iosAppId uuid;
        mobileAppRecord RECORD;
    BEGIN
        -- in case of running the upgrade script a second time
        IF EXISTS(SELECT * FROM information_schema.columns WHERE table_name = 'mobile_app' and column_name = 'oauth2_enabled') THEN
            UPDATE mobile_app SET platform_type = 'ANDROID' WHERE platform_type IS NULL;
            UPDATE mobile_app SET status = 'PUBLISHED' WHERE mobile_app.status IS NULL;
            FOR mobileAppRecord IN SELECT * FROM mobile_app
            LOOP
                -- duplicate app for iOS platform type
                iosAppId := uuid_generate_v4();
                INSERT INTO mobile_app(id, created_time, tenant_id, pkg_name, app_secret, platform_type, status)
                VALUES (iosAppId, (extract(epoch from now()) * 1000), mobileAppRecord.tenant_id, mobileAppRecord.pkg_name, mobileAppRecord.app_secret, 'IOS', mobileAppRecord.status);
                -- create bundle for android and iOS app
                generatedBundleId := uuid_generate_v4();
                INSERT INTO mobile_app_bundle(id, created_time, tenant_id, title, android_app_id, ios_app_id, oauth2_enabled)
                    VALUES (generatedBundleId, (extract(epoch from now()) * 1000), mobileAppRecord.tenant_id,
                            'App bundle ' || mobileAppRecord.pkg_name, mobileAppRecord.id, iosAppId, mobileAppRecord.oauth2_enabled);
                UPDATE mobile_app_bundle_oauth2_client SET mobile_app_bundle_id = generatedBundleId WHERE mobile_app_bundle_id = mobileAppRecord.id;
            END LOOP;
        END IF;
        ALTER TABLE mobile_app DROP COLUMN IF EXISTS oauth2_enabled;
        IF NOT EXISTS(SELECT 1 FROM pg_constraint WHERE conname = 'pkg_platform_unique') THEN
            ALTER TABLE mobile_app ADD CONSTRAINT pkg_platform_unique UNIQUE (pkg_name, platform_type);
        END IF;
    END;
$$;

ALTER TABLE IF EXISTS mobile_app_settings RENAME TO qr_code_settings;
ALTER TABLE qr_code_settings ADD COLUMN IF NOT EXISTS mobile_app_bundle_id uuid;

-- migrate mobile apps from qr code settings to mobile_app, create mobile app bundle for the pair of apps
DO
$$
    DECLARE
        iosPkgName varchar;
        androidAppId uuid;
        iosAppId uuid;
        generatedBundleId uuid;
        qrCodeRecord RECORD;
    BEGIN
        -- in case of running the upgrade script a second time
        IF EXISTS(SELECT * FROM information_schema.columns WHERE table_name = 'qr_code_settings' and column_name = 'android_config') THEN
            FOR qrCodeRecord IN SELECT * FROM qr_code_settings
            LOOP
                generatedBundleId := NULL;
                -- migrate android config
                SELECT id into androidAppId FROM mobile_app WHERE pkg_name = qrCodeRecord.android_config::jsonb ->> 'appPackage' AND platform_type = 'ANDROID';
                IF androidAppId IS NULL THEN
                    androidAppId := uuid_generate_v4();
                    INSERT INTO mobile_app(id, created_time, tenant_id, pkg_name, platform_type, status, store_info)
                    VALUES (androidAppId, (extract(epoch from now()) * 1000), qrCodeRecord.tenant_id,
                            qrCodeRecord.android_config::jsonb ->> 'appPackage', 'ANDROID', 'PUBLISHED', qrCodeRecord.android_config::jsonb - 'appPackage');
                    generatedBundleId := uuid_generate_v4();
                    INSERT INTO mobile_app_bundle(id, created_time, tenant_id, title, android_app_id)
                    VALUES (generatedBundleId, (extract(epoch from now()) * 1000), qrCodeRecord.tenant_id, 'App bundle for qr code', androidAppId);
                    UPDATE qr_code_settings SET mobile_app_bundle_id = generatedBundleId WHERE id = qrCodeRecord.id;
                ELSE
                    UPDATE mobile_app SET store_info = qrCodeRecord.android_config::jsonb - 'appPackage' WHERE id = androidAppId;
                    UPDATE qr_code_settings SET mobile_app_bundle_id = (SELECT id FROM mobile_app_bundle WHERE mobile_app_bundle.android_app_id = androidAppId) WHERE id = qrCodeRecord.id;
                END IF;

                -- migrate ios config
                iosPkgName := substring(qrCodeRecord.ios_config::jsonb ->> 'appId', strpos(qrCodeRecord.ios_config::jsonb ->> 'appId', '.') + 1);
                SELECT id into iosAppId FROM mobile_app WHERE pkg_name = iosPkgName AND platform_type = 'IOS';
                IF iosAppId IS NULL THEN
                    iosAppId := uuid_generate_v4();
                    INSERT INTO mobile_app(id, created_time, tenant_id, pkg_name, platform_type, status, store_info)
                    VALUES (iosAppId, (extract(epoch from now()) * 1000), qrCodeRecord.tenant_id,
                            iosPkgName, 'IOS', 'PUBLISHED', qrCodeRecord.ios_config);
                    IF generatedBundleId IS NULL THEN
                        generatedBundleId := uuid_generate_v4();
                        INSERT INTO mobile_app_bundle(id, created_time, tenant_id, title, ios_app_id)
                        VALUES (generatedBundleId, (extract(epoch from now()) * 1000), qrCodeRecord.tenant_id, 'App bundle for qr code', iosAppId);
                        UPDATE qr_code_settings SET mobile_app_bundle_id = generatedBundleId WHERE id = qrCodeRecord.id;
                    ELSE
                        UPDATE mobile_app_bundle SET ios_app_id = iosAppId WHERE id = generatedBundleId;
                    END IF;
                ELSE
                    UPDATE mobile_app SET store_info = qrCodeRecord.ios_config WHERE id = iosAppId;
                    UPDATE qr_code_settings SET mobile_app_bundle_id = (SELECT id FROM mobile_app_bundle WHERE mobile_app_bundle.ios_app_id = iosAppId) WHERE id = qrCodeRecord.id;
                END IF;
            END LOOP;
            ALTER TABLE qr_code_settings RENAME CONSTRAINT mobile_app_settings_tenant_id_unq_key TO qr_code_settings_tenant_id_unq_key;
        END IF;
        ALTER TABLE qr_code_settings DROP COLUMN IF EXISTS android_config, DROP COLUMN IF EXISTS ios_config;
    END;
$$;

-- migrate self-registration attributes to white-labeling
ALTER TABLE white_labeling DROP CONSTRAINT IF EXISTS white_labeling_domain_name_key;
ALTER TABLE white_labeling ALTER COLUMN type TYPE varchar(30);

DO
$$
    BEGIN
        IF NOT EXISTS(SELECT 1 FROM pg_constraint WHERE conname = 'white_labeling_domain_name_type_key') THEN
            ALTER TABLE white_labeling ADD CONSTRAINT white_labeling_domain_name_type_key UNIQUE (domain_name, type);
        END IF;
    END;
$$;

-- migrate tenant self-registration attributes to white_labeling
INSERT INTO white_labeling(tenant_id, customer_id, type, settings, domain_name)
    (SELECT a.entity_id, '13814000-1dd2-11b2-8080-808080808080', 'SELF_REGISTRATION', a.str_v, a.str_v::jsonb ->> 'domainName' FROM tenant t
        INNER JOIN attribute_kv a ON t.id = a.entity_id AND a.attribute_type = 2 AND a.attribute_key = (select key_id from key_dictionary  where key = 'selfRegistrationParams'))
ON CONFLICT DO NOTHING;

-- migrate mobile self-registration attributes
DO
$$
    DECLARE
        pkgName varchar;
        wlRecord RECORD;
        androidAppId uuid;
        iosAppId uuid;
        generatedBundleId uuid;
    BEGIN
       FOR wlRecord IN SELECT * FROM white_labeling
            LOOP
               generatedBundleId := NULL;
               IF (wlRecord.settings::jsonb -> 'pkgName' IS NOT NULL AND wlRecord.settings::jsonb->> 'pkgName' <> '') THEN
                   pkgName := wlRecord.settings::jsonb ->> 'pkgName';
                   SELECT id into androidAppId FROM mobile_app WHERE pkg_name = pkgName AND platform_type = 'ANDROID';
                   IF androidAppId IS NULL THEN
                       androidAppId := uuid_generate_v4();
                       INSERT INTO mobile_app(id, created_time, tenant_id, pkg_name, platform_type, status)
                       VALUES (androidAppId, (extract(epoch from now()) * 1000), wlRecord.tenant_id, pkgName, 'ANDROID', 'PUBLISHED');
                       generatedBundleId := uuid_generate_v4();
                       INSERT INTO mobile_app_bundle(id, created_time, tenant_id, title, android_app_id, self_registration_config)
                       VALUES (generatedBundleId, (extract(epoch from now()) * 1000), wlRecord.tenant_id, androidAppId, wlRecord.settings);
                   ELSE
                       UPDATE mobile_app_bundle SET self_registration_config = wlRecord.settings WHERE android_app_id = androidAppId;
                   END IF;

                   SELECT id into iosAppId FROM mobile_app WHERE pkg_name = pkgName AND platform_type = 'IOS';
                   IF iosAppId IS NULL THEN
                       iosAppId := uuid_generate_v4();
                       INSERT INTO mobile_app(id, created_time, tenant_id, pkg_name, platform_type, status)
                       VALUES (iosAppId, (extract(epoch from now()) * 1000), wlRecord.tenant_id, pkgName, 'IOS', 'PUBLISHED');
                       IF generatedBundleId IS NULL THEN
                           generatedBundleId := uuid_generate_v4();
                           INSERT INTO mobile_app_bundle(id, created_time, tenant_id, title, ios_app_id, self_registration_config)
                           VALUES (generatedBundleId, (extract(epoch from now()) * 1000), wlRecord.tenant_id, iosAppId, wlRecord.settings);
                       ELSE
                           UPDATE mobile_app_bundle SET ios_app_id = iosAppId, self_registration_config = wlRecord.settings WHERE id = generatedBundleId;
                       END IF;
                   ELSE
                       UPDATE mobile_app_bundle SET self_registration_config = wlRecord.settings WHERE ios_app_id = iosAppId;
                   END IF;
               END IF;
       END LOOP;
    END;
$$;