package ph.edu.teleconsultation.system.portal.service;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import ph.edu.teleconsultation.system.portal.data.DataStore;
import ph.edu.teleconsultation.system.portal.model.AccountStatus;
import ph.edu.teleconsultation.system.portal.model.Notification;
import ph.edu.teleconsultation.system.portal.model.User;

/**
 * Creates and reads notifications. Every user has a stack, newest on top,
 * capped so an inbox can never grow without limit.
 */
public class NotificationService {

    public static final int MAX_PER_USER = 100;

    /** Told about every new notification (the UI uses this to pop it up on screen). */
    private static final List<Consumer<Notification>> LISTENERS = new CopyOnWriteArrayList<>();

    public static void addListener(Consumer<Notification> listener) {
        LISTENERS.add(listener);
    }

    public Notification notifyUser(User user, String title, String message,
                                   Notification.Type type) {

        if (user == null) {
            return null;
        }

        Notification notification = new Notification(
                DataStore.getNextNotificationId(), user, title, message, type);

        ArrayDeque<Notification> stack =
                DataStore.notifications.computeIfAbsent(user, key -> new ArrayDeque<>());

        stack.push(notification);

        while (stack.size() > MAX_PER_USER) {
            stack.removeLast();
        }

        for (Consumer<Notification> listener : LISTENERS) {
            try {
                listener.accept(notification);
            } catch (RuntimeException e) {
                // A pop-up problem must never break the action that created the notification.
                e.printStackTrace();
            }
        }

        return notification;
    }

    /** Appointments and records refer to people by name, so look the user up by name. */
    public Notification notifyByName(String fullName, String title, String message,
                                     Notification.Type type) {

        return notifyUser(findByName(fullName), title, message, type);
    }

    /** Sends the same notification to every active user of one role. */
    public int notifyRole(Class<? extends User> role, String title, String message,
                          Notification.Type type) {

        int sent = 0;

        for (User user : DataStore.users) {

            if (role.isInstance(user) && user.getStatus() == AccountStatus.ACTIVE) {
                notifyUser(user, title, message, type);
                sent++;
            }
        }

        return sent;
    }

    /** Newest first. */
    public ArrayList<Notification> getNotifications(User user) {

        ArrayDeque<Notification> stack = DataStore.notifications.get(user);

        return stack == null ? new ArrayList<>() : new ArrayList<>(stack);
    }

    public int getUnreadCount(User user) {

        ArrayDeque<Notification> stack = DataStore.notifications.get(user);
        int unread = 0;

        if (stack != null) {
            for (Notification n : stack) {
                if (!n.isRead()) {
                    unread++;
                }
            }
        }

        return unread;
    }

    public void markRead(Notification notification) {

        if (notification != null) {
            notification.setRead(true);
        }
    }

    public void markAllRead(User user) {

        ArrayDeque<Notification> stack = DataStore.notifications.get(user);

        if (stack != null) {
            for (Notification n : stack) {
                n.setRead(true);
            }
        }
    }

    public void delete(User user, Notification notification) {

        ArrayDeque<Notification> stack = DataStore.notifications.get(user);

        if (stack != null) {
            stack.remove(notification);
        }
    }

    public void clearAll(User user) {
        DataStore.notifications.remove(user);
    }

    /** Gives every active account without notifications a welcome message (demo accounts). */
    public void seedWelcome() {

        for (User user : DataStore.users) {

            if (user.getStatus() == AccountStatus.ACTIVE
                    && !DataStore.notifications.containsKey(user)) {
                welcome(user);
            }
        }
    }

    public void welcome(User user) {
        notifyUser(user, "Welcome to TeleConsult",
                "Hi " + user.getFullName() + "! Updates about your appointments, "
                        + "consultations and records will appear here.",
                Notification.Type.INFO);
    }

    private User findByName(String fullName) {

        if (fullName == null) {
            return null;
        }

        for (User user : DataStore.users) {

            if (user.getFullName().trim().equalsIgnoreCase(fullName.trim())) {
                return user;
            }
        }

        return null;
    }
}
