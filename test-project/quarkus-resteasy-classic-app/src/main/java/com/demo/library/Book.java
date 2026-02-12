package com.demo.library;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "books")
public class Book extends PanacheEntity {

    public String title;
    public String author;
    public int publicationYear;
    public String isbn;

    public Book() {
    }

    public Book(String title, String author, int year, String isbn) {
        this.title = title;
        this.author = author;
        this.publicationYear = year;
        this.isbn = isbn;
    }
}
