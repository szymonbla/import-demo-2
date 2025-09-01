package com.example.importdemo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ImportDemoApplication {
    public static void main(String[] args) {
        SpringApplication.run(ImportDemoApplication.class, args);
    }
}
/*
*** nie mozna korzystac ze spring batch
1. Chcemy miec predkosc wrzucania osob do pliku dla h2 ok 50k rzedow na sekunde
2. Chcemy zeby import byl nieblokujacy
3. Uzytkownik moze wrzucic dowolnie duzy plik ale pamiec apliacji w jednym momencie nie moze przekroczyc 200MB java.lang.OutOfMemoryError: Java heap space] with root cause
4. Import ma byc transakcyjny czyli albo wszystko albo nic DONE
5.* moze sie wykonywac tylko 1 import na raz
 */