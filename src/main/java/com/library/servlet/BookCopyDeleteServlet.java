package com.library.servlet;

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
import java.sql.ResultSet;

@WebServlet("/api/book/copy/delete")
public class BookCopyDeleteServlet extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        resp.setContentType("application/json;charset=UTF-8");
        PrintWriter out = resp.getWriter();

        BufferedReader reader = req.getReader();
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) sb.append(line);
        String copyId = JSON.parseObject(sb.toString()).getString("copyId");

        try (Connection conn = DBUtil.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // 1. 检查副本状态和所属图书
                String checkSql = "SELECT bookId, isBorrowed FROM `bookcopy` WHERE id = ?";
                PreparedStatement checkPs = conn.prepareStatement(checkSql);
                checkPs.setString(1, copyId);
                ResultSet rs = checkPs.executeQuery();

                if (!rs.next()) {
                    out.write("{\"code\": 400, \"msg\": \"副本不存在\"}");
                    return;
                }
                if (rs.getBoolean("isBorrowed")) {
                    out.write("{\"code\": 400, \"msg\": \"该副本正在借阅中，无法删除！\"}");
                    return;
                }
                String bookId = rs.getString("bookId");

                // 2. 删除副本
                String deleteSql = "DELETE FROM `bookcopy` WHERE id = ?";
                PreparedStatement deletePs = conn.prepareStatement(deleteSql);
                deletePs.setString(1, copyId);
                deletePs.executeUpdate();

                // 3. 库存 - 1
                String updateSql = "UPDATE `book` SET `stock` = `stock` - 1 WHERE `id` = ?";
                PreparedStatement updatePs = conn.prepareStatement(updateSql);
                updatePs.setString(1, bookId);
                updatePs.executeUpdate();

                conn.commit();
                out.write("{\"code\": 200, \"msg\": \"删除副本成功\"}");
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