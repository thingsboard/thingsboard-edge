/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2020 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.client.tools.migrator;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import java.io.File;

public class MigratorTool {

    public static void main(String[] args) {
        CommandLine cmd = parseArgs(args);


        try {
            File latestSource = new File(cmd.getOptionValue("latestTelemetryFrom"));
            File latestSaveDir = new File(cmd.getOptionValue("latestTelemetryOut"));
            File tsSource = new File(cmd.getOptionValue("telemetryFrom"));
            File tsSaveDir = new File(cmd.getOptionValue("telemetryOut"));
            File partitionsSaveDir = new File(cmd.getOptionValue("partitionsOut"));
            boolean castEnable = Boolean.parseBoolean(cmd.getOptionValue("castEnable"));

            PgCaLatestMigrator.migrateLatest(latestSource, latestSaveDir, castEnable);
            PostgresToCassandraTelemetryMigrator.migrateTs(tsSource, tsSaveDir, partitionsSaveDir, castEnable);

        } catch (Throwable th) {
            th.printStackTrace();
            throw new IllegalStateException("failed", th);
        }

    }

    private static CommandLine parseArgs(String[] args) {
        Options options = new Options();

        Option latestTsOpt = new Option("latestFrom", "latestTelemetryFrom", true, "latest telemetry source file path");
        latestTsOpt.setRequired(true);
        options.addOption(latestTsOpt);

        Option latestTsOutOpt = new Option("latestOut", "latestTelemetryOut", true, "latest telemetry save dir");
        latestTsOutOpt.setRequired(true);
        options.addOption(latestTsOutOpt);

        Option tsOpt = new Option("tsFrom", "telemetryFrom", true, "telemetry source file path");
        tsOpt.setRequired(true);
        options.addOption(tsOpt);

        Option tsOutOpt = new Option("tsOut", "telemetryOut", true, "sstable save dir");
        tsOutOpt.setRequired(true);
        options.addOption(tsOutOpt);

        Option partitionOutOpt = new Option("partitionsOut", "partitionsOut", true, "partitions save dir");
        partitionOutOpt.setRequired(true);
        options.addOption(partitionOutOpt);

        Option castOpt = new Option("castEnable", "castEnable", true, "cast String to Double if possible");
        castOpt.setRequired(true);
        options.addOption(castOpt);

        HelpFormatter formatter = new HelpFormatter();
        CommandLineParser parser = new BasicParser();

        try {
            return parser.parse(options, args);
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            formatter.printHelp("utility-name", options);

            System.exit(1);
        }
        return null;
    }

}
