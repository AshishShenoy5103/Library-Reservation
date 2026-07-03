package com.api.libraryreservation.controller;

import com.api.libraryreservation.model.Book;
import com.api.libraryreservation.model.Reservation;
import com.api.libraryreservation.service.BookStore;
import com.api.libraryreservation.service.ReservationStore;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/books")
public class BookController {
    private final BookStore bookStore;
    private final ReservationStore reservationStore;

    public BookController(BookStore bookStore, ReservationStore reservationStore) {
        this.bookStore = bookStore;
        this.reservationStore = reservationStore;
    }

    @GetMapping
    public ResponseEntity<?> listBooks() {
        return ResponseEntity.ok(bookStore.getAll());
    }

    @PostMapping
    public ResponseEntity<?> addBook(@RequestBody Map<String, Object> body) {
        String title = (String) body.get("title");
        String author = (String) body.get("author");
        int copies = (int) body.get("totalCopies");
        Book book = bookStore.add(title, author, copies);
        return ResponseEntity.status(HttpStatus.CREATED).body(book);
    }

    @PostMapping("/{id}/reserve")
    public ResponseEntity<?> reserveBook(@PathVariable Long id) {
        Book book = bookStore.get(id);
        if(book == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Book Not Found"));
        }

        if(book.getAvailableCopies() <= 0) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "No copies available"));
        }

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        book.setAvailableCopies(book.getAvailableCopies()-1);
        Reservation reservation = reservationStore.create(username, id);

        return ResponseEntity.ok(reservation);
    }

    @PostMapping("/{id}/return")
    public ResponseEntity<?> returnBook(@PathVariable Long id) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();

        Reservation reservation = reservationStore.findActiveReservation(username, id);
        if(reservation == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "No active reservation found for this book"));
        }

        reservation.setStatus("RETURNED");
        Book book = bookStore.get(id);
        book.setAvailableCopies(book.getAvailableCopies() + 1);

        return ResponseEntity.ok(reservation);
    }

    @GetMapping("/my-reservation")
    public ResponseEntity<?> myReservation() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(reservationStore.getByUsername(username));
    }
}
