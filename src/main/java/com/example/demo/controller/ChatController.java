package com.example.demo.controller;

import com.example.demo.service.ChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    @Autowired
    private ChatService chatService;

    @PostMapping("/message")
    public Map<String, String> sendMessage(@RequestBody Map<String, String> payload) {
        return Map.of("response", chatService.chat(payload.get("message")));
    }

    @GetMapping("/starters")
    public List<String> getStarters(@RequestParam(required = false) String context,
                                    @RequestParam(required = false) String bookTitle) {
        List<String> starters = new ArrayList<>();

        if ("book_details".equals(context) && bookTitle != null) {
            starters.add("Who is the author of " + bookTitle + "?");
            starters.add("What is the theme of " + bookTitle + "?");
            starters.add("Is " + bookTitle + " suitable for my reading level?");
        } else {
            starters.add("What is a book that I am most likely to enjoy from this list?");
            starters.add("Find a book by theme and author.");
            starters.add("What books would you recommend for a beginner?");
        }
        return starters;
    }
}