package ru.kpfu.itis.postgrescdc;

import lombok.extern.slf4j.Slf4j;
import org.postgresql.PGConnection;
import org.postgresql.PGProperty;
import org.postgresql.replication.LogSequenceNumber;
import org.postgresql.replication.PGReplicationStream;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.nio.ByteBuffer;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.util.concurrent.TimeUnit;


//    SELECT pg_create_logical_replication_slot('test_slot_v1', 'pgoutput')
// CREATE PUBLICATION "pub1" FOR ALL TABLES;

@Slf4j
public class Server {

    public static void main(String[] args) {
        String url = "jdbc:postgresql:";

        Properties props = new Properties();
        PGProperty.PG_HOST.set(props, "localhost");
        PGProperty.PG_PORT.set(props, "5434");
        PGProperty.PG_DBNAME.set(props, "postgres");
        PGProperty.USER.set(props, "postgres");
        PGProperty.PASSWORD.set(props, "postgres");
        PGProperty.ASSUME_MIN_SERVER_VERSION.set(props, "9.4");
        PGProperty.REPLICATION.set(props, "database");

        try (Connection con = DriverManager.getConnection(url, props)) {
            PGConnection replCnt = con.unwrap(PGConnection.class);

            PGReplicationStream stream = replCnt.getReplicationAPI()
                    .replicationStream()
                    .logical()
                    .withSlotName("test_slot_v1")
                    .withSlotOption("proto_version", 1)
                    .withSlotOption("publication_names", "pub1")
                    .withStatusInterval(20, TimeUnit.SECONDS)
                    .start();
            log.info("stream starts");
            int count = 0;
            while (true) {
                ByteBuffer msg = stream.readPending();
                if (msg == null) {
                    TimeUnit.MILLISECONDS.sleep(10L);
                    log.info("msg is null");
                } else {
                    int offset = msg.arrayOffset();
                    byte[] source = msg.array();
                    int length = source.length - offset;

                    LogSequenceNumber lastReceiveLSN = stream.getLastReceiveLSN();
                    log.info("lsn = {}, length={}", lastReceiveLSN, length);
                }
            }
        } catch (SQLException | InterruptedException ex) {
            throw new RuntimeException(ex);
        }

    }
}
