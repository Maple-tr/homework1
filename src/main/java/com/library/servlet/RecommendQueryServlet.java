package com.library.servlet;

import com.alibaba.fastjson.JSON;
import com.library.entity.BookRecommend;
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

@WebServlet("/api/recommend/query")
public class RecommendQueryServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json;charset=UTF-8");
        PrintWriter out = resp.getWriter();

        try (Connection conn = DBUtil.getConnection()) {
            // 按时间倒序查询所有推荐记录
            String sql = "SELECT * FROM `bookrecommend` ORDER BY `createTime` DESC";
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();

            List<BookRecommend> list = JDBCTool.resultSetToList(rs, BookRecommend.class);
            out.write("{\"code\": 200, \"data\": " + JSON.toJSONString(list) + "}");
        } catch (Exception e) {
            e.printStackTrace();
            out.write("{\"code\": 500, \"msg\": \"查询失败\"}");
        }
    }
}