package com.api.libraryreservation.service;

import com.api.libraryreservation.model.Reservation;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class ReservationStore {
    private final List<Reservation> reservations = new ArrayList<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    public Reservation create(String username, Long bookId) {
        Reservation reservation = new Reservation(idGenerator.getAndIncrement(), username, bookId);
        reservations.add(reservation);
        return reservation;
    }

    public List<Reservation> getByUsername(String username) {
        return reservations.stream()
                .filter(r -> r.getUsername().equals(username))
                .toList();
    }

    public Reservation findActiveReservation(String username, Long bookId) {
        return reservations.stream()
                .filter(r -> r.getUsername().equals(username) && r.getBookId().equals(bookId) && r.getStatus().equals("ACTIVE"))
                .findFirst()
                .orElse(null);
    }
}
