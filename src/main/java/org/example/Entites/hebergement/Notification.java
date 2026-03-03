package org.example.Entites.hebergement;

import java.time.LocalDateTime;

public class Notification {
    private int id;
    private int userId;
    private String message;
    private boolean lue;
    private LocalDateTime dateCreation;

    public Notification(int userId, String message) {
        this.userId = userId;
        this.message = message;
        this.lue = false;
        this.dateCreation = LocalDateTime.now();
    }

    public int getId()                          { return id; }
    public void setId(int id)                   { this.id = id; }
    public int getUserId()                      { return userId; }
    public String getMessage()                  { return message; }
    public boolean isLue()                      { return lue; }
    public void setLue(boolean lue)             { this.lue = lue; }
    public LocalDateTime getDateCreation()      { return dateCreation; }
    public void setDateCreation(LocalDateTime d){ this.dateCreation = d; }
}