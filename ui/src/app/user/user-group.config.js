/*
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2020 ThingsBoard, Inc. All Rights Reserved.
 *
 * NOTICE: All information contained herein is, and remains
 * the property of ThingsBoard, Inc. and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to ThingsBoard, Inc.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 *
 * Dissemination of this information or reproduction of this material is strictly forbidden
 * unless prior written permission is obtained from COMPANY.
 *
 * Access to the source code contained herein is hereby forbidden to anyone except current COMPANY employees,
 * managers or contractors who have executed Confidentiality and Non-disclosure agreements
 * explicitly covering such access.
 *
 * The copyright notice above does not evidence any actual or intended publication
 * or disclosure  of  this source code, which includes
 * information that is confidential and/or proprietary, and is a trade secret, of  COMPANY.
 * ANY REPRODUCTION, MODIFICATION, DISTRIBUTION, PUBLIC  PERFORMANCE,
 * OR PUBLIC DISPLAY OF OR THROUGH USE  OF THIS  SOURCE CODE  WITHOUT
 * THE EXPRESS WRITTEN CONSENT OF COMPANY IS STRICTLY PROHIBITED,
 * AND IN VIOLATION OF APPLICABLE LAWS AND INTERNATIONAL TREATIES.
 * THE RECEIPT OR POSSESSION OF THIS SOURCE CODE AND/OR RELATED INFORMATION
 * DOES NOT CONVEY OR IMPLY ANY RIGHTS TO REPRODUCE, DISCLOSE OR DISTRIBUTE ITS CONTENTS,
 * OR TO MANUFACTURE, USE, OR SELL ANYTHING THAT IT  MAY DESCRIBE, IN WHOLE OR IN PART.
 */
import activationLinkDialogTemplate from './activation-link.dialog.tpl.html';
import addUserTemplate from './add-user.tpl.html';

/*@ngInject*/
export default function UserGroupConfig($q, $translate, $document, $mdDialog, tbDialogs, utils, types, securityTypes, toast,
                                        userService, userPermissionsService) {

    var service = {
        createConfig: createConfig
    }

    return service;

    function createConfig(params, entityGroup) {
        var deferred = $q.defer();

        var settings = utils.groupSettingsDefaults(types.entityType.user, entityGroup.configuration.settings);

        var groupConfig = {

            tableTitle: entityGroup.name + ': ' + $translate.instant('user.users'),

            loadEntity: (entityId) => {return userService.getUser(entityId)},
            saveEntity: (entity) => {return userService.saveUser(entity)},
            deleteEntity: (entityId) => {return userService.deleteUser(entityId)},

            addEnabled: () => {
                return settings.enableAdd;
            },

            detailsReadOnly: () => {
                return false;
            },
            assignmentEnabled: () => {
                return settings.enableAssignment;
            },
            manageCredentialsEnabled: () => {
                return settings.enableCredentialsManagement;
            },
            deleteEnabled: () => {
                return settings.enableDelete;
            },
            entitiesDeleteEnabled: () => {
                return settings.enableDelete;
            },
            deleteEntityTitle: (entity) => {
                return $translate.instant('user.delete-user-title', {userEmail: entity.email});
            },
            deleteEntityContent: (/*entity*/) => {
                return $translate.instant('user.delete-user-text');
            },
            deleteEntitiesTitle: (count) => {
                return $translate.instant('user.delete-users-title', {count: count}, 'messageformat');
            },
            deleteEntitiesContent: (/*count*/) => {
                return $translate.instant('user.delete-users-text');
            }
        };

        groupConfig.onDisplayActivationLink = (event, user) => {
            userService.getActivationLink(user.id.id).then(
                function success(activationLink) {
                    openActivationLinkDialog(event, activationLink);
                }
            );
        };

        groupConfig.onResendActivation = (user) => {
            userService.sendActivationEmail(user.email).then(function success() {
                toast.showSuccess($translate.instant('user.activation-email-sent-message'));
            });
        };

        groupConfig.onLoginAsUser = (event, user) => {
            if (event) {
                event.stopPropagation();
            }
            userService.loginAsUser(user.id.id);
        };

        groupConfig.onSetUserCredentialsEnabled = (event, user, userCredentialsEnabled) => {
            if (event) {
                event.stopPropagation();
            }
            userService.setUserCredentialsEnabled(user.id.id, userCredentialsEnabled).then(
                () => {
                    if (!user.additionalInfo) {
                        user.additionalInfo = {};
                    }
                    user.additionalInfo.userCredentialsEnabled = userCredentialsEnabled;
                    if (userCredentialsEnabled) {
                        toast.showSuccess($translate.instant('user.enable-account-message'));
                    } else {
                        toast.showSuccess($translate.instant('user.disable-account-message'));
                    }
                }
            )
        };

        groupConfig.addEntity = (event, entityGroup) => {
            return addUser(event, entityGroup);
        };

        groupConfig.actionCellDescriptors = [];

        if (userService.isUserTokenAccessEnabled() &&
            userPermissionsService.hasGenericPermission(securityTypes.resource.user, securityTypes.operation.impersonate)) {
            var isTenantAdmins = entityGroup.ownerId.entityType === types.entityType.tenant;
            groupConfig.actionCellDescriptors.push(
                {
                    name: $translate.instant(isTenantAdmins ? 'user.login-as-tenant-admin' : 'user.login-as-customer-user'),
                    icon: 'login',
                    isNgIcon: true,
                    isEnabled: () => {
                        return settings.enableLoginAsUser;
                    },
                    onAction: ($event, entity) => {
                        groupConfig.onLoginAsUser($event, entity);
                    }
                }
            );
        }

        utils.groupConfigDefaults(groupConfig);

        deferred.resolve(groupConfig);
        return deferred.promise;
    }

    function openActivationLinkDialog(event, activationLink) {
        $mdDialog.show({
            controller: 'ActivationLinkDialogController',
            controllerAs: 'vm',
            templateUrl: activationLinkDialogTemplate,
            locals: {
                activationLink: activationLink
            },
            parent: angular.element($document[0].body),
            fullscreen: true,
            multiple: true,
            targetEvent: event
        });
    }

    function addUser($event, entityGroup) {
        return $mdDialog.show({
            controller: 'AddGroupUserController',
            controllerAs: 'vm',
            templateUrl: addUserTemplate,
            parent: angular.element($document[0].body),
            locals: {
                entityGroup: entityGroup
            },
            fullscreen: true,
            targetEvent: $event
        });
    }


}