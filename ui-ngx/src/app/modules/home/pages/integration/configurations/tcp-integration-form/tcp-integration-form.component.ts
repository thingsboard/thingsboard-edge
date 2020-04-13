import { Component, Input, OnInit } from '@angular/core';
import { FormGroup } from '@angular/forms';
import {
  handlerConfigurationTypes,
  tcpBinaryByteOrder,
  tcpTextMessageSeparator
} from '../../integration-forms-templates';


@Component({
  selector: 'tb-tcp-integration-form',
  templateUrl: './tcp-integration-form.component.html',
  styleUrls: ['./tcp-integration-form.component.scss']
})
export class TcpIntegrationFormComponent implements OnInit {


  @Input() form: FormGroup;

  handlerConfigurationTypes = handlerConfigurationTypes;
  handlerTypes = handlerConfigurationTypes;
  tcpBinaryByteOrder = tcpBinaryByteOrder;
  tcpTextMessageSeparator = tcpTextMessageSeparator;

  defaultHandlerConfigurations = {
    [handlerConfigurationTypes.binary.value]: {
      handlerType: handlerConfigurationTypes.binary.value,
      byteOrder: tcpBinaryByteOrder.littleEndian.value,
      maxFrameLength: 128,
      lengthFieldOffset: 0,
      lengthFieldLength: 2,
      lengthAdjustment: 0,
      initialBytesToStrip: 0,
      failFast: false
    }, [handlerConfigurationTypes.text.value]: {
      handlerType: handlerConfigurationTypes.text.value,
      maxFrameLength: 128,
      stripDelimiter: true,
      messageSeparator: tcpTextMessageSeparator.systemLineSeparator.value
    },
    [handlerConfigurationTypes.json.value]: {
      handlerType: handlerConfigurationTypes.json.value
    }
  }

  constructor() { }

  ngOnInit(): void {
    delete this.handlerTypes.hex;
  }

  handlerConfigurationTypeChanged(type){
    this.form.get('handlerConfiguration').patchValue(this.defaultHandlerConfigurations[type.value]);
  };

}
