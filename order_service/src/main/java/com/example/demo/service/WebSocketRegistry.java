package com.example.demo.service;

import com.example.demo.model.ClientConnection;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class WebSocketRegistry {

    private final Map<String, ClientConnection> clients =
            new ConcurrentHashMap<>();

    public void add(String username, ClientConnection connection) {
        clients.put(username, connection);
    }

    public void remove(String username) {
        clients.remove(username);
    }

    public ClientConnection get(String username) {
        return clients.get(username);
    }
}
