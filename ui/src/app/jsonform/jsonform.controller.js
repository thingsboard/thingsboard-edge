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
import './jsonform.scss';

/*@ngInject*/
export default function JsonFormController($scope/*, $rootScope, $log*/) {

    var vm = this;

    vm.pretty = pretty;
    vm.resetModel = resetModel;
    vm.itParses     = true;
    vm.itParsesForm = true;

    vm.formJson = "[   \n" +
        "    {\n" +
        "        \"key\": \"name\",\n" +
        "\t\"type\": \"text\"        \n" +
        "    },\n" +
        "    {\n" +
        "\t\"key\": \"name2\",\n" +
        "\t\"type\": \"color\"\n" +
        "    },\n" +
        "    {\n" +
        "\t\"key\": \"name3\",\n" +
        "\t\"type\": \"javascript\"\n" +
        "    },    \n" +
            "\t\"name4\"\n" +
        "]";
    vm.schemaJson = "{\n" +
        "    \"type\": \"object\",\n" +
        "    \"title\": \"Comment\",\n" +
        "    \"properties\": {\n" +
        "        \"name\": {\n" +
        "            \"title\": \"Name 1\",\n" +
        "            \"type\": \"string\"\n" +
        "         },\n" +
        "        \"name2\": {\n" +
        "            \"title\": \"Name 2\",\n" +
        "            \"type\": \"string\"\n" +
        "         },\n" +
        "        \"name3\": {\n" +
        "            \"title\": \"Name 3\",\n" +
        "            \"type\": \"string\"\n" +
        "         },\n" +
        "        \"name4\": {\n" +
        "            \"title\": \"Name 4\",\n" +
        "            \"type\": \"number\"\n" +
        "         }\n" +
        "     },\n" +
        "     \"required\": [\n" +
        "         \"name1\", \"name2\", \"name3\", \"name4\"\n" +
        "     ]\n" +
        "}";
/*        '{\n'+
    '    "type": "object",\n'+
    '    "title": "Comment",\n'+
    '    "properties": {\n'+
    '        "name": {\n'+
    '            "title": "Name",\n'+
    '            "type": "string"\n'+
    '         }\n'+
    '     },\n'+
    '     "required": [\n'+
    '         "name"\n'+
    '     ]\n'+
    '}';*/

    vm.schema = angular.fromJson(vm.schemaJson);
    vm.form = angular.fromJson(vm.formJson);
    vm.model = { name: '#ccc' };

    $scope.$watch('vm.schemaJson',function(val,old){
        if (val && val !== old) {
            try {
                vm.schema = angular.fromJson(vm.schemaJson);
                vm.itParses = true;
            } catch (e){
                vm.itParses = false;
            }
        }
    });


    $scope.$watch('vm.formJson',function(val,old){
        if (val && val !== old) {
            try {
                vm.form = angular.fromJson(vm.formJson);
                vm.itParsesForm = true;
            } catch (e){
                vm.itParsesForm = false;
            }
        }
    });

    function pretty (){
        return angular.isString(vm.model) ? vm.model : angular.toJson(vm.model, true);
    }

    function resetModel () {
        $scope.ngform.$setPristine();
        vm.model = { name: 'New hello world!' };
    }
}
