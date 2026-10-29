package com.library.entity;
public class Operator extends User {
    @Override
    public String[] hasPermission() {
        return new String[]{"添加书籍", "查看所有书籍", "废弃图书", "编辑图书", "图书副本编辑", "借书", "还书", "个人借阅查询"};
    }
}