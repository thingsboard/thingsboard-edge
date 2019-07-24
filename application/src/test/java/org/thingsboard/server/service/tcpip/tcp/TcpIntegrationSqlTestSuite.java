package org.thingsboard.server.service.tcpip.tcp;

import org.junit.ClassRule;
import org.junit.extensions.cpsuite.ClasspathSuite;
import org.junit.runner.RunWith;
import org.thingsboard.server.dao.CustomSqlUnit;

import java.util.Arrays;

@RunWith(ClasspathSuite.class)
@ClasspathSuite.ClassnameFilters({
        "org.thingsboard.server.service.tcpip.tcp.payload.sql.*Test"
})
public class TcpIntegrationSqlTestSuite {
    @ClassRule
    public static CustomSqlUnit sqlUnit = new CustomSqlUnit(
            Arrays.asList("sql/schema-ts.sql", "sql/schema-entities.sql", "sql/system-data.sql"),
            "sql/drop-all-tables.sql",
            "sql-test.properties");
}
