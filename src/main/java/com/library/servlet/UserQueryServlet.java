package com.library.servlet;

import com.alibaba.fastjson.JSON;
import com.library.entity.Student;
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

@WebServlet("/api/user/query")
public class UserQueryServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        resp.setContentType("application/json;charset=UTF-8");
        PrintWriter out = resp.getWriter();

        // 获取前端传来的关键字（如果没有传，默认为空字符串）
        String keyword = req.getParameter("keyword");
        if (keyword == null) keyword = "";
        String searchKey = "%" + keyword + "%";

        try (Connection conn = DBUtil.getConnection()) {
            // 模糊查询：姓名、手机号、专业 包含关键字
            // 排序：createTime 倒序 (最新在前面)
            String sql = "SELECT * FROM `user` WHERE " +
                    "`name` LIKE ? OR " +
                    "`phone` LIKE ? OR " +
                    "`major` LIKE ? " +
                    "ORDER BY `createTime` DESC";

            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, searchKey);
            ps.setString(2, searchKey);
            ps.setString(3, searchKey);

            ResultSet rs = ps.executeQuery();

            // 借用 Student 类接收全部字段
            List<Student> list = JDBCTool.resultSetToList(rs, Student.class);

            // 包装成统一格式返回
            out.write("{\"code\": 200, \"data\": " + JSON.toJSONString(list) + "}");

        } catch (Exception e) {
            e.printStackTrace();
            out.write("{\"code\": 500, \"msg\": \"查询失败：" + e.getMessage() + "\"}");
        }
    }
}