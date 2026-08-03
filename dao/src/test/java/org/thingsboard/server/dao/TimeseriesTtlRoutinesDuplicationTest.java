/**
 * Copyright © 2016-2026 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.dao;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The timeseries TTL routines are defined in two schema files on purpose, because the two files reach different
 * databases:
 * <ul>
 *     <li>{@code schema-ts-psql.sql} is executed only on a fresh install
 *     ({@code ThingsboardInstallService} calls {@code tsDatabaseSchemaService.createDatabaseSchema()} outside the
 *     upgrade branch), so it can never correct an already-installed database;</li>
 *     <li>{@code schema-functions.sql} is re-executed on every offline upgrade via
 *     {@code createOrUpdateViewsAndFunctions()}, with no version or LTS-family filtering, which is what carries a
 *     corrected body to existing databases - including across families, e.g. 4.2.x to 4.3.x.</li>
 * </ul>
 * Two copies mean they can silently diverge, and a stale copy is invisible: the TTL failure is swallowed by
 * {@code JpaSqlTimeseriesDao#cleanupPartitions}, so telemetry rows keep being deleted while partitions are not.
 * This test makes divergence a build failure instead.
 * <p>
 * Deliberately NOT covered here: the copies under {@code application/src/main/data/upgrade/lts/<version>/}. Those
 * are point-in-time snapshots of released migrations and must stay frozen, so a future correction to these routines
 * is expected to leave them behind rather than rewrite shipped history.
 */
class TimeseriesTtlRoutinesDuplicationTest {

    private static final String TS_SCHEMA = "sql/schema-ts-psql.sql";
    private static final String FUNCTIONS_SCHEMA = "sql/schema-functions.sql";

    private static final List<String> DUPLICATED_ROUTINES = List.of(
            "drop_partitions_by_system_ttl",
            "get_partition_by_system_ttl_date",
            "cleanup_timeseries_by_ttl");

    /** Closing line of a dollar-quoted body: '$$;' or '$$ LANGUAGE plpgsql;'. */
    private static final Pattern BODY_END = Pattern.compile("^\\$\\$.*;\\s*$");

    @Test
    void duplicatedRoutinesAreIdenticalInBothSchemaFiles() {
        List<String> tsSchema = readLines(TS_SCHEMA);
        List<String> functionsSchema = readLines(FUNCTIONS_SCHEMA);

        for (String routine : DUPLICATED_ROUTINES) {
            assertThat(extractRoutine(functionsSchema, routine, FUNCTIONS_SCHEMA))
                    .as("%s must be identical in %s and %s - update both copies", routine, TS_SCHEMA, FUNCTIONS_SCHEMA)
                    .isEqualTo(extractRoutine(tsSchema, routine, TS_SCHEMA));
        }
    }

    @Test
    void eachDuplicatedRoutineIsDefinedExactlyOncePerFile() {
        for (String schema : List.of(TS_SCHEMA, FUNCTIONS_SCHEMA)) {
            List<String> lines = readLines(schema);
            for (String routine : DUPLICATED_ROUTINES) {
                assertThat(lines.stream().filter(line -> isDefinitionOf(line, routine)).count())
                        .as("%s must be defined exactly once in %s", routine, schema)
                        .isEqualTo(1);
            }
        }
    }

    private static String extractRoutine(List<String> lines, String routine, String schema) {
        int start = -1;
        for (int i = 0; i < lines.size(); i++) {
            if (isDefinitionOf(lines.get(i), routine)) {
                start = i;
                break;
            }
        }
        assertThat(start).as("%s is not defined in %s", routine, schema).isNotNegative();

        for (int i = start; i < lines.size(); i++) {
            if (BODY_END.matcher(lines.get(i)).matches()) {
                return String.join("\n", lines.subList(start, i + 1));
            }
        }
        throw new AssertionError("Unterminated body of " + routine + " in " + schema);
    }

    private static boolean isDefinitionOf(String line, String routine) {
        return line.startsWith("CREATE OR REPLACE FUNCTION " + routine + "(")
                || line.startsWith("CREATE OR REPLACE PROCEDURE " + routine + "(");
    }

    private static List<String> readLines(String resource) {
        try {
            return Resources.toString(Resources.getResource(resource), Charsets.UTF_8).lines().toList();
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to read " + resource, e);
        }
    }

}
