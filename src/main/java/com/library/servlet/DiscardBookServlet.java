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
import java.sql.ResultSet;

@WebServlet("/api/book/discard")
public class DiscardBookServlet extends HttpServlet {
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
        String bookId = json.getString("id");

        try (Connection conn = DBUtil.getConnection()) {
            // 1. 检查是否有未还的借阅记录 (通过 bookcopy 表关联)
            String checkSql = "SELECT COUNT(*) FROM `loan` l " +
                    "JOIN `bookcopy` c ON l.bookCopyId = c.id " +
                    "WHERE c.bookId = ? AND l.status = '未还'";
            PreparedStatement checkPs = conn.prepareStatement(checkSql);
            checkPs.setString(1, bookId);
            ResultSet rs = checkPs.executeQuery();

            if (rs.next() && rs.getInt(1) > 0) {
                out.write("{\"code\": 400, \"msg\": \"该书还有未归还的副本，无法废弃！\"}");
                return;
            }

            // 2. 如果没有未还的，执行废弃（改状态）
            String updateSql = "UPDATE `book` SET `status` = '已废弃', `updateTime` = ? WHERE `id` = ?";
            PreparedStatement updatePs = conn.prepareStatement(updateSql);
            updatePs.setTimestamp(1, java.sql.Timestamp.valueOf(java.time.LocalDateTime.now()));
            updatePs.setString(2, bookId);

            int rows = updatePs.executeUpdate();
            if (rows > 0) {
                out.write("{\"code\": 200, \"msg\": \"图书已成功废弃\"}");
            } else {
                out.write("{\"code\": 400, \"msg\": \"废弃失败\"}");
            }
        } catch (Exception e) {
            e.printStackTrace();
            out.write("{\"code\": 500, \"msg\": \"系统异常：" + e.getMessage() + "\"}");
        }
    }
}