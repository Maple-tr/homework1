package com.library.entity;
public class Admin extends User {
    @Override
    public String[] hasPermission() {
        return new String[]{"用户信息查询", "借阅记录查询", "借阅数据统计", "借书", "还书", "审阅推荐图书"};
    }
}