import { Component, OnInit, Input } from '@angular/core';
import { FormGroup } from '@angular/forms';
import { Observable } from 'rxjs';
import { handlerConfigurationTypes, tcpBinaryByteOrder, tcpTextMessageSeparator } from '../../integartion-forms-templates';


@Component({
  selector: 'tb-tcp-integration-form',
  templateUrl: './tcp-integration-form.component.html',
  styleUrls: ['./tcp-integration-form.component.scss']
})
export class TcpIntegrationFormComponent implements OnInit {

  @Input() isLoading$: Observable<boolean>;
  @Input() isEdit: boolean;
  @Input() form: FormGroup;

  handlerConfigurationTypes = handlerConfigurationTypes;
  handlerTypes = handlerConfigurationTypes;
  tcpBinaryByteOrder = tcpBinaryByteOrder;
  tcpTextMessageSeparator = tcpTextMessageSeparator;

  constructor() { }

  ngOnInit(): void {
    delete this.handlerTypes.hex;
  }

  handlerConfigurationTypeChanged = () => {
  /*  let handlerType = scope.configuration.clientConfiguration.handlerConfiguration.handlerType;
    scope.configuration.clientConfiguration.handlerConfiguration = {};
    scope.configuration.clientConfiguration.handlerConfiguration = angular.copy(defaultHandlerConfigurations[handlerType]);*/
};

}
