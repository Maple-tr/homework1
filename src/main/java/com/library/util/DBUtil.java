package com.library.util;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Properties;

public class DBUtil {
    private static String url;
    private static String username;
    private static String password;

    static {
        try {
            // 加载 resources 目录下的 db.properties
            InputStream is = DBUtil.class.getClassLoader().getResourceAsStream("db.properties");
            if (is == null) {
                throw new RuntimeException("找不到 db.properties 文件！");
            }
            Properties prop = new Properties();
            prop.load(is);
            url = prop.getProperty("jdbc.url");
            username = prop.getProperty("jdbc.username");
            password = prop.getProperty("jdbc.password");
            String driver = prop.getProperty("jdbc.driver");

            // 加载驱动
            Class.forName(driver);
        } catch (Exception e) {
            throw new RuntimeException("数据库配置加载失败", e);
        }
    }

    // 获取数据库连接
    public static Connection getConnection() throws Exception {
        return DriverManager.getConnection(url, username, password);
    }

    // 关闭连接
    public static void close(Connection conn) {
        if (conn != null) {
            try {
                conn.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}