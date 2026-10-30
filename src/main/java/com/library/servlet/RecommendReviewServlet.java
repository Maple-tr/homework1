package com.library.servlet;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.library.entity.Book;
import com.library.entity.BookRecommend;
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
import java.util.UUID;

@WebServlet("/api/recommend/review")
public class RecommendReviewServlet extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        resp.setContentType("application/json;charset=UTF-8");
        PrintWriter out = resp.getWriter();

        BufferedReader reader = req.getReader();
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) sb.append(line);
        JSONObject json = JSON.parseObject(sb.toString());

        String recId = json.getString("id");
        String action = json.getString("action"); // "pass" 或 "reject"
        String reason = json.getString("reason");

        try (Connection conn = DBUtil.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // 查推荐信息
                PreparedStatement ps = conn.prepareStatement("SELECT * FROM `bookrecommend` WHERE id = ?");
                ps.setString(1, recId);
                ResultSet rs = ps.executeQuery();
                if(!rs.next()) {
                    out.write("{\"code\": 400, \"msg\": \"记录不存在\"}");
                    return;
                }

                String bookName = rs.getString("bookName");
                String author = rs.getString("author");
                String publisher = rs.getString("publisher");
                Double price = rs.getDouble("price");

                if ("pass".equals(action)) {
                    // 1. 加入图书表
                    Book book = new Book();
                    book.setTitle(bookName);
                    book.setAuthor(author);
                    book.setPublisher(publisher);
                    book.setPrice(price);
                    book.setStock(1);
                    book.setStatus("正常");
                    JDBCTool.save(book, conn);

                    // 2. 自动生成 1 个副本
                    String copySql = "INSERT INTO `bookcopy` (`id`, `createTime`, `updateTime`, `bookId`, `isBorrowed`) VALUES (?,?,?,?,?)";
                    PreparedStatement copyPs = conn.prepareStatement(copySql);
                    copyPs.setString(1, UUID.randomUUID().toString().replace("-", ""));
                    copyPs.setTimestamp(2, java.sql.Timestamp.valueOf(java.time.LocalDateTime.now()));
                    copyPs.setTimestamp(3, java.sql.Timestamp.valueOf(java.time.LocalDateTime.now()));
                    copyPs.setString(4, book.getId());
                    copyPs.setBoolean(5, false);
                    copyPs.executeUpdate();

                    // 3. 更新状态
                    PreparedStatement updatePs = conn.prepareStatement("UPDATE `bookrecommend` SET status = '已通过' WHERE id = ?");
                    updatePs.setString(1, recId);
                    updatePs.executeUpdate();

                } else {
                    // 驳回
                    PreparedStatement updatePs = conn.prepareStatement("UPDATE `bookrecommend` SET status = '已驳回', reason = ? WHERE id = ?");
                    updatePs.setString(1, reason);
                    updatePs.setString(2, recId);
                    updatePs.executeUpdate();
                }

                conn.commit();
                out.write("{\"code\": 200, \"msg\": \"操作成功\"}");
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