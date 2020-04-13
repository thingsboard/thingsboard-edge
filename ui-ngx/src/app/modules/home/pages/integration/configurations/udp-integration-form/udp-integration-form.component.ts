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


  @Input() form: FormGroup;

  handlerConfigurationTypes = handlerConfigurationTypes;

  defaultHandlerConfigurations = {
    [handlerConfigurationTypes.binary.value]: {
      handlerType: handlerConfigurationTypes.binary.value
    },
    [handlerConfigurationTypes.text.value]: {
      handlerType: handlerConfigurationTypes.text.value,
      charsetName: 'UTF-8'
    },
    [handlerConfigurationTypes.json.value]: {
      handlerType: handlerConfigurationTypes.json.value
    },
    [handlerConfigurationTypes.hex.value]: {
      handlerType: handlerConfigurationTypes.hex.value,
      maxFrameLength: 128
    },
  }


  constructor() { }

  ngOnInit(): void {
  }

  handlerConfigurationTypeChanged(type) {
    this.form.get('handlerConfiguration').patchValue(this.defaultHandlerConfigurations[type.value])
  };

}
