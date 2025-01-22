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

ALTER TABLE user_credentials ADD COLUMN IF NOT EXISTS last_login_ts BIGINT;
UPDATE user_credentials c SET last_login_ts = (SELECT (additional_info::json ->> 'lastLoginTs')::bigint FROM tb_user u WHERE u.id = c.user_id)
  WHERE last_login_ts IS NULL;

ALTER TABLE user_credentials ADD COLUMN IF NOT EXISTS failed_login_attempts INT;
UPDATE user_credentials c SET failed_login_attempts = (SELECT (additional_info::json ->> 'failedLoginAttempts')::int FROM tb_user u WHERE u.id = c.user_id)
  WHERE failed_login_attempts IS NULL;

UPDATE tb_user SET additional_info = (additional_info::jsonb - 'lastLoginTs' - 'failedLoginAttempts' - 'userCredentialsEnabled')::text
  WHERE additional_info IS NOT NULL AND additional_info != 'null' AND jsonb_typeof(additional_info::jsonb) = 'object';

-- UPDATE RULE NODE DEBUG MODE TO DEBUG STRATEGY START

ALTER TABLE rule_node ADD COLUMN IF NOT EXISTS debug_settings varchar(1024) DEFAULT null;
DO
$$
    BEGIN
        IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'rule_node' AND column_name = 'debug_mode')
            THEN
                UPDATE rule_node SET debug_settings = '{"failuresEnabled": true, "allEnabledUntil": ' || cast((extract(epoch from now()) + 900) * 1000 as bigint) || '}' WHERE debug_mode = true; -- 15 minutes according to thingsboard.yml default settings.
                ALTER TABLE rule_node DROP COLUMN debug_mode;
        END IF;
    END
$$;

-- UPDATE RULE NODE DEBUG MODE TO DEBUG STRATEGY END

-- UPDATE INTEGRATION DEBUG MODE TO DEBUG STRATEGY START

DROP VIEW IF EXISTS integration_info;
ALTER TABLE integration ADD COLUMN IF NOT EXISTS debug_settings varchar(1024) DEFAULT null;
DO
$$
    BEGIN
        IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'integration' AND column_name = 'debug_mode') THEN
            UPDATE integration SET debug_settings = '{"failuresEnabled": true, "allEnabledUntil": ' || cast((extract(epoch from now()) + 900) * 1000 as bigint) || '}' WHERE debug_mode = true; -- 15 minutes according to thingsboard.yml default settings.
            ALTER TABLE integration DROP COLUMN debug_mode;
        END IF;
    END
$$;

-- UPDATE INTEGRATION DEBUG MODE TO DEBUG STRATEGY END

-- UPDATE CONVERTER DEBUG MODE TO DEBUG STRATEGY START

ALTER TABLE converter ADD COLUMN IF NOT EXISTS debug_settings varchar(1024) DEFAULT null;
DO
$$
    BEGIN
        IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'converter' AND column_name = 'debug_mode') THEN
            UPDATE converter SET debug_settings = '{"failuresEnabled": true, "allEnabledUntil": ' || cast((extract(epoch from now()) + 900) * 1000 as bigint) || '}' WHERE debug_mode = true; -- 15 minutes according to thingsboard.yml default settings.
            ALTER TABLE converter DROP COLUMN debug_mode;
        END IF;
    END
$$;

-- UPDATE CONVERTER DEBUG MODE TO DEBUG STRATEGY END

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
    terms_of_use varchar(10000000),
    privacy_policy varchar(10000000),
    oauth2_enabled boolean,
    CONSTRAINT fk_android_app_id FOREIGN KEY (android_app_id) REFERENCES mobile_app(id) ON DELETE SET NULL,
    CONSTRAINT fk_ios_app_id FOREIGN KEY (ios_app_id) REFERENCES mobile_app(id) ON DELETE SET NULL
);
CREATE INDEX IF NOT EXISTS mobile_app_bundle_tenant_id ON mobile_app_bundle(tenant_id);

ALTER TABLE mobile_app ADD COLUMN IF NOT EXISTS platform_type varchar(32),
    ADD COLUMN IF NOT EXISTS status varchar(32),
    ADD COLUMN IF NOT EXISTS version_info varchar(100000),
    ADD COLUMN IF NOT EXISTS store_info varchar(16384),
    DROP CONSTRAINT IF EXISTS mobile_app_pkg_name_key,
    DROP CONSTRAINT IF EXISTS mobile_app_unq_key;

-- rename mobile_app_oauth2_client to mobile_app_bundle_oauth2_client
DO
$$
    BEGIN
        -- in case of running the upgrade script a second time
        IF EXISTS(SELECT 1 FROM information_schema.tables WHERE table_name = 'mobile_app_oauth2_client') THEN
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
        IF EXISTS(SELECT 1 FROM information_schema.columns WHERE table_name = 'mobile_app' and column_name = 'oauth2_enabled') THEN
            UPDATE mobile_app SET platform_type = 'ANDROID' WHERE platform_type IS NULL;
            UPDATE mobile_app SET status = 'DRAFT' WHERE mobile_app.status IS NULL;
            FOR mobileAppRecord IN SELECT * FROM mobile_app
            LOOP
                -- duplicate app for iOS platform type
                iosAppId := uuid_generate_v4();
                INSERT INTO mobile_app(id, created_time, tenant_id, pkg_name, app_secret, platform_type, status)
                VALUES (iosAppId, mobileAppRecord.created_time, mobileAppRecord.tenant_id, mobileAppRecord.pkg_name, mobileAppRecord.app_secret, 'IOS', mobileAppRecord.status)
                ON CONFLICT DO NOTHING;
                -- create bundle for android and iOS app
                generatedBundleId := uuid_generate_v4();
                INSERT INTO mobile_app_bundle(id, created_time, tenant_id, title, android_app_id, ios_app_id, oauth2_enabled)
                VALUES (generatedBundleId, mobileAppRecord.created_time, mobileAppRecord.tenant_id,
                        mobileAppRecord.pkg_name || ' (autogenerated)', mobileAppRecord.id, iosAppId, mobileAppRecord.oauth2_enabled)
                ON CONFLICT DO NOTHING;
                UPDATE mobile_app_bundle_oauth2_client SET mobile_app_bundle_id = generatedBundleId WHERE mobile_app_bundle_id = mobileAppRecord.id;
            END LOOP;
        END IF;
        IF NOT EXISTS(SELECT 1 FROM pg_constraint WHERE conname = 'fk_mobile_app_bundle_oauth2_client_bundle_id') THEN
            ALTER TABLE mobile_app_bundle_oauth2_client ADD CONSTRAINT fk_mobile_app_bundle_oauth2_client_bundle_id
                FOREIGN KEY (mobile_app_bundle_id) REFERENCES mobile_app_bundle(id) ON DELETE CASCADE;
        END IF;
        ALTER TABLE mobile_app DROP COLUMN IF EXISTS oauth2_enabled;
        IF NOT EXISTS(SELECT 1 FROM pg_constraint WHERE conname = 'mobile_app_pkg_name_platform_unq_key') THEN
            ALTER TABLE mobile_app ADD CONSTRAINT mobile_app_pkg_name_platform_unq_key UNIQUE (pkg_name, platform_type);
        END IF;
    END;
$$;

ALTER TABLE IF EXISTS mobile_app_settings RENAME TO qr_code_settings;
ALTER TABLE qr_code_settings ADD COLUMN IF NOT EXISTS mobile_app_bundle_id uuid,
    ADD COLUMN IF NOT EXISTS android_enabled boolean DEFAULT true,
    ADD COLUMN IF NOT EXISTS ios_enabled boolean DEFAULT true;

-- migrate mobile apps from qr code settings to mobile_app, create mobile app bundle for the pair of apps
DO
$$
    DECLARE
        androidPkgName varchar;
        iosPkgName varchar;
        androidAppId uuid;
        iosAppId uuid;
        androidApp RECORD;
        iosApp RECORD;
        generatedBundleId uuid;
        qrCodeRecord RECORD;
    BEGIN
        -- in case of running the upgrade script a second time
        IF EXISTS(SELECT 1 FROM information_schema.columns WHERE table_name = 'qr_code_settings' AND column_name = 'android_config') THEN
            FOR qrCodeRecord IN SELECT * FROM qr_code_settings
            LOOP
                generatedBundleId := NULL;
                -- migrate android config
                IF (qrCodeRecord.android_config::jsonb ->> 'appPackage' IS NOT NULL) THEN
                    androidPkgName := qrCodeRecord.android_config::jsonb ->> 'appPackage';
                    SELECT * into androidApp FROM mobile_app WHERE pkg_name = androidPkgName AND platform_type = 'ANDROID';
                    IF androidApp IS NULL THEN
                        androidAppId := uuid_generate_v4();
                        INSERT INTO mobile_app(id, created_time, tenant_id, pkg_name, platform_type, status, store_info)
                        VALUES (androidAppId, (extract(epoch from now()) * 1000), qrCodeRecord.tenant_id,
                                androidPkgName, 'ANDROID', 'DRAFT', qrCodeRecord.android_config::jsonb - 'appPackage' - 'enabled');
                        generatedBundleId := uuid_generate_v4();
                        INSERT INTO mobile_app_bundle(id, created_time, tenant_id, title, android_app_id)
                        VALUES (generatedBundleId, (extract(epoch from now()) * 1000), qrCodeRecord.tenant_id, androidPkgName || ' (autogenerated)', androidAppId);
                        UPDATE qr_code_settings SET mobile_app_bundle_id = generatedBundleId WHERE id = qrCodeRecord.id;
                    ELSE
                        IF qrCodeRecord.tenant_id = androidApp.tenant_id THEN
                            UPDATE mobile_app SET store_info = qrCodeRecord.android_config::jsonb - 'appPackage' - 'enabled' WHERE id = androidAppId;
                            UPDATE qr_code_settings SET mobile_app_bundle_id = (SELECT id FROM mobile_app_bundle WHERE mobile_app_bundle.android_app_id = androidAppId) WHERE id = qrCodeRecord.id;
                        ELSE
                            UPDATE qr_code_settings SET use_default_app = true WHERE id = qrCodeRecord.id;
                        END IF;
                    END IF;
                END IF;
                UPDATE qr_code_settings SET android_enabled = (qrCodeRecord.android_config::jsonb ->> 'enabled')::boolean WHERE id = qrCodeRecord.id;

                -- migrate ios config
                IF (qrCodeRecord.ios_config::jsonb ->> 'appId' IS NOT NULL) THEN
                    iosPkgName := substring(qrCodeRecord.ios_config::jsonb ->> 'appId', strpos(qrCodeRecord.ios_config::jsonb ->> 'appId', '.') + 1);
                    SELECT * INTO iosApp FROM mobile_app WHERE pkg_name = iosPkgName AND platform_type = 'IOS';
                    IF iosApp IS NULL THEN
                        iosAppId := uuid_generate_v4();
                        INSERT INTO mobile_app(id, created_time, tenant_id, pkg_name, platform_type, status, store_info)
                        VALUES (iosAppId, (extract(epoch from now()) * 1000), qrCodeRecord.tenant_id,
                                iosPkgName, 'IOS', 'DRAFT', qrCodeRecord.ios_config::jsonb - 'enabled');
                        IF generatedBundleId IS NULL THEN
                            generatedBundleId := uuid_generate_v4();
                            INSERT INTO mobile_app_bundle(id, created_time, tenant_id, title, ios_app_id)
                            VALUES (generatedBundleId, (extract(epoch from now()) * 1000), qrCodeRecord.tenant_id, iosPkgName || ' (autogenerated)', iosAppId);
                            UPDATE qr_code_settings SET mobile_app_bundle_id = generatedBundleId WHERE id = qrCodeRecord.id;
                        ELSE
                            UPDATE mobile_app_bundle SET ios_app_id = iosAppId WHERE id = generatedBundleId;
                        END IF;
                    ELSE
                        IF qrCodeRecord.tenant_id = iosApp.tenant_id THEN
                            UPDATE qr_code_settings SET mobile_app_bundle_id = (SELECT id FROM mobile_app_bundle WHERE mobile_app_bundle.ios_app_id = iosAppId) WHERE id = qrCodeRecord.id;
                            UPDATE mobile_app SET store_info = qrCodeRecord.ios_config::jsonb - 'enabled' WHERE id = iosAppId;
                        ELSE
                            UPDATE qr_code_settings SET use_default_app = true WHERE id = qrCodeRecord.id;
                        END IF;
                    END IF;
                END IF;
                UPDATE qr_code_settings SET ios_enabled = (qrCodeRecord.ios_config::jsonb -> 'enabled')::boolean WHERE id = qrCodeRecord.id;
            END LOOP;
            ALTER TABLE qr_code_settings RENAME CONSTRAINT mobile_app_settings_tenant_id_unq_key TO qr_code_settings_tenant_id_unq_key;
            ALTER TABLE qr_code_settings RENAME CONSTRAINT mobile_app_settings_pkey TO qr_code_settings_pkey;
        END IF;
        ALTER TABLE qr_code_settings DROP COLUMN IF EXISTS android_config, DROP COLUMN IF EXISTS ios_config;
    END;
$$;

-- update constraint name
DO
$$
    BEGIN
        ALTER TABLE domain DROP CONSTRAINT IF EXISTS domain_unq_key;
        IF NOT EXISTS(SELECT 1 FROM pg_constraint WHERE conname = 'domain_name_key') THEN
            ALTER TABLE domain ADD CONSTRAINT domain_name_key UNIQUE (name);
        END IF;
    END;
$$;

-- UPDATE RESOURCE JS_MODULE SUB TYPE START

UPDATE resource SET resource_sub_type = 'EXTENSION' WHERE resource_type = 'JS_MODULE' AND resource_sub_type IS NULL;

-- UPDATE RESOURCE JS_MODULE SUB TYPE END

-- migrate self-registration attributes
DO
$$
    DECLARE
        pkgName varchar;
        srSettings record;
        androidApp record;
        androidAppId uuid;
        iosApp record;
        iosAppId uuid;
        generatedBundleId uuid;
    BEGIN
        IF EXISTS(SELECT 1 FROM information_schema.columns WHERE table_name = 'white_labeling' AND column_name = 'domain_name') THEN
            ALTER TABLE white_labeling DROP CONSTRAINT IF EXISTS white_labeling_domain_name_key;
            ALTER TABLE white_labeling ALTER COLUMN type TYPE varchar(30);
            IF NOT EXISTS(SELECT 1 FROM pg_constraint WHERE conname = 'white_labeling_domain_name_type_key') THEN
                ALTER TABLE white_labeling ADD CONSTRAINT white_labeling_domain_name_type_key UNIQUE (domain_name, type);
            END IF;

            INSERT INTO white_labeling(tenant_id, customer_id, type, settings, domain_name)
                (SELECT a.entity_id, '13814000-1dd2-11b2-8080-808080808080', 'SELF_REGISTRATION', a.str_v, a.str_v::jsonb ->> 'domainName' FROM tenant t
                    INNER JOIN admin_settings s ON s.key LIKE 'selfRegistrationDomainNamePrefix%' AND s.json_value::jsonb ->> 'entityId' = t.id::text
                    INNER JOIN attribute_kv a ON t.id = a.entity_id AND a.attribute_type = 2 AND a.attribute_key = (select key_id from key_dictionary where key = 'selfRegistrationParams')
                                                     AND a.str_v IS NOT NULL)
            ON CONFLICT DO NOTHING;

            INSERT INTO white_labeling(tenant_id, customer_id, type, settings, domain_name)
                (SELECT a.entity_id, '13814000-1dd2-11b2-8080-808080808080', 'TERMS_OF_USE', a.str_v, substring(s.key FROM 'selfRegistrationDomainNamePrefix_(.*)') FROM tenant t
                    INNER JOIN admin_settings s ON s.key LIKE 'selfRegistrationDomainNamePrefix%' AND s.json_value::jsonb ->> 'entityId' = t.id::text
                    INNER JOIN attribute_kv a ON t.id = a.entity_id AND a.attribute_type = 2 AND a.attribute_key = (select key_id from key_dictionary where key = 'termsOfUse')
                                                     AND a.str_v IS NOT NULL)
            ON CONFLICT DO NOTHING;

            INSERT INTO white_labeling(tenant_id, customer_id, type, settings, domain_name)
                (SELECT a.entity_id, '13814000-1dd2-11b2-8080-808080808080', 'PRIVACY_POLICY', a.str_v, substring(s.key FROM 'selfRegistrationDomainNamePrefix_(.*)') FROM tenant t
                    INNER JOIN admin_settings s ON s.key LIKE 'selfRegistrationDomainNamePrefix%' AND s.json_value::jsonb ->> 'entityId' = t.id::text
                    INNER JOIN attribute_kv a ON t.id = a.entity_id AND a.attribute_type = 2 AND a.attribute_key = (select key_id from key_dictionary where key = 'privacyPolicy')
                                                     AND a.str_v IS NOT NULL)
            ON CONFLICT DO NOTHING;

            FOR srSettings IN SELECT * FROM attribute_kv a WHERE a.attribute_type = 2
                                                             AND a.attribute_key = (select key_id from key_dictionary where key = 'selfRegistrationParams')
                                                             AND a.str_v IS NOT NULL
                LOOP
                    generatedBundleId := NULL;
                    IF (srSettings.str_v::jsonb -> 'pkgName' IS NOT NULL AND srSettings.str_v::jsonb->> 'pkgName' <> '') THEN
                        pkgName := srSettings.str_v::jsonb ->> 'pkgName';
                        SELECT * into androidApp FROM mobile_app WHERE pkg_name = pkgName AND platform_type = 'ANDROID';
                        IF androidApp IS NULL THEN
                            androidAppId := uuid_generate_v4();
                            INSERT INTO mobile_app(id, created_time, tenant_id, pkg_name, app_secret, platform_type, status)
                            VALUES (androidAppId, (extract(epoch from now()) * 1000), srSettings.entity_id, pkgName,
                                    srSettings.str_v::jsonb ->> 'appSecret', 'ANDROID', 'DRAFT');
                            generatedBundleId := uuid_generate_v4();
                            INSERT INTO mobile_app_bundle(id, created_time, tenant_id, title, android_app_id, self_registration_config, terms_of_use, privacy_policy)
                            VALUES (generatedBundleId, (extract(epoch from now()) * 1000), srSettings.entity_id, pkgName || ' (autogenerated)', androidAppId, srSettings.str_v,
                                    (SELECT settings::jsonb ->> 'termsOfUse' FROM white_labeling WHERE type = 'TERMS_OF_USE' AND tenant_id = srSettings.entity_id AND settings is NOT NULL),
                                    (SELECT settings::jsonb ->> 'privacyPolicy' FROM white_labeling WHERE type = 'PRIVACY_POLICY' AND tenant_id = srSettings.entity_id AND settings is NOT NULL));
                        ELSE IF androidApp.tenant_id = srSettings.entity_id THEN
                            UPDATE mobile_app_bundle SET self_registration_config = srSettings.str_v,
                                                         terms_of_use = (SELECT settings::jsonb ->> 'termsOfUse' FROM white_labeling WHERE type = 'TERMS_OF_USE' AND tenant_id = srSettings.entity_id AND settings is NOT NULL),
                                                         privacy_policy = (SELECT settings::jsonb ->> 'privacyPolicy' FROM white_labeling WHERE type = 'PRIVACY_POLICY' AND tenant_id = srSettings.entity_id AND settings is NOT NULL)
                            WHERE android_app_id = androidApp.id;
                        END IF;
                        END IF;

                        SELECT * into iosApp FROM mobile_app WHERE pkg_name = pkgName AND platform_type = 'IOS';
                        IF iosApp IS NULL THEN
                            iosAppId := uuid_generate_v4();
                            INSERT INTO mobile_app(id, created_time, tenant_id, pkg_name, app_secret, platform_type, status)
                            VALUES (iosAppId, (extract(epoch from now()) * 1000), srSettings.entity_id, pkgName,
                                    srSettings.str_v::jsonb ->> 'appSecret', 'IOS', 'DRAFT');
                            IF generatedBundleId IS NULL THEN
                                generatedBundleId := uuid_generate_v4();
                                INSERT INTO mobile_app_bundle(id, created_time, tenant_id, title, ios_app_id, self_registration_config, terms_of_use, privacy_policy)
                                VALUES (generatedBundleId, (extract(epoch from now()) * 1000), srSettings.entity_id, pkgName || ' (autogenerated)', iosAppId, srSettings.str_v,
                                        (SELECT settings::jsonb ->> 'termsOfUse' FROM white_labeling WHERE type = 'TERMS_OF_USE' AND tenant_id = srSettings.entity_id AND settings is NOT NULL),
                                        (SELECT settings::jsonb ->> 'privacyPolicy' FROM white_labeling WHERE type = 'PRIVACY_POLICY' AND tenant_id = srSettings.entity_id AND settings is NOT NULL));
                            ELSE
                                UPDATE mobile_app_bundle SET ios_app_id = iosAppId WHERE id = generatedBundleId;
                            END IF;
                        ELSE IF iosApp.tenant_id = srSettings.entity_id THEN
                            UPDATE mobile_app_bundle SET self_registration_config = srSettings.str_v,
                                                         terms_of_use = (SELECT settings::jsonb ->> 'termsOfUse' FROM white_labeling WHERE type = 'TERMS_OF_USE' AND tenant_id = srSettings.entity_id AND settings is NOT NULL),
                                                         privacy_policy = (SELECT settings::jsonb ->> 'privacyPolicy' FROM white_labeling WHERE type = 'PRIVACY_POLICY' AND tenant_id = srSettings.entity_id AND settings is NOT NULL)
                                                     WHERE ios_app_id = iosApp.id;
                            END IF;
                        END IF;
                    END IF;
            END LOOP;
            DELETE FROM attribute_kv WHERE attribute_key IN (SELECT key_id FROM key_dictionary WHERE key IN ('selfRegistrationParams', 'termsOfUse', 'privacyPolicy'));
        END IF;
    END;
$$;

-- convert WEB self-registration settings to new structure
DO
$$
    BEGIN
        -- in case of running the upgrade script a second time
        IF (SELECT settings::jsonb -> 'captcha' FROM white_labeling WHERE white_labeling.type = 'SELF_REGISTRATION' LIMIT 1) IS NULL THEN
            UPDATE white_labeling SET settings = json_build_object(
                    'enabled', true,
                    'type', 'WEB',
                    'title', settings::jsonb ->> 'signUpTextMessage',
                    'notificationEmail', settings::jsonb ->> 'notificationEmail',
                    'showPrivacyPolicy', settings::jsonb ->> 'showPrivacyPolicy',
                    'showTermsOfUse', settings::jsonb ->> 'showTermsOfUse',
                    'permissions', settings::jsonb -> 'permissions',
                    'captcha', json_object(ARRAY['siteKey', 'version', 'logActionName', 'secretKey'],
                                           ARRAY[settings::jsonb ->> 'captchaSiteKey',
                                                   settings::jsonb ->> 'captchaVersion',
                                                   settings::jsonb ->> 'captchaAction',
                                                   settings::jsonb ->> 'captchaSecretKey']),
                    'defaultDashboard', json_object(ARRAY['id', 'fullscreen'],
                                                    ARRAY[settings::jsonb ->> 'defaultDashboardId',
                                                            settings::jsonb ->> 'defaultDashboardFullscreen']))
            WHERE type = 'SELF_REGISTRATION' AND settings IS NOT NULL;
        END IF;
    END;
$$;

-- convert MOBILE self-registration settings to new structure
DO
$$
    BEGIN
        -- in case of running the upgrade script a second time
        IF (SELECT self_registration_config::jsonb -> 'captcha' FROM mobile_app_bundle WHERE self_registration_config IS NOT NULL LIMIT 1) IS NULL THEN
            UPDATE mobile_app_bundle SET self_registration_config = json_build_object(
                    'enabled', true,
                    'type', 'MOBILE',
                    'title', self_registration_config::jsonb ->> 'signUpTextMessage',
                    'signUpFields', jsonb_build_array(json_build_object('id', 'EMAIL', 'label', 'Email', 'required', true),
                                                      json_build_object('id', 'FIRST_NAME', 'label', 'First name', 'required', false),
                                                      json_build_object('id', 'LAST_NAME', 'label', 'Last name', 'required', false),
                                                      json_build_object('id', 'PASSWORD', 'label', 'Create password', 'required', true),
                                                      json_build_object('id', 'REPEAT_PASSWORD', 'label', 'Repeat your password', 'required', true)),
                    'notificationEmail', self_registration_config::jsonb ->> 'notificationEmail',
                    'showPrivacyPolicy', self_registration_config::jsonb ->> 'showPrivacyPolicy',
                    'showTermsOfUse', self_registration_config::jsonb ->> 'showTermsOfUse',
                    'permissions', self_registration_config::jsonb -> 'permissions',
                    'captcha', json_object(ARRAY['siteKey', 'version', 'logActionName', 'secretKey'],
                                           ARRAY[self_registration_config::jsonb ->> 'captchaSiteKey',
                                                   self_registration_config::jsonb ->> 'captchaVersion',
                                                   self_registration_config::jsonb ->> 'captchaAction',
                                                   self_registration_config::jsonb ->> 'captchaSecretKey']),
                    'defaultDashboard', json_object(ARRAY['id', 'fullscreen'],
                                                    ARRAY[self_registration_config::jsonb ->> 'defaultDashboardId',
                                                            self_registration_config::jsonb ->> 'defaultDashboardFullscreen']),
                    'redirect', json_object(ARRAY['scheme', 'host'],
                                            ARRAY[self_registration_config::jsonb ->> 'appScheme',
                                                    self_registration_config::jsonb ->> 'appHost']))
            WHERE self_registration_config IS NOT NULL;
        END IF;
    END;
$$;

-- migrating notification email to notification target in self registration configs
CREATE OR REPLACE FUNCTION update_self_registration_notification_config(
    self_registration_settings jsonb,
    tenant uuid
) RETURNS jsonb AS $$
    DECLARE
        notification_email varchar;
        notification_target_id uuid;
        tb_user record;
    BEGIN
        notification_email := self_registration_settings ->> 'notificationEmail';

        IF notification_email IS NOT NULL AND notification_email NOT IN ('', 'null') THEN
            SELECT * FROM tb_user WHERE tenant_id = tenant AND email = notification_email INTO tb_user;
            IF tb_user IS NULL THEN
                SELECT * FROM tb_user WHERE tenant_id = tenant ORDER BY created_time ASC LIMIT 1 INTO tb_user;
            END IF;

            SELECT id INTO notification_target_id FROM notification_target WHERE tenant_id = tenant AND name = tb_user.email;
            IF notification_target_id IS NULL THEN
                notification_target_id = uuid_generate_v4();
                INSERT INTO notification_target (id, created_time, tenant_id, name, configuration)
                    VALUES (notification_target_id, (extract(epoch from now()) * 1000), tenant,
                    tb_user.email, jsonb_build_object('type', 'PLATFORM_USERS', 'description',
                    concat('User ', tb_user.email, '; used in the self registration settings'), 'usersFilter',
                        json_build_object('type', 'USER_LIST', 'usersIds', jsonb_build_array(tb_user.id::text))));
            END IF;

            self_registration_settings = jsonb_set(self_registration_settings, '{notificationRecipient}',
                jsonb_build_object('id', notification_target_id::text, 'entityType', 'NOTIFICATION_TARGET'));
            self_registration_settings = self_registration_settings - 'notificationEmail';
            RETURN self_registration_settings;
        ELSE
            RETURN NULL;
        END IF;
    END;
$$ LANGUAGE plpgsql;

-- updating white labeling self registration records
DO
$$
    DECLARE
        wl_record record;
        self_registration_settings jsonb;
    BEGIN
        FOR wl_record IN SELECT * FROM white_labeling WHERE type = 'SELF_REGISTRATION'
            LOOP
                self_registration_settings := update_self_registration_notification_config(wl_record.settings::jsonb, wl_record.tenant_id);
                IF self_registration_settings IS NOT NULL THEN
                    UPDATE white_labeling SET settings = self_registration_settings WHERE tenant_id = wl_record.tenant_id
                        AND customer_id = wl_record.customer_id AND type = 'SELF_REGISTRATION';
                END IF;
            END LOOP;
    END;
$$;

-- updating mobile bundle self registration records
DO
$$
    DECLARE
        mobile_bundle_record record;
        self_registration_settings jsonb;
    BEGIN
        FOR mobile_bundle_record IN SELECT * FROM mobile_app_bundle WHERE self_registration_config IS NOT NULL
            LOOP
                self_registration_settings := update_self_registration_notification_config(mobile_bundle_record.self_registration_config::jsonb, mobile_bundle_record.tenant_id);
                IF self_registration_settings IS NOT NULL THEN
                    UPDATE mobile_app_bundle SET self_registration_config = self_registration_settings WHERE id = mobile_bundle_record.id;
                END IF;
            END LOOP;
    END;
$$;

DROP FUNCTION IF EXISTS update_self_registration_notification_config;

-- DOMAINS MIGRATION START

-- update domain table structure
DO
$$
    BEGIN
        ALTER TABLE domain ADD COLUMN IF NOT EXISTS customer_id uuid not null default '13814000-1dd2-11b2-8080-808080808080';
        ALTER TABLE domain DROP CONSTRAINT IF EXISTS domain_unq_key;
        IF NOT EXISTS(SELECT 1 FROM pg_constraint WHERE conname = 'domain_name_key') THEN
            ALTER TABLE domain ADD CONSTRAINT domain_name_key UNIQUE (name);
        END IF;
    END;
$$;

-- update white_labeling table structure
DO
$$
    BEGIN
        ALTER TABLE white_labeling DROP CONSTRAINT IF EXISTS white_labeling_domain_name_type_key;
        ALTER TABLE white_labeling ADD COLUMN IF NOT EXISTS domain_id uuid;
        IF NOT EXISTS(SELECT 1 FROM pg_constraint WHERE conname = 'white_labeling_domain_id_type_key') THEN
            ALTER TABLE white_labeling ADD CONSTRAINT white_labeling_domain_id_type_key UNIQUE (domain_id, type);
        END IF;
        IF NOT EXISTS(SELECT 1 FROM pg_constraint WHERE conname = 'fk_white_labeling_domain_id') THEN
            ALTER TABLE white_labeling ADD CONSTRAINT fk_white_labeling_domain_id FOREIGN KEY (domain_id) REFERENCES domain(id);
        END IF;
    END;
$$;

-- migrate white_labeling.domain_name -> domain_id
DO
$$
    DECLARE
        generatedDomainId uuid;
        wlRecord record;
        domainRecord record;
    BEGIN
        IF EXISTS(SELECT 1 FROM information_schema.columns WHERE table_name = 'white_labeling' AND column_name = 'domain_name') THEN
            FOR wlRecord IN SELECT * FROM white_labeling WHERE domain_name IS NOT NULL
            LOOP
                SELECT * INTO domainRecord FROM domain WHERE domain.name = wlRecord.domain_name;
                IF domainRecord IS NULL THEN
                    generatedDomainId := uuid_generate_v4();
                    INSERT INTO domain(id, created_time, tenant_id, customer_id, name, oauth2_enabled, edge_enabled)
                    VALUES (generatedDomainId, (extract(epoch from now()) * 1000), wlRecord.tenant_id, wlRecord.customer_id, wlRecord.domain_name, true, true);
                    UPDATE white_labeling SET domain_id = generatedDomainId WHERE domain_name = wlRecord.domain_name AND type = wlRecord.type;
                ELSE IF (domainRecord.tenant_id = wlRecord.tenant_id AND domainRecord.customer_id = wlRecord.customer_id) THEN
                        UPDATE white_labeling SET domain_id = domainRecord.id WHERE domain_name = wlRecord.domain_name AND type = wlRecord.type;
                    END IF;
                END IF;
            END LOOP;
            ALTER TABLE white_labeling DROP COLUMN IF EXISTS domain_name;
            UPDATE white_labeling SET settings = (settings::jsonb - 'domainName')::text WHERE type = 'LOGIN';
        END IF;
    END;
$$;

ALTER TABLE oauth2_client ADD COLUMN IF NOT EXISTS customer_id uuid not null default '13814000-1dd2-11b2-8080-808080808080';

-- UPDATE SAVE TIME SERIES NODES START

DO $$
    BEGIN
        -- Check if the rule_node table exists
        IF EXISTS (
            SELECT 1
            FROM information_schema.tables
            WHERE table_name = 'rule_node'
        ) THEN

            UPDATE rule_node
            SET configuration = (
                (configuration::jsonb - 'skipLatestPersistence')
                    || jsonb_build_object(
                        'persistenceSettings', jsonb_build_object(
                                'type',       'ADVANCED',
                                'timeseries', jsonb_build_object('type', 'ON_EVERY_MESSAGE'),
                                'latest',     jsonb_build_object('type', 'SKIP'),
                                'webSockets', jsonb_build_object('type', 'ON_EVERY_MESSAGE')
                                               )
                       )
                )::text,
                configuration_version = 1
            WHERE type = 'org.thingsboard.rule.engine.telemetry.TbMsgTimeseriesNode'
              AND configuration_version = 0
              AND configuration::jsonb ->> 'skipLatestPersistence' = 'true';

            UPDATE rule_node
            SET configuration = (
                (configuration::jsonb - 'skipLatestPersistence')
                    || jsonb_build_object(
                        'persistenceSettings', jsonb_build_object(
                                'type', 'ON_EVERY_MESSAGE'
                                               )
                       )
                )::text,
                configuration_version = 1
            WHERE type = 'org.thingsboard.rule.engine.telemetry.TbMsgTimeseriesNode'
              AND configuration_version = 0
              AND (configuration::jsonb ->> 'skipLatestPersistence' != 'true' OR configuration::jsonb ->> 'skipLatestPersistence' IS NULL);

        END IF;
    END;
$$;

-- UPDATE SAVE TIME SERIES NODES END
