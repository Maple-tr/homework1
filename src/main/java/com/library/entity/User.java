package com.library.entity;

import java.time.LocalDateTime;

public abstract class User extends Base {
    private String name;
    private String password;
    private String phone;
    private String role;
    private String major;
    private LocalDateTime loginTime;

    // 子类特有字段统统写在这里（因为数据库是一张大表）
    private Integer permissionLevel; // Admin
    private java.time.LocalDate entryTime; // Admin
    private String area; // Operator
    private String duty; // Operator
    private String grade; // Student
    private String college; // Student, Teacher
    private String title; // Teacher

    public abstract String[] hasPermission();

    // Getters and Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getMajor() { return major; }
    public void setMajor(String major) { this.major = major; }
    public LocalDateTime getLoginTime() { return loginTime; }
    public void setLoginTime(LocalDateTime loginTime) { this.loginTime = loginTime; }
    public Integer getPermissionLevel() { return permissionLevel; }
    public void setPermissionLevel(Integer permissionLevel) { this.permissionLevel = permissionLevel; }
    public java.time.LocalDate getEntryTime() { return entryTime; }
    public void setEntryTime(java.time.LocalDate entryTime) { this.entryTime = entryTime; }
    public String getArea() { return area; }
    public void setArea(String area) { this.area = area; }
    public String getDuty() { return duty; }
    public void setDuty(String duty) { this.duty = duty; }
    public String getGrade() { return grade; }
    public void setGrade(String grade) { this.grade = grade; }
    public String getCollege() { return college; }
    public void setCollege(String college) { this.college = college; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
}