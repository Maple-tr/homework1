package com.library.servlet;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.library.entity.Admin;
import com.library.entity.Operator;
import com.library.entity.Student;
import com.library.entity.Teacher;
import com.library.entity.User;
import com.library.util.DBUtil;
import com.library.util.JDBCTool;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.List;

@WebServlet("/api/login")
public class LoginServlet extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // 1. 设置编码
        req.setCharacterEncoding("UTF-8");
        resp.setContentType("application/json;charset=UTF-8");
        PrintWriter out = resp.getWriter();

        // 2. 接收数据
        BufferedReader reader = req.getReader();
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) sb.append(line);
        JSONObject json = JSON.parseObject(sb.toString());

        String name = json.getString("name");
        String password = json.getString("password");
        String captcha = json.getString("captcha");

        // 3. 校验验证码
        HttpSession session = req.getSession();
        String sessionCode = (String) session.getAttribute("captchaCode");
        if (sessionCode == null || !sessionCode.equalsIgnoreCase(captcha)) {
            out.write("{\"code\": 400, \"msg\": \"验证码错误\"}");
            return;
        }

        // 4. 校验账号密码
        try (Connection conn = DBUtil.getConnection()) {
            String sql = "SELECT * FROM user WHERE name = ? AND password = ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, name);
            ps.setString(2, password);
            java.sql.ResultSet rs = ps.executeQuery();

            List<Student> list = JDBCTool.resultSetToList(rs, Student.class);
            if (list.isEmpty()) {
                out.write("{\"code\": 400, \"msg\": \"用户名或密码错误\"}");
            } else {
                Student s = list.get(0);
                User user;

                // 根据中文角色匹配具体的子类
                String roleStr = s.getRole();
                if ("管理员".equals(roleStr)) {
                    user = new Admin();
                } else if ("操作员".equals(roleStr)) {
                    user = new Operator();
                } else if ("教师".equals(roleStr)) {
                    user = new Teacher();
                } else {
                    user = new Student();
                }
                user = JSON.parseObject(JSON.toJSONString(s), user.getClass());

                // ==========================================
                // === 新增：更新数据库中的最后登录时间 ===
                // ==========================================
                java.time.LocalDateTime now = java.time.LocalDateTime.now();
                String updateSql = "UPDATE `user` SET `loginTime` = ? WHERE `id` = ?";
                PreparedStatement updatePs = conn.prepareStatement(updateSql);
                updatePs.setTimestamp(1, java.sql.Timestamp.valueOf(now));
                updatePs.setString(2, user.getId());
                updatePs.executeUpdate();
                updatePs.close();

                // 把新时间同步给当前对象，以便存入 Session
                user.setLoginTime(now);

                // ==========================================
                // 保存用户信息到 Session
                // ==========================================
                session.setAttribute("loginUser", user);
                session.setAttribute("userRole", user.getRole());
                session.setAttribute("userName", user.getName());

                out.write("{\"code\": 200, \"msg\": \"登录成功\", \"role\": \"" + user.getRole() + "\"}");
            }
        } catch (Exception e) {
            e.printStackTrace();
            out.write("{\"code\": 500, \"msg\": \"服务器异常\"}");
        }
    }
}