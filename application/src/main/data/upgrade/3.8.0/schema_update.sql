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
    terms_of_use varchar(10000000),
    privacy_policy varchar(10000000),
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
            UPDATE mobile_app SET status = 'DRAFT' WHERE mobile_app.status IS NULL;
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

-- migrate qr code settings to mobile_app, create mobile app bundle for the pair of apps if needed
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
                            qrCodeRecord.android_config::jsonb ->> 'appPackage', 'ANDROID', 'DRAFT', qrCodeRecord.android_config::jsonb - 'appPackage');
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
                            iosPkgName, 'IOS', 'DRAFT', qrCodeRecord.ios_config);
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

-- migrate self-registration for web to white-labeling
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

INSERT INTO white_labeling(tenant_id, customer_id, type, settings, domain_name)
    (SELECT a.entity_id, '13814000-1dd2-11b2-8080-808080808080', 'SELF_REGISTRATION', a.str_v, a.str_v::jsonb ->> 'domainName' FROM tenant t
        INNER JOIN admin_settings s ON s.key LIKE 'selfRegistrationDomainNamePrefix%' AND s.json_value::jsonb ->> 'entityId' = t.id::text
        INNER JOIN attribute_kv a ON t.id = a.entity_id AND a.attribute_type = 2 AND a.attribute_key = (select key_id from key_dictionary where key = 'selfRegistrationParams'))
ON CONFLICT DO NOTHING;

INSERT INTO white_labeling(tenant_id, customer_id, type, settings, domain_name)
    (SELECT a.entity_id, '13814000-1dd2-11b2-8080-808080808080', 'TERMS_OF_USE', a.str_v, a.str_v::jsonb ->> 'domainName' FROM tenant t
        INNER JOIN admin_settings s ON s.key LIKE 'selfRegistrationDomainNamePrefix%' AND s.json_value::jsonb ->> 'entityId' = t.id::text
        INNER JOIN attribute_kv a ON t.id = a.entity_id AND a.attribute_type = 2 AND a.attribute_key = (select key_id from key_dictionary where key = 'termsOfUse'))
ON CONFLICT DO NOTHING;

INSERT INTO white_labeling(tenant_id, customer_id, type, settings, domain_name)
    (SELECT a.entity_id, '13814000-1dd2-11b2-8080-808080808080', 'PRIVACY_POLICY', a.str_v, a.str_v::jsonb ->> 'domainName' FROM tenant t
        INNER JOIN admin_settings s ON s.key LIKE 'selfRegistrationDomainNamePrefix%' AND s.json_value::jsonb ->> 'entityId' = t.id::text
        INNER JOIN attribute_kv a ON t.id = a.entity_id AND a.attribute_type = 2 AND a.attribute_key = (select key_id from key_dictionary where key = 'privacyPolicy'))
ON CONFLICT DO NOTHING;

-- DELETE FROM attribute_kv WHERE attribute_key = (select key_id from key_dictionary where key = 'selfRegistrationParams') OR
--         attribute_key = (select key_id from key_dictionary where key = 'termsOfUse') OR
--         attribute_key = (select key_id from key_dictionary where key = 'privacyPolicy');

-- migrate mobile self-registration attributes
DO
$$
    DECLARE
        pkgName varchar;
        wlRecord record;
        androidApp record;
        androidAppId uuid;
        iosApp record;
        iosAppId uuid;
        generatedBundleId uuid;
    BEGIN
       FOR wlRecord IN SELECT * FROM white_labeling WHERE type = 'SELF_REGISTRATION'
            LOOP
               generatedBundleId := NULL;
               IF (wlRecord.settings::jsonb -> 'pkgName' IS NOT NULL AND wlRecord.settings::jsonb->> 'pkgName' <> '') THEN
                   pkgName := wlRecord.settings::jsonb ->> 'pkgName';
                   SELECT * into androidApp FROM mobile_app WHERE pkg_name = pkgName AND platform_type = 'ANDROID';
                   IF androidApp IS NULL THEN
                       androidAppId := uuid_generate_v4();
                       INSERT INTO mobile_app(id, created_time, tenant_id, pkg_name, platform_type, status)
                       VALUES (androidAppId, (extract(epoch from now()) * 1000), wlRecord.tenant_id, pkgName, 'ANDROID', 'DRAFT');
                       generatedBundleId := uuid_generate_v4();
                       INSERT INTO mobile_app_bundle(id, created_time, tenant_id, title, android_app_id, self_registration_config, terms_of_use, privacy_policy)
                       VALUES (generatedBundleId, (extract(epoch from now()) * 1000), wlRecord.tenant_id, 'Autogenerated for '|| pkgName, androidAppId, wlRecord.settings,
                               (SELECT settings FROM white_labeling WHERE type = 'TERMS_OF_USE' AND tenant_id = wlRecord.tenant_id),
                               (SELECT settings FROM white_labeling WHERE type = 'PRIVACY_POLICY' AND tenant_id = wlRecord.tenant_id));
                   ELSE IF androidApp.tenant_id != '13814000-1dd2-11b2-8080-808080808080' THEN
                       UPDATE mobile_app_bundle SET self_registration_config = wlRecord.settings,
                                                    terms_of_use = (SELECT settings FROM white_labeling WHERE type = 'TERMS_OF_USE' AND tenant_id = wlRecord.tenant_id),
                                                    privacy_policy = (SELECT settings FROM white_labeling WHERE type = 'PRIVACY_POLICY' AND tenant_id = wlRecord.tenant_id)
                                                WHERE android_app_id = androidAppId;
                       END IF;
                   END IF;

                   SELECT * into iosApp FROM mobile_app WHERE pkg_name = pkgName AND platform_type = 'IOS';
                   IF iosApp IS NULL THEN
                       iosAppId := uuid_generate_v4();
                       INSERT INTO mobile_app(id, created_time, tenant_id, pkg_name, platform_type, status)
                       VALUES (iosAppId, (extract(epoch from now()) * 1000), wlRecord.tenant_id, pkgName, 'IOS', 'DRAFT');
                       IF generatedBundleId IS NULL THEN
                           generatedBundleId := uuid_generate_v4();
                           INSERT INTO mobile_app_bundle(id, created_time, tenant_id, title, ios_app_id, self_registration_config, terms_of_use, privacy_policy)
                           VALUES (generatedBundleId, (extract(epoch from now()) * 1000), wlRecord.tenant_id, 'Autogenerated for '|| pkgName, iosAppId, wlRecord.settings,
                                   (SELECT settings FROM white_labeling WHERE type = 'TERMS_OF_USE' AND tenant_id = wlRecord.tenant_id),
                                   (SELECT settings FROM white_labeling WHERE type = 'PRIVACY_POLICY' AND tenant_id = wlRecord.tenant_id));
                       ELSE
                           UPDATE mobile_app_bundle SET ios_app_id = iosAppId, self_registration_config = wlRecord.settings,
                                                        terms_of_use = (SELECT settings FROM white_labeling WHERE type = 'TERMS_OF_USE' AND tenant_id = wlRecord.tenant_id),
                                                        privacy_policy = (SELECT settings FROM white_labeling WHERE type = 'PRIVACY_POLICY' AND tenant_id = wlRecord.tenant_id)
                                                    WHERE id = generatedBundleId;
                       END IF;
                   ELSE IF iosApp.tenant_id != '13814000-1dd2-11b2-8080-808080808080' THEN
                       UPDATE mobile_app_bundle SET self_registration_config = wlRecord.settings,
                                                    terms_of_use = (SELECT settings FROM white_labeling WHERE type = 'TERMS_OF_USE' AND tenant_id = wlRecord.tenant_id),
                                                    privacy_policy = (SELECT settings FROM white_labeling WHERE type = 'PRIVACY_POLICY' AND tenant_id = wlRecord.tenant_id)
                                                WHERE ios_app_id = iosAppId;
                       END IF;
                   END IF;
               END IF;
       END LOOP;
    END;
$$;

-- convert WEB self-registration settings to new structure
DO
$$
    BEGIN
        -- in case of running the upgrade script a second time
        IF (SELECT settings::jsonb -> 'captcha' FROM white_labeling WHERE white_labeling.type = 'SELF_REGISTRATION' LIMIT 1) IS NULL THEN
            UPDATE white_labeling SET settings = json_build_object(
                    'type', 'WEB',
                    'title', settings::jsonb ->> 'signUpTextMessage',
                    'signUpFields', jsonb_build_array(json_build_object('id', 'EMAIL', 'label', 'Email', 'required', true),
                                                      json_build_object('id', 'FIRST_NAME', 'label', 'First name', 'required', false),
                                                      json_build_object('id', 'LAST_NAME', 'label', 'Last name', 'required', false),
                                                      json_build_object('id', 'PASSWORD', 'label', 'Create password', 'required',true),
                                                      json_build_object('id', 'REPEAT_PASSWORD', 'Repeat your password', 'Email', 'required',true)),
                    'domain', settings::jsonb ->> 'domain',
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
                    'type', 'MOBILE',
                    'title', self_registration_config::jsonb ->> 'signUpTextMessage',
                    'signUpFields', jsonb_build_array(json_build_object('id', 'EMAIL', 'label', 'Email', 'required', true),
                                                      json_build_object('id', 'FIRST_NAME', 'label', 'First name', 'required', false),
                                                      json_build_object('id', 'LAST_NAME', 'label', 'Last name', 'required', false),
                                                      json_build_object('id', 'PASSWORD', 'label', 'Create password', 'required',true),
                                                      json_build_object('id', 'REPEAT_PASSWORD', 'Repeat your password', 'Email', 'required',true)),
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