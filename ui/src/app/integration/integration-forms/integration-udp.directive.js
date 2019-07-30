import integrationUdpTemplate from './integration-udp.tpl.html';

/*@ngInject*/
export default function IntegrationUdpDirective($compile, $templateCache, $translate, $mdExpansionPanel, types) {

    var linker = function (scope, element, attrs, ngModelCtrl) {
        var template = $templateCache.get(integrationUdpTemplate);
        element.html(template);

        scope.types = types;
        scope.$mdExpansionPanel = $mdExpansionPanel;

        scope.$watch('configuration', function (newConfiguration, oldConfiguration) {
            if (!angular.equals(newConfiguration, oldConfiguration)) {
                ngModelCtrl.$setViewValue(scope.configuration);
            }
        });

        ngModelCtrl.$render = function () {
            scope.configuration = ngModelCtrl.$viewValue;
            setupUdpConfiguration();
        };

        function setupUdpConfiguration() {
            if (!scope.configuration) {
                scope.configuration.port = 11560;
                scope.configuration.soBroadcast = true;
                scope.configuration.soRcvBuf = 128;
                scope.configuration.handlerConfiguration.handlerType = types.handlerConfigurationTypes.binary.value;
            }
        }

        scope.handlerConfigurationTypeChanged = () => {
            var handlerType = scope.configuration.handlerConfiguration.handlerType;
            scope.configuration.handlerConfiguration = {};
            scope.configuration.handlerConfiguration.handlerType = handlerType;
        };

        $compile(element.contents())(scope);
    };

    return {
        restrict: "E",
        require: "^ngModel",
        scope: {
            isEdit: '=',
            integrationType: '='
        },
        link: linker
    };
}
