///
/// ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
///
/// Copyright © 2016-2023 ThingsBoard, Inc. All Rights Reserved.
///
/// NOTICE: All information contained herein is, and remains
/// the property of ThingsBoard, Inc. and its suppliers,
/// if any.  The intellectual and technical concepts contained
/// herein are proprietary to ThingsBoard, Inc.
/// and its suppliers and may be covered by U.S. and Foreign Patents,
/// patents in process, and are protected by trade secret or copyright law.
///
/// Dissemination of this information or reproduction of this material is strictly forbidden
/// unless prior written permission is obtained from COMPANY.
///
/// Access to the source code contained herein is hereby forbidden to anyone except current COMPANY employees,
/// managers or contractors who have executed Confidentiality and Non-disclosure agreements
/// explicitly covering such access.
///
/// The copyright notice above does not evidence any actual or intended publication
/// or disclosure  of  this source code, which includes
/// information that is confidential and/or proprietary, and is a trade secret, of  COMPANY.
/// ANY REPRODUCTION, MODIFICATION, DISTRIBUTION, PUBLIC  PERFORMANCE,
/// OR PUBLIC DISPLAY OF OR THROUGH USE  OF THIS  SOURCE CODE  WITHOUT
/// THE EXPRESS WRITTEN CONSENT OF COMPANY IS STRICTLY PROHIBITED,
/// AND IN VIOLATION OF APPLICABLE LAWS AND INTERNATIONAL TREATIES.
/// THE RECEIPT OR POSSESSION OF THIS SOURCE CODE AND/OR RELATED INFORMATION
/// DOES NOT CONVEY OR IMPLY ANY RIGHTS TO REPRODUCE, DISCLOSE OR DISTRIBUTE ITS CONTENTS,
/// OR TO MANUFACTURE, USE, OR SELL ANYTHING THAT IT  MAY DESCRIBE, IN WHOLE OR IN PART.
///

import express from 'express';
import { _logger}  from '../config/logger';
import http from 'http';
import { Socket } from 'net';

export class HttpServer {

  private logger = _logger('httpServer');
  private app = express();
  private server: http.Server | null;
  private connections: Socket[] = [];

  constructor(httpPort: number) {
    this.app.get('/livenessProbe', async (req, res) => {
      const message = {
        now: new Date().toISOString()
      };
      res.send(message);
    })

    this.server = this.app.listen(httpPort, () => {
      this.logger.info('Started HTTP endpoint on port %s. Please, use /livenessProbe !', httpPort);
    }).on('error', (error) => {
      this.logger.error(error);
    });

    this.server.on('connection', connection => {
      this.connections.push(connection);
      connection.on('close', () => this.connections = this.connections.filter(curr => curr !== connection));
    });
  }

  async stop() {
    if (this.server) {
      this.logger.info('Stopping HTTP Server...');
      const _server = this.server;
      this.server = null;
      this.connections.forEach(curr => curr.end(() => curr.destroy()));
      await new Promise<void>(
          (resolve, reject) => {
            _server.close((err) => {
              this.logger.info('HTTP Server stopped.');
              resolve();
            });
          }
      );
    }
  }
}
