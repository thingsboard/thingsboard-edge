import integrationTcpIpTemplate from './integration-tcpip.tpl.html';

export default function IntegrationTcpIpDirective($compile, $templateCache, $translate, $mdExpansionPanel, types) {

    var linker = function (scope, element, attrs, ngModelCtrl) {
        var template = $templateCache.get(integrationTcpIpTemplate);
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
                scope.configuration.port = 10560;
                scope.configuration.soBroadcast = true;
                scope.configuration.handlerConfiguration.handlerType = types.handlerConfigurationTypes.text.value;
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
