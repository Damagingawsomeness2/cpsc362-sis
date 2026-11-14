package edu.csuf.sis;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Student Information System for CSUF (CPSC 362 group project).
 *
 * <p>One deployable app (a modular monolith). Each package matches a box in the component diagram:
 * {@code gateway} (API Gateway), {@code catalog}, {@code registration}, {@code records}, {@code audit},
 * {@code identity}, {@code chat}, and {@code domain} (the SIS Database tables).
 */
@SpringBootApplication
public class SisApplication {

    public static void main(String[] args) {
        SpringApplication.run(SisApplication.class, args);
    }
}
