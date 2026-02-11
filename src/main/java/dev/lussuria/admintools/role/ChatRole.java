package dev.lussuria.admintools.role;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class ChatRole {
    private UUID id;
    private String name;
    private String displayName;
    private String color;
    private boolean bold;
    private boolean italic;
    private int priority;
    private final List<String> groups;

    public ChatRole() {
        this.id = UUID.randomUUID();
        this.name = "";
        this.displayName = "";
        this.color = "#ffffff";
        this.bold = false;
        this.italic = false;
        this.priority = 100;
        this.groups = new ArrayList<>();
    }

    public ChatRole(String name, String displayName, String color, int priority) {
        this.id = UUID.randomUUID();
        this.name = name;
        this.displayName = displayName;
        this.color = color;
        this.bold = false;
        this.italic = false;
        this.priority = priority;
        this.groups = new ArrayList<>();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public boolean isBold() {
        return bold;
    }

    public void setBold(boolean bold) {
        this.bold = bold;
    }

    public boolean isItalic() {
        return italic;
    }

    public void setItalic(boolean italic) {
        this.italic = italic;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public List<String> getGroups() {
        return groups;
    }
}
