package com.library.servlet;

import com.alibaba.fastjson.JSON;
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

@WebServlet("/api/book/copy/query")
public class BookCopyQueryServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json;charset=UTF-8");
        PrintWriter out = resp.getWriter();
        String bookId = req.getParameter("bookId");

        try (Connection conn = DBUtil.getConnection()) {
            String sql = "SELECT id, isBorrowed FROM `bookcopy` WHERE bookId = ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, bookId);
            ResultSet rs = ps.executeQuery();

            List<Map<String, Object>> list = new ArrayList<>();
            while (rs.next()) {
                Map<String, Object> map = new HashMap<>();
                map.put("id", rs.getString("id"));
                map.put("isBorrowed", rs.getBoolean("isBorrowed"));
                list.add(map);
            }
            out.write("{\"code\": 200, \"data\": " + JSON.toJSONString(list) + "}");
        } catch (Exception e) {
            e.printStackTrace();
            out.write("{\"code\": 500, \"msg\": \"查询失败\"}");
        }
    }
}