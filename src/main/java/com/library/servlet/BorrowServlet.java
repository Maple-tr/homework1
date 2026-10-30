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
import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.util.UUID;

@WebServlet("/api/borrow")
public class BorrowServlet extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        resp.setContentType("application/json;charset=UTF-8");
        PrintWriter out = resp.getWriter();

        HttpSession session = req.getSession();
        User user = (User) session.getAttribute("loginUser");
        if (user == null) {
            out.write("{\"code\": 401, \"msg\": \"请先登录\"}");
            return;
        }

        BufferedReader reader = req.getReader();
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) sb.append(line);
        JSONObject json = JSON.parseObject(sb.toString());
        String bookId = json.getString("bookId");

        try (Connection conn = DBUtil.getConnection()) {
            conn.setAutoCommit(false); // 开启事务

            try {
                // 1. 找一本该图书且未被借出的副本
                String findCopySql = "SELECT id FROM `bookcopy` WHERE bookId = ? AND isBorrowed = 0 LIMIT 1";
                PreparedStatement findPs = conn.prepareStatement(findCopySql);
                findPs.setString(1, bookId);
                ResultSet rs = findPs.executeQuery();

                if (!rs.next()) {
                    out.write("{\"code\": 400, \"msg\": \"该书暂无库存可借\"}");
                    return;
                }
                String copyId = rs.getString("id");

                // 2. 更新副本状态为已借出 (isBorrowed = 1)
                String updateCopySql = "UPDATE `bookcopy` SET isBorrowed = 1 WHERE id = ?";
                PreparedStatement updatePs = conn.prepareStatement(updateCopySql);
                updatePs.setString(1, copyId);
                updatePs.executeUpdate();

                // 3. 查询图书的还书期限 (天数)
                String getBookSql = "SELECT days FROM `book` WHERE id = ?";
                PreparedStatement getBookPs = conn.prepareStatement(getBookSql);
                getBookPs.setString(1, bookId);
                ResultSet bookRs = getBookPs.executeQuery();
                int days = 30; // 默认30天
                if (bookRs.next() && bookRs.getObject("days") != null) {
                    days = bookRs.getInt("days");
                }

                // 4. 插入借阅记录 (loan 表)
                LocalDateTime now = LocalDateTime.now();
                LocalDateTime dueDate = now.plusDays(days);
                String insertLoanSql = "INSERT INTO `loan` (`id`, `createTime`, `updateTime`, `bookCopyId`, `userId`, `borrowDate`, `dueDate`, `status`) VALUES (?,?,?,?,?,?,?,?)";
                PreparedStatement insertPs = conn.prepareStatement(insertLoanSql);
                insertPs.setString(1, UUID.randomUUID().toString().replace("-", ""));
                insertPs.setTimestamp(2, java.sql.Timestamp.valueOf(now));
                insertPs.setTimestamp(3, java.sql.Timestamp.valueOf(now));
                insertPs.setString(4, copyId);
                insertPs.setString(5, user.getId());
                insertPs.setTimestamp(6, java.sql.Timestamp.valueOf(now));
                insertPs.setTimestamp(7, java.sql.Timestamp.valueOf(dueDate));
                insertPs.setString(8, "未还");
                insertPs.executeUpdate();

                conn.commit(); // 提交事务
                out.write("{\"code\": 200, \"msg\": \"借阅成功！应还日期：" + dueDate.toLocalDate() + "\"}");

            } catch (Exception e) {
                conn.rollback(); // 出现异常，回滚所有操作
                throw e;
            }
        } catch (Exception e) {
            e.printStackTrace();
            out.write("{\"code\": 500, \"msg\": \"系统异常：" + e.getMessage() + "\"}");
        }
    }
}