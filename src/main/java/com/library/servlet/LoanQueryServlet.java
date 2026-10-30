package com.library.servlet;

import com.alibaba.fastjson.JSON;
import com.library.entity.User;
import com.library.util.DBUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@WebServlet("/api/loan/query")
public class LoanQueryServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json;charset=UTF-8");
        PrintWriter out = resp.getWriter();

        User user = (User) req.getSession().getAttribute("loginUser");
        if (user == null) {
            out.write("{\"code\": 401, \"msg\": \"请先登录\"}");
            return;
        }

        try (Connection conn = DBUtil.getConnection()) {
            // 连表查询，把书名、副本号一起查出来
            String sql = "SELECT l.id as loanId, l.status, l.borrowDate, l.dueDate, " +
                    "b.title, b.author, c.id as copyId " +
                    "FROM `loan` l " +
                    "JOIN `bookcopy` c ON l.bookCopyId = c.id " +
                    "JOIN `book` b ON c.bookId = b.id " +
                    "WHERE l.userId = ? ORDER BY l.borrowDate DESC";

            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, user.getId());
            ResultSet rs = ps.executeQuery();

            List<Map<String, Object>> list = new ArrayList<>();
            while (rs.next()) {
                Map<String, Object> map = new HashMap<>();
                map.put("loanId", rs.getString("loanId"));
                map.put("status", rs.getString("status"));
                map.put("title", rs.getString("title"));
                map.put("author", rs.getString("author"));
                map.put("copyId", rs.getString("copyId"));
                map.put("borrowDate", rs.getTimestamp("borrowDate") != null ? rs.getTimestamp("borrowDate").toString().substring(0, 10) : "");
                map.put("dueDate", rs.getTimestamp("dueDate") != null ? rs.getTimestamp("dueDate").toString().substring(0, 10) : "");
                list.add(map);
            }
            out.write("{\"code\": 200, \"data\": " + JSON.toJSONString(list) + "}");
        } catch (Exception e) {
            e.printStackTrace();
            out.write("{\"code\": 500, \"msg\": \"查询异常\"}");
        }
    }
}