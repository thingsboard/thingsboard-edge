#!/bin/bash
#
# Copyright © 2016-2025 The Thingsboard Authors
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

set -e

source compose-utils.sh

COMPOSE_VERSION=$(composeVersion) || exit $?

ADDITIONAL_COMPOSE_ARGS=$(additionalComposeArgs) || exit $?

ADDITIONAL_CACHE_ARGS=$(additionalComposeCacheArgs) || exit $?

ADDITIONAL_STARTUP_SERVICES=$(additionalStartupServices) || exit $?

checkFolders --create || exit $?

if [ ! -z "${ADDITIONAL_STARTUP_SERVICES// }" ]; then

    COMPOSE_ARGS="\
          -f docker-compose.yml ${ADDITIONAL_CACHE_ARGS} ${ADDITIONAL_COMPOSE_ARGS} \
          up -d ${ADDITIONAL_STARTUP_SERVICES}"

    case $COMPOSE_VERSION in
        V2)
            docker compose $COMPOSE_ARGS
        ;;
        V1)
            docker-compose $COMPOSE_ARGS
        ;;
        *)
            # unknown option
        ;;
    esac
fi

COMPOSE_ARGS="\
      -f docker-compose.yml ${ADDITIONAL_CACHE_ARGS} ${ADDITIONAL_COMPOSE_ARGS} \
      run --no-deps --rm -e INSTALL_TB_EDGE=true\
      tb-edge1"

case $COMPOSE_VERSION in
    V2)
        docker compose $COMPOSE_ARGS
    ;;
    V1)
        docker-compose $COMPOSE_ARGS
    ;;
    *)
        # unknown option
    ;;
esac
