import { Component, OnInit, Input } from '@angular/core';
import { FormGroup } from '@angular/forms';

@Component({
  selector: 'tb-cert-upload',
  templateUrl: './cert-upload.component.html',
  styleUrls: ['./cert-upload.component.scss']
})
export class CertUploadComponent implements OnInit {

  @Input() form: FormGroup;

  constructor() { }

  ngOnInit(): void {
  }

}
