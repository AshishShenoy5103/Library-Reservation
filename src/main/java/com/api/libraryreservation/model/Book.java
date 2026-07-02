package com.api.libraryreservation.model;

public class Book {
    private Long id;
    private String title;
    private String author;
    private int totalCopies;
    private int availableCopies;

    public Book(Long id, String title, String author, int totalCopies, int availableCopies) {
        this.id = id;
        this.title = title;
        this.author = author;
        this.totalCopies = totalCopies;
        this.availableCopies = availableCopies;
    }

    public Long getId() {return id;}
    public String getTitle() {return title;}
    public String getAuthor() {return author;}
    public int getTotalCopies() {return totalCopies;}
    public int getAvailableCopies() {return availableCopies;}

    public void setAvailableCopies(int availableCopies) {this.availableCopies = availableCopies;}
}
