/*
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2024 ThingsBoard, Inc. All Rights Reserved.
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
import ThingsboardBaseComponent from './json-form-base-component';
import reactCSS from 'reactcss';
import tinycolor from 'tinycolor2';
import TextField from '@mui/material/TextField';
import { JsonFormFieldProps, JsonFormFieldState } from '@shared/components/json-form/react/json-form.models';
import IconButton from '@mui/material/IconButton';
import Clear from '@mui/icons-material/Clear';
import Tooltip from '@mui/material/Tooltip';

interface ThingsboardColorState extends JsonFormFieldState {
  color: tinycolor.ColorFormats.RGBA | null;
  focused: boolean;
}

class ThingsboardColor extends React.Component<JsonFormFieldProps, ThingsboardColorState> {

    containerRef = React.createRef<HTMLDivElement>();

    constructor(props: JsonFormFieldProps) {
        super(props);
        this.onBlur = this.onBlur.bind(this);
        this.onFocus = this.onFocus.bind(this);
        this.onValueChanged = this.onValueChanged.bind(this);
        this.onSwatchClick = this.onSwatchClick.bind(this);
        this.onClear = this.onClear.bind(this);
        const value = props.value ? props.value + '' : null;
        const color = value != null ? tinycolor(value).toRgb() : null;
        this.state = {
            color,
            focused: false
        };
    }

    onBlur() {
      this.setState({focused: false});
    }

    onFocus() {
      this.setState({focused: true});
    }

    componentDidMount() {
        const node = this.containerRef.current;
        const colContainer = $(node).children('#color-container');
        colContainer.click(() => {
            if (!this.props.form.readonly) {
              this.onSwatchClick();
            }
        });
    }

    componentWillUnmount() {
        const node = this.containerRef.current;
        const colContainer = $(node).children('#color-container');
        colContainer.off( 'click' );
    }

    onValueChanged(value: tinycolor.ColorFormats.RGBA | null) {
        let color: tinycolor.Instance = null;
        if (value != null) {
            color = tinycolor(value);
        }
        this.setState({
            color: value
        });
        let colorValue = '';
        if (color != null && color.getAlpha() !== 1) {
            colorValue = color.toRgbString();
        } else if (color != null) {
            colorValue = color.toHexString();
        }
        this.props.onChangeValidate({
            target: {
                value: colorValue
            }
        });
    }

    onSwatchClick() {
        this.props.onColorClick(this.props.form.key, this.state.color,
          (color) => {
            this.onValueChanged(color);
          }
        );
    }

    onClear(event: React.MouseEvent) {
        if (event) {
            event.stopPropagation();
        }
        this.onValueChanged(null);
    }

    render() {

        let background = 'rgba(0,0,0,0)';
        if (this.state.color != null) {
            background = `rgba(${ this.state.color.r }, ${ this.state.color.g }, ${ this.state.color.b }, ${ this.state.color.a })`;
        }

        const styles = reactCSS({
            default: {
                color: {
                    background: `${ background }`
                },
                swatch: {
                    display: 'inline-block',
                    marginRight: '10px',
                    marginTop: 'auto',
                    marginBottom: 'auto',
                    cursor: 'pointer',
                    opacity: `${ this.props.form.readonly ? '0.6' : '1' }`
                },
                swatchText: {
                    width: '100%'
                },
                container: {
                    display: 'flex',
                    flexDirection: 'row',
                    alignItems: 'center'
                },
                colorContainer: {
                    display: 'flex',
                    width: '100%'
                }
            },
        });

        let fieldClass = 'tb-field';
        if (this.props.form.required) {
            fieldClass += ' tb-required';
        }
        if (this.props.form.readonly) {
            fieldClass += ' tb-readonly';
        }
        if (this.state.focused) {
            fieldClass += ' tb-focused';
        }

        let stringColor = '';
        if (this.state.color != null) {
            const color = tinycolor(this.state.color);
            stringColor = color.toRgbString();
        }

        return (
            <div ref={this.containerRef} style={ styles.container }>
                 <div id='color-container' style={ styles.colorContainer }>
                    <div className='tb-color-preview' style={ styles.swatch }>
                        <div className='tb-color-result' style={ styles.color }/>
                    </div>
                   <TextField
                     variant={'standard'}
                     className={fieldClass}
                     label={this.props.form.title}
                     error={!this.props.valid}
                     helperText={this.props.valid ? this.props.form.placeholder : this.props.error}
                     value={stringColor}
                     disabled={this.props.form.readonly}
                     onFocus={this.onFocus}
                     onBlur={this.onBlur}
                     style={ styles.swatchText }/>
                 </div>
                 <Tooltip title='Clear' placement='top'><IconButton onClick={this.onClear}><Clear/></IconButton></Tooltip>
            </div>
        );
    }
}

export default ThingsboardBaseComponent(ThingsboardColor);
