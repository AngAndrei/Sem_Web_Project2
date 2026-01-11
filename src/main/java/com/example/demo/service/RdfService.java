package com.example.demo.service;

import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.RDF;
import org.springframework.stereotype.Service;
import java.io.*;
import java.util.*;

@Service
public class RdfService {
    private Model model;
    private final String NS = "http://example.org/bookrec#";

    public RdfService() {
        model = ModelFactory.createDefaultModel();
    }

    public void loadRdf(InputStream in) {
        model.read(in, null);
    }

    public List<Map<String, String>> getGraphData() {
        List<Map<String, String>> edges = new ArrayList<>();
        StmtIterator iter = model.listStatements();
        while (iter.hasNext()) {
            Statement stmt = iter.nextStatement();
            Map<String, String> edge = new HashMap<>();
            edge.put("subject", stmt.getSubject().toString());
            edge.put("predicate", stmt.getPredicate().getLocalName());
            edge.put("object", stmt.getObject().toString());
            edges.add(edge);
        }
        return edges;
    }

    public void addBook(String title, String theme, String level, String author) {
        String bookUri = NS + title.replaceAll(" ", "");
        Resource book = model.createResource(bookUri);

        Property hasTitle = model.createProperty(NS + "hasTitle");
        Property hasTheme = model.createProperty(NS + "hasTheme");
        Property suitableFor = model.createProperty(NS + "isSuitableForLevel");
        Property hasAuthor = model.createProperty(NS + "hasAuthor");
        Resource typeBook = model.createResource(NS + "Book");

        book.addProperty(RDF.type, typeBook);
        book.addProperty(hasTitle, title);
        book.addProperty(hasTheme, theme);
        book.addProperty(suitableFor, level);
        book.addProperty(hasAuthor, author);
    }

    public void addUser(String name, String level, String theme) {
        String userUri = NS + name.replaceAll(" ", "");
        Resource user = model.createResource(userUri);

        Property hasReadingLevel = model.createProperty(NS + "hasReadingLevel");
        Property prefersTheme = model.createProperty(NS + "prefersTheme");
        Resource typeUser = model.createResource(NS + "User");

        user.addProperty(RDF.type, typeUser);
        user.addProperty(hasReadingLevel, level);
        user.addProperty(prefersTheme, theme);
    }

    public void modifyBookLevel(String title, String newLevel) {
        String bookUri = NS + title.replaceAll(" ", "");
        Resource book = model.getResource(bookUri);
        Property suitableFor = model.createProperty(NS + "isSuitableForLevel");

        if (model.containsResource(book)) {
            model.removeAll(book, suitableFor, null);
            book.addProperty(suitableFor, newLevel);
        }
    }

    public List<Map<String, String>> getAllBooks() {
        List<Map<String, String>> books = new ArrayList<>();
        Resource typeBook = model.createResource(NS + "Book");
        ResIterator iter = model.listSubjectsWithProperty(RDF.type, typeBook);

        Property hasTitle = model.createProperty(NS + "hasTitle");
        Property hasAuthor = model.createProperty(NS + "hasAuthor");

        while (iter.hasNext()) {
            Resource r = iter.nextResource();
            Map<String, String> book = new HashMap<>();
            book.put("uri", r.getURI());

            if (r.hasProperty(hasTitle)) {
                book.put("title", r.getProperty(hasTitle).getString());
            } else {
                book.put("title", r.getLocalName());
            }

            if (r.hasProperty(hasAuthor)) {
                book.put("author", r.getProperty(hasAuthor).getString());
            } else {
                book.put("author", "Unknown");
            }

            books.add(book);
        }
        return books;
    }

    public Map<String, Object> getBookDetails(String uri) {
        Resource book = model.getResource(uri);
        Map<String, Object> details = new HashMap<>();
        details.put("uri", book.getURI());

        StmtIterator iter = book.listProperties();
        while(iter.hasNext()) {
            Statement s = iter.nextStatement();
            String pred = s.getPredicate().getLocalName();
            String obj = s.getObject().toString();

            if (details.containsKey(pred)) {
                Object existing = details.get(pred);
                if (existing instanceof List) {
                    ((List) existing).add(obj);
                } else {
                    List<String> list = new ArrayList<>();
                    list.add((String)existing);
                    list.add(obj);
                    details.put(pred, list);
                }
            } else {
                details.put(pred, obj);
            }
        }
        return details;
    }
}