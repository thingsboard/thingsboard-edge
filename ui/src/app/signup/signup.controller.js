/*
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2019 ThingsBoard, Inc. All Rights Reserved.
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
import privacuPolicyTemplate from './privacy-policy-dialog.tpl.html'

/*@ngInject*/
export default function SignUpController($state, $mdDialog, $translate, toast, signUpService, loginService, $rootScope,
                                         $document) {
    var vm = this;

    vm.signupRequest = {
        firstName: '',
        lastName: '',
        email: '',
        password: ''
    };

    vm.signUp = signUp;
    vm.login = login;
    vm.openPrivacyPolicy = openPrivacyPolicy;

    vm.signupParams = $rootScope.signUpParams;

    function doSignUp() {
        if (validateSignUpRequest()) {
            signUpService.signup(vm.signupRequest).then(function success(response) {
                if (response.data && response.data === 'INACTIVE_USER_EXISTS') {
                    promptToResendEmailVerification();
                } else {
                    $state.go('signup.emailVerification', { email: vm.signupRequest.email })
                }
            }, function fail() {
            });
        }
    }

    function promptToResendEmailVerification() {
        var dialog = $mdDialog.confirm()
            .title($translate.instant('signup.inactive-user-exists-title'))
            .htmlContent($translate.instant('signup.inactive-user-exists-text'))
            .ariaLabel($translate.instant('signup.inactive-user-exists-title'))
            .cancel($translate.instant('action.cancel'))
            .ok($translate.instant('signup.resend'));
        $mdDialog.show(dialog).then(function () {
            loginService.resendEmailActivation(vm.signupRequest.email).then(function success() {
                $state.go('signup.emailVerification', {email: vm.signupRequest.email });
            }, function fail() {
            });
        }, function () {
        });
    }

    function validateSignUpRequest() {
        if (vm.passwordCheck !== vm.signupRequest.password) {
            toast.showError($translate.instant('login.passwords-mismatch-error'));
            return false;
        }
        if (vm.signupRequest.password.length < 6) {
            toast.showError($translate.instant('signup.password-length-message'));
            return false;
        }
        if (angular.isUndefined(vm.signupRequest.recaptchaResponse) || vm.signupRequest.recaptchaResponse.length < 1) {
            toast.showError($translate.instant('signup.no-captcha-message'));
            return false;
        }
        if (!vm.acceptPrivacyPolicy) {
            toast.showError("You must accept our Privacy Policy");
            return false;
        }
        return true;
    }

    function openPrivacyPolicy($event) {
        if ($event) {
            $event.stopPropagation();
        }
        $mdDialog.show({
            controller: showPrivacyPolicyController,
            controllerAs: 'vm',
            templateUrl: privacuPolicyTemplate,
            parent: angular.element($document[0].body),
            clickOutsideToClose:true,
            fullscreen: true,
            targetEvent: $event
        }).then(function () {
            vm.acceptPrivacyPolicy = true;
        }, function () {
        });
    }

    function signUp() {
        doSignUp();
    }

    function login() {
        $state.go('login')
    }
}

/*@ngInject*/
function showPrivacyPolicyController($mdDialog, selfRegistrationService, $sce) {

    var vm = this;

    selfRegistrationService.loadPrivacyPolicy().then(function success(data) {
        vm.privacyPolicyText = $sce.trustAsHtml(data);
    });

    vm.accept = accept;
    vm.cancel = cancel;

    function cancel() {
        $mdDialog.cancel();
    }

    function accept() {
        $mdDialog.hide();
    }
}
