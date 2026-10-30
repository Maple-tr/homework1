package com.library.servlet;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.JSON;
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
import java.util.UUID;

@WebServlet("/api/book/copy/add")
public class BookCopyAddServlet extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        resp.setContentType("application/json;charset=UTF-8");
        PrintWriter out = resp.getWriter();

        BufferedReader reader = req.getReader();
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) sb.append(line);
        String bookId = JSON.parseObject(sb.toString()).getString("bookId");

        try (Connection conn = DBUtil.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // 1. 插入副本
                String insertSql = "INSERT INTO `bookcopy` (`id`, `createTime`, `updateTime`, `bookId`, `isBorrowed`) VALUES (?,?,?,?,?)";
                PreparedStatement ps = conn.prepareStatement(insertSql);
                ps.setString(1, UUID.randomUUID().toString().replace("-", ""));
                ps.setTimestamp(2, java.sql.Timestamp.valueOf(java.time.LocalDateTime.now()));
                ps.setTimestamp(3, java.sql.Timestamp.valueOf(java.time.LocalDateTime.now()));
                ps.setString(4, bookId);
                ps.setBoolean(5, false);
                ps.executeUpdate();

                // 2. 库存 + 1
                String updateSql = "UPDATE `book` SET `stock` = `stock` + 1 WHERE `id` = ?";
                PreparedStatement updatePs = conn.prepareStatement(updateSql);
                updatePs.setString(1, bookId);
                updatePs.executeUpdate();

                conn.commit();
                out.write("{\"code\": 200, \"msg\": \"新增副本成功\"}");
            } catch (Exception e) {
                conn.rollback();
                throw e;
            }
        } catch (Exception e) {
            e.printStackTrace();
            out.write("{\"code\": 500, \"msg\": \"系统异常\"}");
        }
    }
}