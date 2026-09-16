// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
            boolean castEnable = Boolean.parseBoolean(cmd.getOptionValue("castEnable"));
            File allTelemetrySource = new File(cmd.getOptionValue("telemetryFrom"));
            File tsSaveDir = null;
            File partitionsSaveDir = null;
            File latestSaveDir = null;

            RelatedEntitiesParser allEntityIdsAndTypes =
                    new RelatedEntitiesParser(new File(cmd.getOptionValue("relatedEntities")));
            DictionaryParser dictionaryParser = new DictionaryParser(allTelemetrySource);

            if(cmd.getOptionValue("latestTelemetryOut") != null) {
                latestSaveDir = new File(cmd.getOptionValue("latestTelemetryOut"));
            }
            if(cmd.getOptionValue("telemetryOut") != null) {
                tsSaveDir = new File(cmd.getOptionValue("telemetryOut"));
                partitionsSaveDir = new File(cmd.getOptionValue("partitionsOut"));
            }

            new PgCaMigrator(allTelemetrySource, tsSaveDir, partitionsSaveDir, latestSaveDir, allEntityIdsAndTypes, dictionaryParser, castEnable).migrate();

        } catch (Throwable th) {
            th.printStackTrace();
            throw new IllegalStateException("failed", th);
        }

    }

    private static CommandLine parseArgs(String[] args) {
        Options options = new Options();

        Option telemetryAllFrom = new Option("telemetryFrom", "telemetryFrom", true, "telemetry source file");
        telemetryAllFrom.setRequired(true);
        options.addOption(telemetryAllFrom);

        Option latestTsOutOpt = new Option("latestOut", "latestTelemetryOut", true, "latest telemetry save dir");
        latestTsOutOpt.setRequired(false);
        options.addOption(latestTsOutOpt);

        Option tsOutOpt = new Option("tsOut", "telemetryOut", true, "sstable save dir");
        tsOutOpt.setRequired(false);
        options.addOption(tsOutOpt);

        Option partitionOutOpt = new Option("partitionsOut", "partitionsOut", true, "partitions save dir");
        partitionOutOpt.setRequired(false);
        options.addOption(partitionOutOpt);

        Option castOpt = new Option("castEnable", "castEnable", true, "cast String to Double if possible");
        castOpt.setRequired(true);
        options.addOption(castOpt);

        Option relatedOpt = new Option("relatedEntities", "relatedEntities", true, "related entities source file path");
        relatedOpt.setRequired(true);
        options.addOption(relatedOpt);

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
