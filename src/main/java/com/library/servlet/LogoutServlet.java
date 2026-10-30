package com.library.servlet;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet("/api/logout")
public class LogoutServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // 销毁当前用户的 Session，彻底清除登录状态
        req.getSession().invalidate();
        resp.setContentType("application/json;charset=UTF-8");
        resp.getWriter().write("{\"code\": 200, \"msg\": \"已退出登录\"}");
    }
}