package com.library.servlet;

import com.alibaba.fastjson.JSON;
import com.library.entity.Book;
import com.library.util.DBUtil;
import com.library.util.JDBCTool;

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
import java.util.List;

@WebServlet("/api/book/query")
public class BookQueryServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        resp.setContentType("application/json;charset=UTF-8");
        PrintWriter out = resp.getWriter();

        String keyword = req.getParameter("keyword");
        if (keyword == null) keyword = "";
        String searchKey = "%" + keyword + "%";

        try (Connection conn = DBUtil.getConnection()) {
            // 模糊查询书名或作者，并按创建时间倒序（新书在最前）
            String sql = "SELECT * FROM `book` WHERE `title` LIKE ? OR `author` LIKE ? ORDER BY `createTime` DESC";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, searchKey);
            ps.setString(2, searchKey);
            ResultSet rs = ps.executeQuery();

            List<Book> list = JDBCTool.resultSetToList(rs, Book.class);
            out.write("{\"code\": 200, \"data\": " + JSON.toJSONString(list) + "}");
        } catch (Exception e) {
            e.printStackTrace();
            out.write("{\"code\": 500, \"msg\": \"查询失败\"}");
        }
    }
}