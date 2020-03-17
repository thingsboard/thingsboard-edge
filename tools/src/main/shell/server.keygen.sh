#!/bin/bash
#
# ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
#
# Copyright © 2016-2020 ThingsBoard, Inc. All Rights Reserved.
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

usage() {
    echo "This script generates thingsboard server's ssl certificate"
    echo "and optionally copies it to the server's resource directory."
    echo "usage: ./server.keygen.sh [-c flag] [-d directory] [-p file]"
    echo "    -c | --copy flag                  Specifies if the keystore should be copied to the server directory. Defaults to true"
    echo "    -d | --dir directory              Server keystore directory, where the generated keystore file will be copied. If specified, overrides the value from the properties file"
    echo "                                      Default value is SERVER_KEYSTORE_DIR property from properties file"
    echo "    -p | --props | --properties file  Properties file. default value is ./keygen.properties"
	echo "    -h | --help | ?                   Show this message"
}

COPY=true;
COPY_DIR=
PROPERTIES_FILE=keygen.properties

while true; do
  case "$1" in
    -c | --copy)                  COPY=$2 ;
                                  shift
                                  ;;
    -d | --dir | --directory )    COPY_DIR=$2 ;
                                  shift
                                  ;;
    -p | --props | --properties ) PROPERTIES_FILE=$2 ;
                                  shift
                                  ;;
    -- )                          shift;
                                  break
                                  ;;
    "" )                          break
                                  ;;

    -h | --help | ? | *)          usage
                                  exit 0
                                  ;;
  esac
  shift
done

if [[ "$COPY" != true ]] && [[ "$COPY" != false ]]; then
   usage
fi

. $PROPERTIES_FILE

if [ -f $SERVER_FILE_PREFIX.jks ] || [ -f $SERVER_FILE_PREFIX.cer ] || [ -f $SERVER_FILE_PREFIX.pub.pem ] || [ -f $SERVER_FILE_PREFIX.pub.der ];
then
while :
   do
       read -p "Output files from previous server.keygen.sh script run found. Overwrite?[yes]" response
       case $response in
        [nN]|[nN][oO])
            echo "Skipping"
            echo "Done"
            exit 0
            ;;
        [yY]|[yY][eE]|[yY][eE]|[sS]|[yY]|"")
            echo "Cleaning up files"
            rm -rf $SERVER_FILE_PREFIX.jks
            rm -rf $SERVER_FILE_PREFIX.pub.pem
            rm -rf $SERVER_FILE_PREFIX.cer
            break;
            ;;
        *)  echo "Please reply 'yes' or 'no'"
            ;;
        esac
    done
fi

echo "Generating SSL Key Pair..."

keytool -genkeypair -v \
  -alias $SERVER_KEY_ALIAS \
  -dname "CN=$DOMAIN_SUFFIX, OU=$ORGANIZATIONAL_UNIT, O=$ORGANIZATION, L=$CITY, ST=$STATE_OR_PROVINCE, C=$TWO_LETTER_COUNTRY_CODE" \
  -keystore $SERVER_FILE_PREFIX.jks \
  -keypass $SERVER_KEY_PASSWORD \
  -storepass $SERVER_KEYSTORE_PASSWORD \
  -keyalg RSA \
  -keysize 2048 \
  -validity 9999

status=$?
if [[ $status != 0 ]]; then
    exit $status;
fi

keytool -export \
  -alias $SERVER_KEY_ALIAS \
  -keystore $SERVER_FILE_PREFIX.jks \
  -file $SERVER_FILE_PREFIX.pub.pem -rfc \
  -storepass $SERVER_KEYSTORE_PASSWORD

keytool -export \
  -alias $SERVER_KEY_ALIAS \
  -file $SERVER_FILE_PREFIX.cer \
  -keystore $SERVER_FILE_PREFIX.jks \
  -storepass $SERVER_KEYSTORE_PASSWORD \
  -keypass $SERVER_KEY_PASSWORD

status=$?
if [[ $status != 0 ]]; then
    exit $status;
fi


if [[ $COPY = true ]]; then
    if [[ -z "$COPY_DIR" ]]; then
        while :
        do
            read -p  "Do you want to copy $SERVER_FILE_PREFIX.jks to server directory? [Y/N]: " yn
            case $yn in
                [nN]|[nN][oO])
                    break
                    ;;
                [yY]|[yY][eE]|[yY][eE]|[sS]|[yY]|"")
                    read -p "(Default: $SERVER_KEYSTORE_DIR): " dir
                     if [[ !  -z  $dir  ]]; then
                        DESTINATION=$dir;
                     else
                        DESTINATION=$SERVER_KEYSTORE_DIR
                     fi;
                     break;;
                *)  echo "Please reply 'yes' or 'no'"
                    ;;
             esac
         done
    else
        DESTINATION=$COPY_DIR
    fi
    echo "*** DEST: $DESTINATION"
    if [[ -n $DESTINATION ]]; then
        mkdir -p $DESTINATION
        cp $SERVER_FILE_PREFIX.jks $DESTINATION
        if [ $? -ne 0 ]; then
            echo "Failed to copy keystore file."
        else
            echo "File copied successfully."
        fi
    fi
fi
echo "Done."