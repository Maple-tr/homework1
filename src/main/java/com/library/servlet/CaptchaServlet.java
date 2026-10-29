package com.library.servlet;

import javax.imageio.ImageIO;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Random;

@WebServlet("/api/captcha")
public class CaptchaServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // 1. 生成验证码
        int width = 100, height = 38;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics g = image.getGraphics();
        g.setColor(new Color(240, 240, 240));
        g.fillRect(0, 0, width, height);

        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        Random r = new Random();
        StringBuilder code = new StringBuilder();
        g.setFont(new Font("Arial", Font.BOLD, 24));
        for (int i = 0; i < 4; i++) {
            String c = String.valueOf(chars.charAt(r.nextInt(chars.length())));
            code.append(c);
            g.setColor(new Color(r.nextInt(150), r.nextInt(150), r.nextInt(150)));
            g.drawString(c, 20 * i + 10, 28);
        }

        // 画干扰线
        for (int i = 0; i < 5; i++) {
            g.setColor(new Color(r.nextInt(200), r.nextInt(200), r.nextInt(200)));
            g.drawLine(r.nextInt(width), r.nextInt(height), r.nextInt(width), r.nextInt(height));
        }
        g.dispose();

        // 2. 存入 Session
        HttpSession session = req.getSession();
        session.setAttribute("captchaCode", code.toString());

        // 3. 输出图片
        resp.setContentType("image/jpeg");
        ImageIO.write(image, "JPEG", resp.getOutputStream());
    }
}