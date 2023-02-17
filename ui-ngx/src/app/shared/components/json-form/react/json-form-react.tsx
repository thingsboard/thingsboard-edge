/*
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2023 ThingsBoard, Inc. All Rights Reserved.
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
import * as React from 'react';
import { createTheme, ThemeProvider } from '@material-ui/core/styles';
import createThingsboardTheme from './styles/thingsboardTheme';
import ThingsboardSchemaForm from './json-form-schema-form';
import { JsonFormProps } from './json-form.models';

class ReactSchemaForm extends React.Component<JsonFormProps, {}> {

  static defaultProps: JsonFormProps;

  constructor(props) {
    super(props);
  }

  render() {
    if (this.props.form.length > 0) {
      return <ThemeProvider
        theme={createTheme(createThingsboardTheme(this.props.primaryPalette, this.props.accentPalette))}>
        <ThingsboardSchemaForm {...this.props} /></ThemeProvider>;
    } else {
      return <div></div>;
    }
  }
}

ReactSchemaForm.defaultProps = {
  isFullscreen: false,
  schema: {},
  form: ['*'],
  groupInfoes: [],
  option: {
    formDefaults: {
      startEmpty: true
    }
  }
};

export default ReactSchemaForm;
