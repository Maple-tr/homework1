package com.library.servlet;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.library.entity.Book;
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
import java.util.UUID;

@WebServlet("/api/book/add")
public class AddBookServlet extends HttpServlet {
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

        try (Connection conn = DBUtil.getConnection()) {
            conn.setAutoCommit(false); // 开启事务

            try {
                // 1. 插入 Book
                Book book = new Book();
                book.setTitle(json.getString("title"));
                book.setAuthor(json.getString("author"));
                book.setType(json.getString("type"));
                book.setPublisher(json.getString("publisher"));
                book.setDays(json.getInteger("days"));
                book.setPrice(json.getDouble("price"));
                book.setStock(json.getInteger("stock"));
                book.setStatus("正常");
                book.setIntroduction(json.getString("introduction"));

                JDBCTool.save(book, conn);

                // 2. 根据库存自动生成 BookCopy 副本
                int stock = book.getStock();
                if (stock > 0) {
                    String copySql = "INSERT INTO `bookcopy` (`id`, `createTime`, `updateTime`, `bookId`, `isBorrowed`) VALUES (?,?,?,?,?)";
                    PreparedStatement ps = conn.prepareStatement(copySql);
                    for (int i = 0; i < stock; i++) {
                        ps.setString(1, UUID.randomUUID().toString().replace("-", ""));
                        ps.setTimestamp(2, java.sql.Timestamp.valueOf(java.time.LocalDateTime.now()));
                        ps.setTimestamp(3, java.sql.Timestamp.valueOf(java.time.LocalDateTime.now()));
                        ps.setString(4, book.getId());
                        ps.setBoolean(5, false); // 默认未借出
                        ps.addBatch();
                    }
                    ps.executeBatch();
                    ps.close();
                }

                conn.commit(); // 提交事务
                out.write("{\"code\": 200, \"msg\": \"添加图书及副本成功\"}");

            } catch (Exception e) {
                conn.rollback();
                throw e;
            }
        } catch (Exception e) {
            e.printStackTrace();
            out.write("{\"code\": 500, \"msg\": \"服务器异常：" + e.getMessage() + "\"}");
        }
    }
}