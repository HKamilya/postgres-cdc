package ru.kpfu.itis.postgrescdc.service.replication;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.postgresql.PGProperty;
import org.postgresql.core.BaseConnection;
import org.postgresql.core.ServerVersion;
import org.postgresql.replication.LogSequenceNumber;
import ru.kpfu.itis.postgrescdc.model.PluginEnum;

import java.sql.*;
import java.util.Objects;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Slf4j
public abstract class ReplicationServiceImpl {

    protected String createUrl(String host, String port, String database) {
        return "jdbc:postgresql://" + host + ':' + port + '/' + database;
    }

    public Connection createConnection(String user, String password, String host, String port, String database) throws SQLException {
        try {
            return DriverManager.getConnection(createUrl(host, port, database), user, password);
        } catch (SQLException ex) {
            // ignore
        }
        return DriverManager.getConnection(createUrl(host, port, database), user, password);

    }

    protected void dropPublication(Connection connection, String publication) throws SQLException {

        try (PreparedStatement preparedStatement =
                     connection.prepareStatement("DROP PUBLICATION " + publication)) {
            preparedStatement.execute();
        }
    }

    protected void createPublication(Connection connection, String publication, boolean forAllTables, String tables) throws SQLException {
        if (StringUtils.isNotBlank(tables)) {
            try (PreparedStatement preparedStatement =
                         connection.prepareStatement("CREATE PUBLICATION " + publication + " FOR TABLE " + tables)) {
                preparedStatement.execute();
            }
        }
        if (forAllTables) {
            try (PreparedStatement preparedStatement =
                         connection.prepareStatement("CREATE PUBLICATION " + publication + " FOR ALL TABLES")) {
                preparedStatement.execute();
            }
        }
    }

    protected void createLogicalReplicationSlot(Connection connection, String slotName, String outputPlugin) throws InterruptedException, SQLException, TimeoutException {
        //drop previous slot
        dropReplicationSlot(connection, slotName);

        try (PreparedStatement preparedStatement =
                     connection.prepareStatement("SELECT * FROM pg_create_logical_replication_slot(?, ?)")) {
            preparedStatement.setString(1, slotName);
            preparedStatement.setString(2, outputPlugin);
            try (ResultSet rs = preparedStatement.executeQuery()) {
                while (rs.next()) {
                    log.info("Slot Name: " + rs.getString(1));
                    log.info("Xlog Position: " + rs.getString(2));
                }
            }

        }
    }

    public void dropReplicationSlot(Connection connection, String slotName)
            throws SQLException, InterruptedException, TimeoutException {
        try (PreparedStatement preparedStatement = connection.prepareStatement(
                "SELECT pg_terminate_backend(active_pid) FROM pg_replication_slots "
                        + "WHERE active = true AND slot_name = ?")) {
            preparedStatement.setString(1, slotName);
            preparedStatement.execute();
        }

        waitStopReplicationSlot(connection, slotName);

        try (PreparedStatement preparedStatement = connection.prepareStatement("SELECT pg_drop_replication_slot(slot_name) "
                + "FROM pg_replication_slots WHERE slot_name = ?")) {
            preparedStatement.setString(1, slotName);
            preparedStatement.execute();
        }
    }

    private void waitStopReplicationSlot(Connection connection, String slotName)
            throws InterruptedException, TimeoutException, SQLException {
        long startWaitTime = System.currentTimeMillis();
        boolean stillActive;
        long timeInWait = 0;

        do {
            stillActive = isReplicationSlotActive(connection, slotName);
            if (stillActive) {
                TimeUnit.MILLISECONDS.sleep(100L);
                timeInWait = System.currentTimeMillis() - startWaitTime;
            }
        } while (stillActive && timeInWait <= 30000);

        if (stillActive) {
            throw new TimeoutException("Wait stop replication slot " + timeInWait + " timeout occurs");
        }
    }

    protected boolean isReplicationSlotExists(String slotName, String plugin, Connection connection) {
        try (Statement st = connection.createStatement()) {
            try (ResultSet rs = st.executeQuery("select slot_name, plugin from pg_replication_slots")) {
                if (rs.next()) {
                    return Objects.equals(rs.getString(1), slotName) && Objects.equals(rs.getString(2), plugin);
                }
                return false;
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    public boolean isReplicationSlotActive(Connection connection, String slotName)
            throws SQLException {

        try (PreparedStatement preparedStatement = connection.prepareStatement("SELECT active FROM pg_replication_slots WHERE slot_name = ?")) {
            preparedStatement.setString(1, slotName);
            try (ResultSet rs = preparedStatement.executeQuery()) {
                return rs.next() && rs.getBoolean(1);
            }
        }
    }

    protected LogSequenceNumber getCurrentLSN(Connection connection) throws SQLException {
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

    protected Connection openReplicationConnection(String user, String password, String host, String port, String database) throws Exception {
        Properties properties = new Properties();
        properties.setProperty("user", user);
        properties.setProperty("password", password);
        PGProperty.ASSUME_MIN_SERVER_VERSION.set(properties, "9.4");
        PGProperty.REPLICATION.set(properties, "database");
        PGProperty.PREFER_QUERY_MODE.set(properties, "simple");
        return DriverManager.getConnection(createUrl(host, port, database), properties);
    }

    public abstract void receiveChangesFromCurrentLsn(Connection connection,
                                                      Connection replicationConnection,
                                                      PluginEnum plugin,
                                                      String slotName,
                                                      String publicationName,
                                                      String topic,
                                                      UUID connectorId,
                                                      String lsnString, String tables) throws Exception;
}
