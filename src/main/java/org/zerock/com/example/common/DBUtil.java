package org.zerock.com.example.common;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

@Component
public class DBUtil {

    private static DataSource dataSource;

    @Autowired
    public DBUtil(DataSource ds) {
        DBUtil.dataSource = ds;
    }

    public static Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }
}
