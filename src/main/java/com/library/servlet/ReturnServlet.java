package com.library.servlet;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.JSON;
import com.library.entity.User;
import com.library.util.DBUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.LocalDateTime;

@WebServlet("/api/return")
public class ReturnServlet extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        resp.setContentType("application/json;charset=UTF-8");
        PrintWriter out = resp.getWriter();

        User user = (User) req.getSession().getAttribute("loginUser");
        if (user == null) {
            out.write("{\"code\": 401, \"msg\": \"请先登录\"}");
            return;
        }

        BufferedReader reader = req.getReader();
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) sb.append(line);
        JSONObject json = JSON.parseObject(sb.toString());
        String loanId = json.getString("loanId");

        try (Connection conn = DBUtil.getConnection()) {
            conn.setAutoCommit(false); // 开启事务

            try {
                // 1. 获取该借阅记录的 bookCopyId
                String getLoanSql = "SELECT bookCopyId FROM `loan` WHERE id = ? AND userId = ? AND status = '未还'";
                PreparedStatement getLoanPs = conn.prepareStatement(getLoanSql);
                getLoanPs.setString(1, loanId);
                getLoanPs.setString(2, user.getId());
                java.sql.ResultSet rs = getLoanPs.executeQuery();

                if (!rs.next()) {
                    out.write("{\"code\": 400, \"msg\": \"未找到该借阅记录或已归还\"}");
                    return;
                }
                String bookCopyId = rs.getString("bookCopyId");

                // 2. 更新借阅记录状态为已还
                LocalDateTime now = LocalDateTime.now();
                String updateLoanSql = "UPDATE `loan` SET status = '已还', returnDate = ? WHERE id = ?";
                PreparedStatement updateLoanPs = conn.prepareStatement(updateLoanSql);
                updateLoanPs.setTimestamp(1, java.sql.Timestamp.valueOf(now));
                updateLoanPs.setString(2, loanId);
                updateLoanPs.executeUpdate();

                // 3. 更新图书副本状态为未借出
                String updateCopySql = "UPDATE `bookcopy` SET isBorrowed = 0 WHERE id = ?";
                PreparedStatement updateCopyPs = conn.prepareStatement(updateCopySql);
                updateCopyPs.setString(1, bookCopyId);
                updateCopyPs.executeUpdate();

                conn.commit(); // 提交事务
                out.write("{\"code\": 200, \"msg\": \"还书成功\"}");

            } catch (Exception e) {
                conn.rollback();
                throw e;
            }
        } catch (Exception e) {
            e.printStackTrace();
            out.write("{\"code\": 500, \"msg\": \"系统异常：" + e.getMessage() + "\"}");
        }
    }
}