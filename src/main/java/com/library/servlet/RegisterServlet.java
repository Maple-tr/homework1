package com.library.servlet;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.library.entity.*;
import com.library.util.DBUtil;
import com.library.util.JDBCTool;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;

@WebServlet("/api/register")
public class RegisterServlet extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // 强制设置请求体编码为 UTF-8
        req.setCharacterEncoding("UTF-8");
        resp.setContentType("application/json;charset=UTF-8");
        PrintWriter out = resp.getWriter();

        // 1. 接收数据
        BufferedReader reader = req.getReader();
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) sb.append(line);
        JSONObject json = JSON.parseObject(sb.toString());

        String name = json.getString("name");
        String password = json.getString("password");
        String phone = json.getString("phone");
        String major = json.getString("major");
        String role = json.getString("role");

        try (Connection conn = DBUtil.getConnection()) {
            // 2. 校验账号和手机号是否重复
            String checkSql = "SELECT COUNT(*) FROM `user` WHERE name = ? OR phone = ?";
            PreparedStatement checkPs = conn.prepareStatement(checkSql);
            checkPs.setString(1, name);
            checkPs.setString(2, phone);
            ResultSet checkRs = checkPs.executeQuery();
            if (checkRs.next() && checkRs.getInt(1) > 0) {
                out.write("{\"code\": 400, \"msg\": \"姓名或手机号已被注册，请重试\"}");
                return;
            }

            // 3. 根据角色实例化具体子类
            User user;
            if ("管理员".equals(role)) {
                Admin admin = new Admin();
                admin.setPermissionLevel(json.getInteger("permissionLevel"));
                String entry = json.getString("entryTime");
                if (entry != null && !entry.isEmpty()) {
                    admin.setEntryTime(LocalDate.parse(entry));
                }
                user = admin;
            } else if ("操作员".equals(role)) {
                Operator op = new Operator();
                op.setArea(json.getString("area"));
                op.setDuty(json.getString("duty"));
                user = op;
            } else if ("教师".equals(role)) {
                Teacher t = new Teacher();
                t.setCollege(json.getString("college"));
                t.setTitle(json.getString("title"));
                user = t;
            } else {
                Student s = new Student();
                s.setGrade(json.getString("grade"));
                s.setCollege(json.getString("college"));
                user = s;
            }

            // 4. 填充公共字段
            user.setName(name);
            user.setPassword(password);
            user.setPhone(phone);
            user.setMajor(major);
            user.setRole(role);

            // 5. 保存到数据库 (现在 JDBCTool 能正确找到 user 表了)
            JDBCTool.save(user, conn);
            out.write("{\"code\": 200, \"msg\": \"注册成功\"}");

        } catch (Exception e) {
            e.printStackTrace();
            out.write("{\"code\": 500, \"msg\": \"服务器异常：" + e.getMessage() + "\"}");
        }
    }
}