package ph.edu.liceo.portal.model;

import java.time.LocalDateTime;

/** A message shown to one user (a bell entry with an unread flag). */
public class Notification {

    public enum Type { INFO, SUCCESS, WARNING, ALERT }

    private final int id;
    private final User recipient;
    private final String title;
    private final String message;
    private final Type type;
    private final LocalDateTime createdAt;
    private boolean read;

    public Notification(int id, User recipient, String title, String message, Type type) {
        this.id = id;
        this.recipient = recipient;
        this.title = title;
        this.message = message;
        this.type = type == null ? Type.INFO : type;
        this.createdAt = LocalDateTime.now();
        this.read = false;
    }

    public int getId() {
        return id;
    }

    public User getRecipient() {
        return recipient;
    }

    public String getTitle() {
        return title;
    }

    public String getMessage() {
        return message;
    }

    public Type getType() {
        return type;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }
}
