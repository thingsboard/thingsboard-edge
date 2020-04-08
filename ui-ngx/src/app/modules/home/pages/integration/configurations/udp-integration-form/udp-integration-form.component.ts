import { Component, OnInit, Input } from '@angular/core';
import { FormGroup } from '@angular/forms';
import { Observable } from 'rxjs';
import { handlerConfigurationTypes } from '../../integartion-forms-templates';


@Component({
  selector: 'tb-udp-integration-form',
  templateUrl: './udp-integration-form.component.html',
  styleUrls: ['./udp-integration-form.component.scss']
})
export class UdpIntegrationFormComponent implements OnInit {

  @Input() isLoading$: Observable<boolean>;
  @Input() isEdit: boolean;
  @Input() form: FormGroup;

  handlerConfigurationTypes = handlerConfigurationTypes;


  constructor() { }

  ngOnInit(): void {
  }

  handlerConfigurationTypeChanged = () => {
   /* let handlerType = scope.configuration.clientConfiguration.handlerConfiguration.handlerType;
    scope.configuration.clientConfiguration.handlerConfiguration = {};
    scope.configuration.clientConfiguration.handlerConfiguration = angular.copy(defaultHandlerConfigurations[handlerType]);*/
};

}
