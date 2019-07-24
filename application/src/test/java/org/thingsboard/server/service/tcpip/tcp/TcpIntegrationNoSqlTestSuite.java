package org.thingsboard.server.service.tcpip.tcp;

import org.cassandraunit.dataset.cql.ClassPathCQLDataSet;
import org.junit.ClassRule;
import org.junit.extensions.cpsuite.ClasspathSuite;
import org.junit.runner.RunWith;
import org.thingsboard.server.dao.CustomCassandraCQLUnit;

import java.util.Arrays;

@RunWith(ClasspathSuite.class)
@ClasspathSuite.ClassnameFilters({
        "org.thingsboard.server.service.tcpip.tcp.payload.nosql.*Test"
})
public class TcpIntegrationNoSqlTestSuite {
    @ClassRule
    public static CustomCassandraCQLUnit cassandraUnit =
            new CustomCassandraCQLUnit(
                    Arrays.asList(
                            new ClassPathCQLDataSet("cassandra/schema-ts.cql", false, false),
                            new ClassPathCQLDataSet("cassandra/schema-entities.cql", false, false),
                            new ClassPathCQLDataSet("cassandra/system-data.cql", false, false)),
                    "cassandra-test.yaml", 30000l);
}
