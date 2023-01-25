package ru.kpfu.itis.postgrescdc.service;

import lombok.extern.slf4j.Slf4j;
import org.postgresql.PGConnection;
import org.postgresql.PGProperty;
import org.postgresql.core.BaseConnection;
import org.postgresql.core.ServerVersion;
import org.postgresql.replication.LogSequenceNumber;
import org.postgresql.replication.PGReplicationStream;
import org.postgresql.util.PSQLException;
import org.springframework.stereotype.Service;
import ru.kpfu.itis.postgrescdc.model.ConnectorModel;

import java.nio.ByteBuffer;
import java.sql.*;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;


@Slf4j
@Service
public class Wal2JsonConnectorServiceImpl extends ConnectorServiceImpl implements Wal2JsonConnectorService {

    private static final String SLOT_NAME = "cdc_postgres_wal2json_replication_slot";

    private static final String PUBLICATION = "cdc_postgres_wal2json_pub";

    private static String toString(ByteBuffer buffer) {
        int offset = buffer.arrayOffset();
        byte[] source = buffer.array();
        int length = source.length - offset;

        return new String(source, offset, length);
    }

    @Override
    public void receiveChangesOccursBeforeStartReplication(Connection connection, Connection replicationConnection, boolean fromBegin) throws Exception {
        PGConnection pgConnection = (PGConnection) replicationConnection;

        LogSequenceNumber lsn;
        if (fromBegin) {
            lsn = getAllLSN(connection);
        } else {
            lsn = getCurrentLSN(connection);
        }

        PGReplicationStream stream =
                pgConnection
                        .getReplicationAPI()
                        .replicationStream()
                        .logical()
                        .withSlotName(SLOT_NAME)
                        .withStartPosition(lsn)
//                        .withSlotOption("proto_version", 1)
//                        .withSlotOption("publication_names", PUBLICATION)
                        //   .withSlotOption("include-xids", true)
                        //    .withSlotOption("skip-empty-xacts", true)
                        //    .withSlotOption("proto_version",1)
                        //   .withSlotOption("publication_names", PUBLICATION)
                        //    .withSlotOption("binary","true")
                        //    .withSlotOption("sizeof_datum", "8")
                        //    .withSlotOption("sizeof_int", "4")
                        //       .withSlotOption("sizeof_long", "8")
                        //       .withSlotOption("bigendian", "false")
                        //       .withSlotOption("float4_byval", "true")
                        //       .withSlotOption("float8_byval", "true")
                        //       .withSlotOption("integer_datetimes", "true")
                        // .withSlotOption("include-xids", true)
                        // .withSlotOption("skip-empty-xacts", true)
                        .withStatusInterval(10, TimeUnit.SECONDS)
                        .start();
        ByteBuffer buffer;
        while (true) {
            buffer = stream.readPending();
            if (buffer == null) {
                TimeUnit.MILLISECONDS.sleep(10L);
                continue;
            }

           log.info(toString(buffer));
            //feedback
            stream.setAppliedLSN(stream.getLastReceiveLSN());
            stream.setFlushedLSN(stream.getLastReceiveLSN());
        }

    }

    private LogSequenceNumber getCurrentLSN(Connection connection) throws SQLException {
        try (Statement st = connection.createStatement()) {
            try (ResultSet rs = st.executeQuery("select "
                    + (((BaseConnection) connection).haveMinimumServerVersion(ServerVersion.v10)
                    ? "pg_current_wal_lsn()" : "pg_current_xlog_location()"))) {

                if (rs.next()) {
                    String lsn = rs.getString(1);
                    return LogSequenceNumber.valueOf(lsn);
                } else {
                    return LogSequenceNumber.INVALID_LSN;
                }
            }
        }
    }

    private LogSequenceNumber getAllLSN(Connection connection) throws SQLException {
        try (Statement st = connection.createStatement()) {
            try (ResultSet rs = st.executeQuery("SELECT * FROM pg_logical_slot_peek_changes('" + SLOT_NAME + "', null, null);")) {

                if (rs.next()) {
                    String lsn = rs.getString(1);
                    return LogSequenceNumber.valueOf(lsn);
                } else {
                    return LogSequenceNumber.INVALID_LSN;
                }
            }
        }
    }

    private Connection openReplicationConnection(String user, String password, String host, String port, String database) throws Exception {
        Properties properties = new Properties();
        properties.setProperty("user", user);
        properties.setProperty("password", password);
        PGProperty.ASSUME_MIN_SERVER_VERSION.set(properties, "9.4");
        PGProperty.REPLICATION.set(properties, "database");
        PGProperty.PREFER_QUERY_MODE.set(properties, "simple");
        return DriverManager.getConnection(createUrl(host, port, database), properties);
    }

    private boolean isServerCompatible(Connection connection) {
        return ((BaseConnection) connection).haveMinimumServerVersion(ServerVersion.v9_5);
    }


    public void createConnection(ConnectorModel connectorModel) {
        Connection connection;

        try {
            connection = createConnection(connectorModel.getUser(), connectorModel.getPassword(), connectorModel.getHost(), connectorModel.getPort(), connectorModel.getDatabase());

            if (!isServerCompatible(connection)) {
                log.info("must have server version greater than 9.4");
                System.exit(-1);
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            return;
        }
        try {
            createLogicalReplicationSlot(connection, SLOT_NAME, connectorModel.getPlugin().name());
            try {
                dropPublication(connection, PUBLICATION);
            } catch (PSQLException e) {
                // ignore
            }
            createPublication(connection, PUBLICATION, connectorModel.isForAllTables(), connectorModel.getTables());
            Connection replicationConnection = openReplicationConnection(connectorModel.getUser(), connectorModel.getPassword(), connectorModel.getHost(), connectorModel.getPort(), connectorModel.getDatabase());
            receiveChangesOccursBeforeStartReplication(connection, replicationConnection, connectorModel.isFromBegin());

        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
