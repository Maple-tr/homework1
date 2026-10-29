package com.library.servlet;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

@WebServlet("/api/userInfo")
public class UserInfoServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json;charset=UTF-8");
        HttpSession session = req.getSession();
        String name = (String) session.getAttribute("userName");
        String role = (String) session.getAttribute("userRole");

        if (name == null) {
            resp.getWriter().write("{\"code\": 401, \"msg\": \"未登录\"}");
        } else {
            resp.getWriter().write("{\"code\": 200, \"name\": \"" + name + "\", \"role\": \"" + role + "\"}");
        }
    }
}