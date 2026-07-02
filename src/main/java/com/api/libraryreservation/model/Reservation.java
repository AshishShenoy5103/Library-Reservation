package com.api.libraryreservation.model;

import java.time.LocalDateTime;

public class Reservation {
    private Long id;
    private String username;
    private Long bookId;
    private LocalDateTime reservedAt;
    private String status;

    public Reservation(Long id, String username, Long bookId) {
        this.id = id;
        this.username = username;
        this.bookId = bookId;
        this.reservedAt = LocalDateTime.now();
        this.status = "ACTIVE";
    }

    public Long getId() {return id;}
    public String getUsername() {return username;}
    public Long getBookId() {return bookId;}
    public LocalDateTime getReservedAt() {return reservedAt;}
    public String getStatus() {return status;}

    public void setStatus(String status) {this.status = status;}
}
