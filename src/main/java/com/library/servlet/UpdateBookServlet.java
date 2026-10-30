package com.library.servlet;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.library.util.DBUtil;

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

@WebServlet("/api/book/update")
public class UpdateBookServlet extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        resp.setContentType("application/json;charset=UTF-8");
        PrintWriter out = resp.getWriter();

        BufferedReader reader = req.getReader();
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) sb.append(line);
        JSONObject json = JSON.parseObject(sb.toString());

        try (Connection conn = DBUtil.getConnection()) {
            // 按照要求，只更新指定的字段
            String sql = "UPDATE `book` SET `title`=?, `author`=?, `type`=?, `publisher`=?, `days`=?, `price`=?, `updateTime`=? WHERE `id`=?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, json.getString("title"));
            ps.setString(2, json.getString("author"));
            ps.setString(3, json.getString("type"));
            ps.setString(4, json.getString("publisher"));
            ps.setInt(5, json.getInteger("days"));
            ps.setDouble(6, json.getDouble("price"));
            ps.setTimestamp(7, java.sql.Timestamp.valueOf(java.time.LocalDateTime.now()));
            ps.setString(8, json.getString("id"));

            int rows = ps.executeUpdate();
            if (rows > 0) {
                out.write("{\"code\": 200, \"msg\": \"修改成功\"}");
            } else {
                out.write("{\"code\": 400, \"msg\": \"修改失败，未找到该书\"}");
            }
        } catch (Exception e) {
            e.printStackTrace();
            out.write("{\"code\": 500, \"msg\": \"系统异常：" + e.getMessage() + "\"}");
        }
    }
}