#!/bin/bash
#
# Thingsboard OÜ ("COMPANY") CONFIDENTIAL
#
# Copyright © 2016-2017 Thingsboard OÜ. All Rights Reserved.
#
# NOTICE: All information contained herein is, and remains
# the property of Thingsboard OÜ and its suppliers,
# if any.  The intellectual and technical concepts contained
# herein are proprietary to Thingsboard OÜ
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


dpkg -i /thingsboard.deb

if [ "$DATABASE_TYPE" == "cassandra" ]; then
    until nmap $CASSANDRA_HOST -p $CASSANDRA_PORT | grep "$CASSANDRA_PORT/tcp open\|filtered"
    do
      echo "Wait for cassandra db to start..."
      sleep 10
    done
fi

if [ "$DATABASE_TYPE" == "sql" ]; then
    if [ "$SPRING_DRIVER_CLASS_NAME" == "org.postgresql.Driver" ]; then
        until nmap $POSTGRES_HOST -p $POSTGRES_PORT | grep "$POSTGRES_PORT/tcp open"
        do
          echo "Waiting for postgres db to start..."
          sleep 10
        done
    fi
fi

if [ "$ADD_SCHEMA_AND_SYSTEM_DATA" == "true" ]; then
    echo "Creating 'Thingsboard' schema and system data..."
    if [ "$ADD_DEMO_DATA" == "true" ]; then
        echo "plus demo data..."
        /usr/share/thingsboard/bin/install/install.sh --loadDemo
    elif [ "$ADD_DEMO_DATA" == "false" ]; then
        /usr/share/thingsboard/bin/install/install.sh
    fi
fi


# Copying env variables into conf files
printenv | awk -F "=" '{print "export " $1 "='\''" $2 "'\''"}' >> /usr/share/thingsboard/conf/thingsboard.conf

cat /usr/share/thingsboard/conf/thingsboard.conf

echo "Starting 'Thingsboard' service..."
service thingsboard start

# Wait until log file is created
sleep 10
tail -f /var/log/thingsboard/thingsboard.log
