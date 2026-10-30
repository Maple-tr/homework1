package com.library.servlet;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.library.entity.BookRecommend;
import com.library.entity.User;
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

@WebServlet("/api/recommend/add")
public class RecommendBookServlet extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        resp.setContentType("application/json;charset=UTF-8");
        PrintWriter out = resp.getWriter();

        User user = (User) req.getSession().getAttribute("loginUser");
        if (user == null) { out.write("{\"code\": 401, \"msg\": \"请先登录\"}"); return; }

        BufferedReader reader = req.getReader();
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) sb.append(line);
        JSONObject json = JSON.parseObject(sb.toString());
        String bookName = json.getString("bookName");

        try (Connection conn = DBUtil.getConnection()) {
            // 1. 查重：图书馆是否已经有这本书
            String checkSql = "SELECT COUNT(*) FROM `book` WHERE `title` = ?";
            PreparedStatement checkPs = conn.prepareStatement(checkSql);
            checkPs.setString(1, bookName);
            ResultSet rs = checkPs.executeQuery();
            if (rs.next() && rs.getInt(1) > 0) {
                out.write("{\"code\": 400, \"msg\": \"图书馆已有该书籍，无需推荐！\"}");
                return;
            }

            // 2. 插入推荐表
            BookRecommend rec = new BookRecommend();
            rec.setUserId(user.getId());
            rec.setBookName(bookName);
            rec.setAuthor(json.getString("author"));
            rec.setPublisher(json.getString("publisher"));
            rec.setPrice(json.getDouble("price"));
            rec.setStatus("待审核");

            JDBCTool.save(rec, conn);
            out.write("{\"code\": 200, \"msg\": \"推荐已提交，等待管理员审核\"}");

        } catch (Exception e) {
            e.printStackTrace();
            out.write("{\"code\": 500, \"msg\": \"系统异常\"}");
        }
    }
}