package com.example.backend.controller.test

import org.springframework.web.bind.annotation.*

@RestController
@CrossOrigin("*")
@RequestMapping("/api/test")
class TestController() {

    @GetMapping
    fun testCookie(@CookieValue name: String): String {
        println("TEST COOKIE = $name")
        return name
    }

}