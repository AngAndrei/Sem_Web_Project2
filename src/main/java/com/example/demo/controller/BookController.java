package com.example.demo.controller;

import com.example.demo.service.ChatService;
import com.example.demo.service.RdfService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;

@Controller
public class BookController {

    @Autowired
    private RdfService rdfService;

    @Autowired
    private ChatService chatService;

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @PostMapping("/upload")
    public String uploadFile(@RequestParam("file") MultipartFile file, Model model) {
        try {
            rdfService.loadRdf(file.getInputStream());
            chatService.buildVectorDatabase();
            model.addAttribute("message", "File uploaded successfully!");
            model.addAttribute("graphData", rdfService.getGraphData());
        } catch (IOException e) {
            e.printStackTrace();
            model.addAttribute("message", "Failed to upload file.");
        }
        return "visualize";
    }

    @PostMapping("/setPreferences")
    public String setPreferences(@RequestParam String userName,
                                 @RequestParam String userLevel,
                                 @RequestParam String userTheme) {
        rdfService.addUser(userName, userLevel, userTheme);
        chatService.setCurrentUserPreferences(userName, userLevel, userTheme);
        return "redirect:/books";
    }

    @PostMapping("/addBook")
    public String addBook(@RequestParam String title,
                          @RequestParam String theme,
                          @RequestParam String level,
                          @RequestParam String author) {
        rdfService.addBook(title, theme, level, author);
        chatService.buildVectorDatabase();
        return "redirect:/books";
    }

    @PostMapping("/modifyBook")
    public String modifyBook(@RequestParam String title, @RequestParam String level) {
        rdfService.modifyBookLevel(title, level);
        chatService.buildVectorDatabase();
        return "redirect:/books";
    }

    @GetMapping("/books")
    public String listBooks(Model model) {
        model.addAttribute("books", rdfService.getAllBooks());
        return "books";
    }

    @GetMapping("/book")
    public String bookDetails(@RequestParam String uri, Model model) {
        model.addAttribute("book", rdfService.getBookDetails(uri));
        return "book_details";
    }
}