package com.library.entity;
public class Teacher extends User {
    @Override
    public String[] hasPermission() {
        return new String[]{"借书", "还书", "个人借阅查询", "推荐图书"};
    }
}