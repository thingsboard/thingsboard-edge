#!/bin/bash
#
# ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
#
# Copyright © 2016-2025 ThingsBoard, Inc. All Rights Reserved.
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

set -e # exit on any error

#PROJECTS="msa/tb-node,msa/web-ui,rule-engine-pe/rule-node-twilio-sms"
PROJECTS=""

if [ "$1" ]; then
  PROJECTS="--projects $1"
fi

echo "Building and pushing [amd64,arm64] projects '$PROJECTS' ..."
echo "HELP: usage ./build.sh [projects]"
echo "HELP: example ./build.sh msa/web-ui,msa/web-report"
java -version
#echo "Cleaning ui-ngx/node_modules" && rm -rf ui-ngx/node_modules

MAVEN_OPTS="-Xmx1024m" NODE_OPTIONS="--max_old_space_size=4096" DOCKER_CLI_EXPERIMENTAL=enabled DOCKER_BUILDKIT=0 \
mvn -T2 license:format clean install -DskipTests \
  $PROJECTS --also-make
#   \
#  -Dpush-docker-amd-arm-images
#  -Ddockerfile.skip=false -Dpush-docker-image=true
#  --offline
#  --projects '!msa/web-report' --also-make

# push all
# mvn -T 1C license:format clean install -DskipTests -Ddockerfile.skip=false -Dpush-docker-image=true


## Build and push AMD and ARM docker images using docker buildx
## Reference to article how to setup docker miltiplatform build environment: https://medium.com/@artur.klauser/building-multi-architecture-docker-images-with-buildx-27d80f7e2408
## install docker-ce from docker repo https://docs.docker.com/engine/install/ubuntu/
# sudo apt install -y qemu-user-static binfmt-support
# export DOCKER_CLI_EXPERIMENTAL=enabled
# docker version
# docker run --rm --privileged multiarch/qemu-user-static --reset -p yes
# docker buildx create --name mybuilder
# docker buildx use mybuilder
# docker buildx inspect --bootstrap
# docker buildx ls
# mvn clean install -P push-docker-amd-arm-images