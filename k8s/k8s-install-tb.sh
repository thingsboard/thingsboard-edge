#!/bin/bash
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

function installTb() {

    loadDemo=$1

    kubectl apply -f common/tb-node-configmap.yml
    kubectl apply -f common/database-setup.yml &&
    kubectl wait --for=condition=Ready pod/tb-db-setup --timeout=120s &&
    kubectl exec tb-db-setup -- sh -c 'export INSTALL_TB=true; export LOAD_DEMO='"$loadDemo"'; start-tb-node.sh; touch /tmp/install-finished;'

    kubectl delete pod tb-db-setup

}

function installPostgres() {

    kubectl apply -f common/postgres.yml
    kubectl apply -f common/tb-node-postgres-configmap.yml

    kubectl rollout status deployment/postgres
}

function installHybrid() {

    kubectl apply -f common/postgres.yml
    kubectl apply -f common/cassandra.yml
    kubectl apply -f common/tb-node-hybrid-configmap.yml

    kubectl rollout status deployment/postgres
    kubectl rollout status statefulset/cassandra

    kubectl exec -it cassandra-0 -- bash -c "cqlsh -e \
                    \"CREATE KEYSPACE IF NOT EXISTS thingsboard \
                    WITH replication = { \
                        'class' : 'SimpleStrategy', \
                        'replication_factor' : 1 \
                    };\""
}

while [[ $# -gt 0 ]]
do
key="$1"

case $key in
    --loadDemo)
    LOAD_DEMO=true
    shift # past argument
    ;;
    *)
            # unknown option
    ;;
esac
shift # past argument or value
done

if [ "$LOAD_DEMO" == "true" ]; then
    loadDemo=true
else
    loadDemo=false
fi

source .env

kubectl apply -f common/tb-namespace.yml
kubectl config set-context $(kubectl config current-context) --namespace=thingsboard

case $DEPLOYMENT_TYPE in
        basic)
        ;;
        high-availability)
        ;;
        *)
        echo "Unknown DEPLOYMENT_TYPE value specified: '${DEPLOYMENT_TYPE}'. Should be either basic or high-availability." >&2
        exit 1
esac

case $DATABASE in
        postgres)
            installPostgres
            installTb ${loadDemo}
        ;;
        hybrid)
            installHybrid
            installTb ${loadDemo}
        ;;
        *)
        echo "Unknown DATABASE value specified: '${DATABASE}'. Should be either postgres or hybrid." >&2
        exit 1
esac

