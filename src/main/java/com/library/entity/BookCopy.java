package com.library.entity;

public class BookCopy extends Base {
    private Book book;
    private boolean isBorrowed;

    public Book getBook() { return book; }
    public void setBook(Book book) { this.book = book; }
    public boolean isBorrowed() { return isBorrowed; }
    public void setBorrowed(boolean borrowed) { isBorrowed = borrowed; }
}