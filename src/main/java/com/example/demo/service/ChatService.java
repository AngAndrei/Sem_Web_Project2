package com.example.demo.service;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ChatService {

    @Autowired
    private RdfService rdfService;

    private EmbeddingStore<TextSegment> embeddingStore;
    private EmbeddingModel embeddingModel;
    private ChatLanguageModel chatModel;
    private ChatMemory chatMemory;

    private String currentUserContext = "";

    public ChatService() {
        this.embeddingStore = new InMemoryEmbeddingStore<>();
        this.embeddingModel = new AllMiniLmL6V2EmbeddingModel();
        this.chatMemory = MessageWindowChatMemory.withMaxMessages(10);

        this.chatModel = OllamaChatModel.builder()
                .baseUrl("http://localhost:11434")
                .modelName("llama3")
                .timeout(java.time.Duration.ofSeconds(60))
                .build();
    }

    @PostConstruct
    public void buildVectorDatabase() {
        this.embeddingStore = new InMemoryEmbeddingStore<>();
        List<Map<String, String>> books = rdfService.getAllBooks();

        for (Map<String, String> b : books) {
            Map<String, Object> details = rdfService.getBookDetails(b.get("uri"));
            StringBuilder segment = new StringBuilder();
            segment.append("Book Title: ").append(details.getOrDefault("hasTitle", "Unknown")).append(". ");
            segment.append("Author: ").append(details.getOrDefault("hasAuthor", "Unknown")).append(". ");
            segment.append("Theme: ").append(details.getOrDefault("hasTheme", "Unknown")).append(". ");
            segment.append("Level: ").append(details.getOrDefault("isSuitableForLevel", "Unknown")).append(". ");

            TextSegment textSegment = TextSegment.from(segment.toString());
            dev.langchain4j.data.embedding.Embedding embedding = embeddingModel.embed(textSegment).content();
            embeddingStore.add(embedding, textSegment);
        }

        if (!currentUserContext.isEmpty()) {
            TextSegment userSegment = TextSegment.from(currentUserContext);
            dev.langchain4j.data.embedding.Embedding embedding = embeddingModel.embed(userSegment).content();
            embeddingStore.add(embedding, userSegment);
        }
    }

    public void setCurrentUserPreferences(String name, String level, String theme) {
        this.currentUserContext = "Active User Profile: Name is " + name +
                ". Reading Level is " + level +
                ". Preferred Theme is " + theme + ".";
        buildVectorDatabase();
    }

    public String chat(String userMessage) {
        dev.langchain4j.data.embedding.Embedding queryEmbedding = embeddingModel.embed(userMessage).content();

        List<EmbeddingMatch<TextSegment>> relevant = embeddingStore.findRelevant(queryEmbedding, 10, 0.5);

        String contextInfo = relevant.stream()
                .map(match -> match.embedded().text())
                .collect(Collectors.joining("\n"));

        String systemPrompt = "You are a rigid library database assistant. " +
                "Your ONLY job is to answer based on the 'Context' below. " +
                "\n\nRULES:" +
                "\n1. You must ONLY recommend books listed in the Context that have a title." +
                "\n2. If the Context says 'Author: Gigel', you MUST say the author is Gigel, even if you know otherwise, same for the other properties." +
                "\n3. Do not use outside knowledge." +
                "\n4. Give the answers plainly, you MUST NOT mention anything about the context where you take your information from." +
                "\n5. Give up to three book recommendations." +
                "\n6. Always ask at the end of your response if the user wants something else from you." +
                "\n\nContext:\n" + contextInfo + "\n" + currentUserContext;

        chatMemory.add(dev.langchain4j.data.message.UserMessage.from(userMessage));

        dev.langchain4j.data.message.AiMessage response = chatModel.generate(
                dev.langchain4j.data.message.SystemMessage.from(systemPrompt),
                dev.langchain4j.data.message.UserMessage.from(userMessage)
        ).content();

        chatMemory.add(response);
        return response.text();
    }
}

