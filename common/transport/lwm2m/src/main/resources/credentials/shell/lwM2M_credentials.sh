#!/bin/sh
#
# ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
#
# Copyright © 2016-2021 ThingsBoard, Inc. All Rights Reserved.
#
# NOTICE: All information contained herein is, and remains
# the property of ThingsBoard, Inc. and its suppliers,
# if any.  The intellectual and technical concepts contained
# herein are proprietary to ThingsBoard, Inc.
# and its suppliers and may be covered by U.S. and Foreign Patents,
# patents in process, and are protected by trade secret or copyright law.
#
# Dissemination of this information or reproduction of this material is strictly forbidden
# unless prior written permission is obtained from COMPANY.
#
# Access to the source code contained herein is hereby forbidden to anyone except current COMPANY employees,
# managers or contractors who have executed Confidentiality and Non-disclosure agreements
# explicitly covering such access.
#
# The copyright notice above does not evidence any actual or intended publication
# or disclosure  of  this source code, which includes
# information that is confidential and/or proprietary, and is a trade secret, of  COMPANY.
# ANY REPRODUCTION, MODIFICATION, DISTRIBUTION, PUBLIC  PERFORMANCE,
# OR PUBLIC DISPLAY OF OR THROUGH USE  OF THIS  SOURCE CODE  WITHOUT
# THE EXPRESS WRITTEN CONSENT OF COMPANY IS STRICTLY PROHIBITED,
# AND IN VIOLATION OF APPLICABLE LAWS AND INTERNATIONAL TREATIES.
# THE RECEIPT OR POSSESSION OF THIS SOURCE CODE AND/OR RELATED INFORMATION
# DOES NOT CONVEY OR IMPLY ANY RIGHTS TO REPRODUCE, DISCLOSE OR DISTRIBUTE ITS CONTENTS,
# OR TO MANUFACTURE, USE, OR SELL ANYTHING THAT IT  MAY DESCRIBE, IN WHOLE OR IN PART.
#

# source the properties:
. ./lwM2M_keygen.properties

# Generation of the keystore.
echo "${H0}====START========${RESET}"
echo "${H1}Server Keystore : ${RESET}"
echo "${H1}==================${RESET}"
echo "${H2}Creating the trusted root CA key and certificate...${RESET}"
# -keysize
#    1024 (when using -genkeypair)
keytool \
  -genkeypair \
  -alias $ROOT_KEY_ALIAS \
  -keyalg EC \
  -dname "CN=$ROOT_CN, OU=$ORGANIZATIONAL_UNIT, O=$ORGANIZATION, L=$CITY, ST=$STATE_OR_PROVINCE, C=$TWO_LETTER_COUNTRY_CODE" \
  -validity $VALIDITY \
  -storetype $STORETYPE \
  -keypass $SERVER_STORE_PWD \
  -keystore $SERVER_STORE \
  -storepass $SERVER_STORE_PWD

echo
echo "${H2}Creating server key and self-signed  certificate ...${RESET}"
keytool \
  -genkeypair \
  -alias $SERVER_ALIAS \
  -keyalg EC \
  -dname "CN=$SERVER_SELF_CN, OU=$ORGANIZATIONAL_UNIT, O=$ORGANIZATION, L=$CITY, ST=$STATE_OR_PROVINCE, C=$TWO_LETTER_COUNTRY_CODE" \
  -validity $VALIDITY \
  -storetype $STORETYPE \
  -keypass $SERVER_STORE_PWD \
  -keystore $SERVER_STORE \
  -storepass $SERVER_STORE_PWD
keytool \
  -exportcert \
  -alias $SERVER_ALIAS \
  -keystore $SERVER_STORE \
  -storepass $SERVER_STORE_PWD | \
  keytool \
    -importcert \
    -alias $SERVER_SELF_ALIAS \
    -keystore $SERVER_STORE \
    -storepass $SERVER_STORE_PWD \
    -noprompt

echo
echo "${H2}Creating server certificate signed by root CA...${RESET}"
keytool \
  -certreq \
  -alias $SERVER_ALIAS \
  -dname "CN=$SERVER_CN, OU=$ORGANIZATIONAL_UNIT, O=$ORGANIZATION, L=$CITY, ST=$STATE_OR_PROVINCE, C=$TWO_LETTER_COUNTRY_CODE" \
  -keystore $SERVER_STORE \
  -storepass $SERVER_STORE_PWD | \
  keytool \
    -gencert \
    -alias $ROOT_KEY_ALIAS \
    -keystore $SERVER_STORE \
    -storepass $SERVER_STORE_PWD \
    -storetype $STORETYPE \
    -validity $VALIDITY  | \
    keytool \
      -importcert \
      -alias $SERVER_ALIAS \
      -keystore $SERVER_STORE \
      -storepass $SERVER_STORE_PWD

echo
echo "${H2}Creating server key and self-signed  certificate ...${RESET}"
keytool \
  -genkeypair \
  -alias $BOOTSTRAP_ALIAS \
  -keyalg EC \
  -dname "CN=$BOOTSTRAP_SELF_CN, OU=$ORGANIZATIONAL_UNIT, O=$ORGANIZATION, L=$CITY, ST=$STATE_OR_PROVINCE, C=$TWO_LETTER_COUNTRY_CODE" \
  -validity $VALIDITY \
  -storetype $STORETYPE \
  -keypass $SERVER_STORE_PWD \
  -keystore $SERVER_STORE \
  -storepass $SERVER_STORE_PWD
keytool \
  -exportcert \
  -alias $BOOTSTRAP_ALIAS \
  -keystore $SERVER_STORE \
  -storepass $SERVER_STORE_PWD | \
  keytool \
    -importcert \
    -alias $BOOTSTRAP_SELF_ALIAS \
    -keystore $SERVER_STORE \
    -storepass $SERVER_STORE_PWD \
    -noprompt

echo
echo "${H2}Creating bootstrap certificate signed by root CA...${RESET}"
keytool \
  -certreq \
  -alias $BOOTSTRAP_ALIAS \
  -dname "CN=$BOOTSTRAP_CN, OU=$ORGANIZATIONAL_UNIT, O=$ORGANIZATION, L=$CITY, ST=$STATE_OR_PROVINCE, C=$TWO_LETTER_COUNTRY_CODE" \
  -keystore $SERVER_STORE \
  -storepass $SERVER_STORE_PWD | \
  keytool \
    -gencert \
    -alias $ROOT_KEY_ALIAS \
    -keystore $SERVER_STORE \
    -storepass $SERVER_STORE_PWD \
    -storetype $STORETYPE \
    -validity $VALIDITY  | \
    keytool \
      -importcert \
      -alias $BOOTSTRAP_ALIAS \
      -keystore $SERVER_STORE \
      -storepass $SERVER_STORE_PWD


echo
echo "${H1}Client Keystore : ${RESET}"
echo "${H1}==================${RESET}"
echo "${H2}Creating client key and self-signed certificate with expected CN...${RESET}"
keytool \
  -genkeypair \
  -alias $CLIENT_ALIAS \
  -keyalg EC \
  -dname "CN=$CLIENT_SELF_CN, OU=$ORGANIZATIONAL_UNIT, O=$ORGANIZATION, L=$CITY, ST=$STATE_OR_PROVINCE, C=$TWO_LETTER_COUNTRY_CODE" \
  -validity $VALIDITY \
  -storetype $STORETYPE \
  -keypass $CLIENT_STORE_PWD \
  -keystore $CLIENT_STORE \
  -storepass $CLIENT_STORE_PWD
keytool \
  -exportcert \
  -alias $CLIENT_ALIAS \
  -keystore $CLIENT_STORE \
  -storepass $CLIENT_STORE_PWD | \
  keytool \
    -importcert \
    -alias $CLIENT_SELF_ALIAS \
    -keystore $CLIENT_STORE \
    -storepass $CLIENT_STORE_PWD \
    -noprompt

echo
echo "${H2}Import root certificate just to be able to import  ned by root CA with expected CN...${RESET}"
keytool \
  -exportcert \
  -alias $ROOT_KEY_ALIAS \
  -keystore $SERVER_STORE \
  -storepass $SERVER_STORE_PWD | \
  keytool \
    -importcert \
    -alias $ROOT_KEY_ALIAS \
    -keystore $CLIENT_STORE \
    -storepass $CLIENT_STORE_PWD \
    -noprompt

echo
echo "${H2}Creating client certificate signed by root CA with expected CN...${RESET}"
keytool \
  -certreq \
  -alias $CLIENT_ALIAS \
  -dname "CN=$CLIENT_CN, OU=$ORGANIZATIONAL_UNIT, O=$ORGANIZATION, L=$CITY, ST=$STATE_OR_PROVINCE, C=$TWO_LETTER_COUNTRY_CODE" \
  -keystore $CLIENT_STORE \
  -storepass $CLIENT_STORE_PWD | \
  keytool \
    -gencert \
    -alias $ROOT_KEY_ALIAS \
    -keystore $SERVER_STORE \
    -storepass $SERVER_STORE_PWD \
    -storetype $STORETYPE \
    -validity $VALIDITY  | \
    keytool \
      -importcert \
      -alias $CLIENT_ALIAS \
      -keystore $CLIENT_STORE \
      -storepass $CLIENT_STORE_PWD \
      -noprompt

echo
echo "${H0}!!! Warning ${H2}Migrate ${H1}${SERVER_STORE} ${H2}to ${H1}PKCS12 ${H2}which is an industry standard format..${RESET}"
keytool \
  -importkeystore \
  -srckeystore $SERVER_STORE \
  -destkeystore $SERVER_STORE \
  -deststoretype pkcs12 \
  -srcstorepass $SERVER_STORE_PWD

echo
echo "${H0}!!! Warning ${H2}Migrate ${H1}${CLIENT_STORE} ${H2}to ${H1}PKCS12 ${H2}which is an industry standard format..${RESET}"
keytool \
  -importkeystore \
  -srckeystore $CLIENT_STORE \
  -destkeystore $CLIENT_STORE \
  -deststoretype pkcs12 \
  -srcstorepass $CLIENT_STORE_PWD
