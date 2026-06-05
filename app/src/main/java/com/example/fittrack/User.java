package com.example.fittrack;

public class User {
    public String name, email, userId;
    public int age;
    public double weight, height;

    // Required empty constructor for Firestore
    public User() {}

    public User(String userId, String name, String email) {
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.age = 0;    // Default values
        this.weight = 0.0;
        this.height = 0.0;
    }
}