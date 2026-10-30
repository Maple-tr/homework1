package com.library.entity;

import java.util.List;

public class Book extends Base {
    private String title;
    private String author;
    private String type;
    private String publisher;
    private Integer days;
    private Double price;
    private Integer stock;
    private String status;
    private String introduction;
    private String embedding; // 根据你的数据库截图，有这个字段
    private String tags;      // 根据你的数据库截图，有这个字段

    // 图书副本集合（非数据库字段，仅用于业务展示）
    private List<BookCopy> bookCopies;

    // --- Getter 和 Setter ---
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getPublisher() { return publisher; }
    public void setPublisher(String publisher) { this.publisher = publisher; }

    public Integer getDays() { return days; }
    public void setDays(Integer days) { this.days = days; }

    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }

    public Integer getStock() { return stock; }
    public void setStock(Integer stock) { this.stock = stock; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getIntroduction() { return introduction; }
    public void setIntroduction(String introduction) { this.introduction = introduction; }

    public String getEmbedding() { return embedding; }
    public void setEmbedding(String embedding) { this.embedding = embedding; }

    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }

    public List<BookCopy> getBookCopies() { return bookCopies; }
    public void setBookCopies(List<BookCopy> bookCopies) { this.bookCopies = bookCopies; }

    @Override
    public String toString() {
        return "Book{" +
                "id='" + getId() + '\'' +
                ", title='" + title + '\'' +
                ", author='" + author + '\'' +
                ", stock=" + stock +
                ", status='" + status + '\'' +
                '}';
    }
}