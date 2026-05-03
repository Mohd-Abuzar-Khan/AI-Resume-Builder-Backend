package com.resumade.auth.dto;

public class NotificationEvent {
    private Integer userId;
    private String recipientEmail;
    private String type;
    private String title;
    private String message;
    private String channel;

    public NotificationEvent() {}

    public NotificationEvent(Integer userId, String recipientEmail, String type, String title, String message, String channel) {
        this.userId = userId;
        this.recipientEmail = recipientEmail;
        this.type = type;
        this.title = title;
        this.message = message;
        this.channel = channel;
    }

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }

    public String getRecipientEmail() { return recipientEmail; }
    public void setRecipientEmail(String recipientEmail) { this.recipientEmail = recipientEmail; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }
}
