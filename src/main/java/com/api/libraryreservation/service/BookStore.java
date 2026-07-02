package com.api.libraryreservation.service;

import com.api.libraryreservation.model.Book;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class BookStore {
    private final Map<Long, Book> books = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    @PostConstruct
    public void seed() {

    }

    public Book add(String title, String author, int totalCopies) {
        long id = idGenerator.getAndIncrement();
        Book book = new Book(id, title, author, totalCopies);
        books.put(id, book);
        return book;
    }

    public Book get(Long id) {
        return books.get(id);
    }

    public Iterable<Book> getAll() {
        return books.values();
    }
}
