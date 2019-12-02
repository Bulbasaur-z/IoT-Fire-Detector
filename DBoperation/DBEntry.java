package DBoperation;

import LogInfo.AppLog;

import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Yucong Li
 * @version 1.0.1
 */

public class DBEntry {
    /**
     * Provide database operation API
     */
    private static final String JDBC_DRIVER = "com.mysql.cj.jdbc.Driver";
    private String DB_URL;
    private String username;
    private String passwrd;
    private Logger logger = AppLog.getLogger();
    private static final String[] RelationTableCol = new String[]{"username", "device_ID"};
    private static final String[] UserInfoCol = new String[]{"username", "password"};
    private static final String[] SensorInfoCol = new String[]{"device_ID", "locate"};

    /**
     * @param DatabaseName Name of DatabaseName
     * @param hostname database IP address
     * @param port database port
     * @param username username to connect database
     * @param passwrd password corresponding with the username
     */
    public DBEntry(String DatabaseName,
                   String hostname,
                   String port,
                   String username,
                   String passwrd) {
        this.DB_URL = "jdbc:mysql://" +
                hostname + ":" + port + "/"
                + DatabaseName;
        this.username = username;
        this.passwrd = passwrd;
        //register driver
        try {
            Class.forName(JDBC_DRIVER);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private List<Map> FormatData(String TableName, ResultSet rs) throws SQLException{
        String[] cols;
        ArrayList<Map> resultList = new ArrayList<>();
        switch (TableName){
            case "user_Info":
                cols = UserInfoCol;
                break;
            case "sensor_info":
                cols = SensorInfoCol;
                break;
            case "relation_table":
                cols = RelationTableCol;
                break;
            default:
                throw new SQLException("Invalid Table name");
        }
        if(rs!=null) {
            rs.beforeFirst();
            while (rs.next()) {
                Map<String, String> row = new LinkedHashMap<>();
                for (String col : cols) {
                    row.put(col, rs.getString(col));
                }
                resultList.add(row);
            }
            if(resultList.size()>0)
                return resultList;
        }
        return null;
    }

    /**
     *
     * @param TableName String
     * @param query_key String
     * @param query_value String
     * @return
     */
    public List<Map> ReadData(String TableName, String query_key, String query_value) {
        String sql = String.format("SELECT * FROM %s WHERE %s='%s'", TableName, query_key, query_value);
        ResultSet rs = null;
        try (Connection conn = DriverManager.getConnection(this.DB_URL, this.username, this.passwrd);
             Statement stmt = conn.createStatement()) {
            //ger result
            rs = stmt.executeQuery(sql);
            rs.beforeFirst();
            return FormatData(TableName, rs);
        } catch (SQLException ex) {
            String msg = "SQLException: " + ex.getMessage();
            msg += "\nSQLState: " + ex.getSQLState();
            msg += "\nVendorError" + ex.getErrorCode();
            logger.log(Level.SEVERE, msg, ex);
            return null;
        }
    }


    public boolean InsertData(String TableName, String[] keys, String[] values) {
        try (Connection conn = DriverManager.getConnection(this.DB_URL, this.username, this.passwrd);
             Statement stmt = conn.createStatement()) {
            String sql = String.format("INSERT INTO %s(%s) VALUES('%s')",
                    TableName, String.join(", ", keys), String.join("', '", values));
            int num = stmt.executeUpdate(sql);
            return num > 0;
        } catch (SQLException ex) {
            String msg = "SQLException: " + ex.getMessage();
            msg += "\nSQLState: " + ex.getSQLState();
            msg += "\nVendorError" + ex.getErrorCode();
            logger.log(Level.SEVERE, msg, ex);;
            return false;
        }
    }

}
