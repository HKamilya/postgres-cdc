package ru.kpfu.itis.postgrescdc.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;

import java.sql.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Slf4j
public abstract class ConnectorServiceImpl {

    String createUrl(String host, String port, String database) {
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

    public void dropPublication(Connection connection, String publication) throws SQLException {

        try (PreparedStatement preparedStatement =
                     connection.prepareStatement("DROP PUBLICATION " + publication)) {
            preparedStatement.execute();
        }
    }

    public void createPublication(Connection connection, String publication, boolean forAllTables, String tables) throws SQLException {
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

    public void createLogicalReplicationSlot(Connection connection, String slotName, String outputPlugin) throws InterruptedException, SQLException, TimeoutException {
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

    public boolean isReplicationSlotActive(Connection connection, String slotName)
            throws SQLException {

        try (PreparedStatement preparedStatement = connection.prepareStatement("SELECT active FROM pg_replication_slots WHERE slot_name = ?")) {
            preparedStatement.setString(1, slotName);
            try (ResultSet rs = preparedStatement.executeQuery()) {
                return rs.next() && rs.getBoolean(1);
            }
        }
    }

    public void waitStopReplicationSlot(Connection connection, String slotName)
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
}
