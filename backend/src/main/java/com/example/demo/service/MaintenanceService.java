package com.example.demo.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/** Performs expiry work even when no user is currently making a related request. */
@Service
public class MaintenanceService {
    private final ChatService chats;
    private final QueuePresenceService queuePresence;

    public MaintenanceService(ChatService chatService, QueuePresenceService queuePresenceService) {
        this.chats = chatService;
        this.queuePresence = queuePresenceService;
    }

    @Scheduled(fixedDelay = 60_000)
    public void removeExpiredState() {
        queuePresence.removeExpiredPresences();
        chats.removeExpiredChats();
    }
}
