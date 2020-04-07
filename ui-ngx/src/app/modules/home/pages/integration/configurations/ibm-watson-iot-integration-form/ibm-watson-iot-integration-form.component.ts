import { Component, OnInit, Input } from '@angular/core';
import { FormGroup } from '@angular/forms';
import { Observable } from 'rxjs';


@Component({
  selector: 'tb-ibm-watson-iot-integration-form',
  templateUrl: './ibm-watson-iot-integration-form.component.html',
  styleUrls: ['./ibm-watson-iot-integration-form.component.scss']
})
export class IbmWatsonIotIntegrationFormComponent implements OnInit {

  @Input() isLoading$: Observable<boolean>;
  @Input() isEdit: boolean;
  @Input() form: FormGroup;


  constructor() { }

  ngOnInit(): void {
  }

}
