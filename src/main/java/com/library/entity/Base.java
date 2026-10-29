package com.library.entity;

import java.time.LocalDateTime;

public abstract class Base {
    private String id;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    // Getter and Setter
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}