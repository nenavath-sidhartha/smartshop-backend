package com.smartshop.backend.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HomeController {

    @GetMapping("/")
    public String home() {
        return "SmartShop Backend is Running!";
    }

    @GetMapping("/api/test")
    public String test() {
        return "SmartShop API is Working!";
    }
}