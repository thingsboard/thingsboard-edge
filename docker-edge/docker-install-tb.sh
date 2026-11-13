#!/bin/bash
#
# SPDX-FileCopyrightText: Copyright The Thingsboard Authors
# SPDX-License-Identifier: Apache-2.0
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
