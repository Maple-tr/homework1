package com.library;

import com.library.entity.User;
import com.library.entity.Student;
import com.library.util.DBUtil;
import com.library.util.JDBCTool;

import java.sql.Connection;
import java.util.List;

public class TestDB {
    public static void main(String[] args) {
        try (Connection conn = DBUtil.getConnection()) {
            System.out.println("数据库连接成功！");

            // 测试查询 user 表（注意：User 是抽象类，JDBCTool 实例化会报错）
            // 所以我们先用具体的 Student.class 去映射，查一下试试
            List<Student> students = JDBCTool.resultSetToList(
                    conn.createStatement().executeQuery("SELECT * FROM user LIMIT 5"),
                    Student.class
            );

            System.out.println("查到了 " + students.size() + " 条用户数据：");
            for (Student s : students) {
                System.out.println("姓名: " + s.getName() + ", 角色: " + s.getRole() + ", 手机: " + s.getPhone());
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}