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

COMPOSE_ARGS_PULL="\
      -f docker-compose.yml ${ADDITIONAL_CACHE_ARGS} ${ADDITIONAL_COMPOSE_ARGS} \
      pull \
      tb-edge1"

COMPOSE_ARGS_UP="\
      -f docker-compose.yml ${ADDITIONAL_CACHE_ARGS} ${ADDITIONAL_COMPOSE_ARGS} \
      up -d ${ADDITIONAL_STARTUP_SERVICES}"

COMPOSE_ARGS_RUN="\
      -f docker-compose.yml ${ADDITIONAL_CACHE_ARGS} ${ADDITIONAL_COMPOSE_ARGS} \
      run --no-deps --rm -e UPGRADE_TB_EDGE=true \
      tb-edge1"

case $COMPOSE_VERSION in
    V2)
        docker compose $COMPOSE_ARGS_PULL
        docker compose $COMPOSE_ARGS_UP
        docker compose $COMPOSE_ARGS_RUN
    ;;
    V1)
        docker-compose $COMPOSE_ARGS_PULL
        docker-compose $COMPOSE_ARGS_UP
        docker-compose $COMPOSE_ARGS_RUN
    ;;
    *)
        # unknown option
    ;;
esac
