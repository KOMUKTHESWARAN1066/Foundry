package com.MaDuSOFTSolutions.foundry;
import java.sql.Connection;
import java.sql.DriverManager;
public class MSSQLConnection {
    private String ip, port, db, username, password;

    public MSSQLConnection(String ip, String port, String db,
                           String username, String password) {
        this.ip = ip;
        this.port = port;
        this.db = db;
        this.username = username;
        this.password = password;
    }

    public Connection CONN() {
        Connection conn = null;
        try {
            Class.forName("net.sourceforge.jtds.jdbc.Driver");
            String connUrl = "jdbc:jtds:sqlserver://" + ip + ":" + port + "/" + db +
                    ";user=" + username + ";password=" + password + ";";
            conn = DriverManager.getConnection(connUrl);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return conn;
    }
}