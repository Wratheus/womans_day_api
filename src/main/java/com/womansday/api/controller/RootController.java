package com.womansday.api.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class RootController {

    @GetMapping("/status")
    public ResponseEntity<Map<String, String>> status() {
        return ResponseEntity.ok(Map.of(
                "name", "WomansDay API",
                "status", "running"
        ));
    }

    @GetMapping(value = "/", produces = MediaType.TEXT_HTML_VALUE)
    public String html() {
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <title>WomansDay API</title>
                </head>
                <body style="font-family:sans-serif;text-align:center;margin-top:50px">
                    <h1>🌸 WomansDay API</h1>
                    <p>Status: <b style="color:green">running</b></p>
                    <p><a href="/api/brew-coffee">☕ Brew coffee</a></p>
                    <p><a href="/api/status">JSON status</a></p>
                </body>
                </html>
                """;
    }

    @SuppressWarnings("null")
    @GetMapping(value = "/brew-coffee", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> makeCoffee() {

        boolean isTeapot = true;

        if (isTeapot) {
            String html = """
                    <!DOCTYPE html>
                    <html>
                    <head>
                        <title>418 I'm a teapot</title>
                    </head>
                    <body style="font-family:sans-serif;text-align:center;margin-top:50px">
                        <h1>🫖 418 I'm a teapot</h1>
                        <p>Я чайник, я не могу варить кофе!</p>
                        <a href="/api/">← назад</a>
                    </body>
                    </html>
                    """;

            return ResponseEntity
                    .status(418)
                    .contentType(MediaType.TEXT_HTML)
                    .body(html);
        }

        String html = """
                <!DOCTYPE html>
                <html>
                <head>
                    <title>Coffee Ready</title>
                </head>
                <body style="font-family:sans-serif;text-align:center;margin-top:50px">
                    <h1>☕ Кофе готов!</h1>
                    <a href="/api/">← назад</a>
                </body>
                </html>
                """;

        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(html);
    }
}