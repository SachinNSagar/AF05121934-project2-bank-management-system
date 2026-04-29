package com.bank.util;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Singleton-style JDBC connection provider.
 * Reads configuration from {@code db.properties} on the classpath.
 */
public final class DBConnection {

    private static String url;
    private static String username;
    private static String password;

    private DBConnection() {
        // utility class
    }

    static {
        try (InputStream in = DBConnection.class
                .getClassLoader()
                .getResourceAsStream("db.properties")) {

            if (in == null) {
                throw new IllegalStateException("db.properties not found on classpath");
            }

            Properties props = new Properties();
            props.load(in);

            Class.forName(props.getProperty("db.driver"));

            url      = props.getProperty("db.url");
            username = props.getProperty("db.username");
            password = props.getProperty("db.password");

        } catch (IOException | ClassNotFoundException e) {
            throw new ExceptionInInitializerError(
                    "Failed to initialize database driver: " + e.getMessage());
        }
    }

    /**
     * Returns a new JDBC connection. Caller is responsible for closing it
     * (preferably via try-with-resources).
     */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, username, password);
    }
}
