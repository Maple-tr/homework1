// BookCopy.java
package com.library.entity;

public class BookCopy extends Base {
    private String bookId; // 对应数据库的 bookId 外键
    private Book book;
    private boolean isBorrowed;

    public String getBookId() { return bookId; }
    public void setBookId(String bookId) { this.bookId = bookId; }
    public Book getBook() { return book; }
    public void setBook(Book book) { this.book = book; }
    public boolean isBorrowed() { return isBorrowed; }
    public void setBorrowed(boolean borrowed) { isBorrowed = borrowed; }
}