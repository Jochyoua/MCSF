package io.github.Jochyoua.MyChristianSwearFilter.signcheck;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class SignViewEvent extends Event implements Cancellable {
    private final Player player;

    private final Location signLocation;

    private String[] lines;

    private boolean modified;

    private boolean cancelled;

    public SignViewEvent(Player player, Location signLocation, String[] lines) {
        super(!Bukkit.isPrimaryThread());
        this.player = player;
        this.signLocation = signLocation;
        this.lines = lines;
        this.modified = false;
        this.cancelled = false;
    }

    public Player getPlayer() {
        return this.player;
    }

    public Location getSignLocation() {
        return this.signLocation.clone();
    }

    public String[] getLines() {
        return this.lines;
    }

    public void setLines(String[] lines) {
        this.modified = true;
        this.lines = lines;
    }

    public boolean isModified() {
        return this.modified;
    }

    public void setModified(boolean modified) {
        this.modified = modified;
    }

    public boolean isCancelled() {
        return this.cancelled;
    }

    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    public String getLine(int index) throws IndexOutOfBoundsException {
        return this.lines[index];
    }

    public void setLine(int index, String line) throws IndexOutOfBoundsException {
        if (line == null)
            line = "";
        if (line.equals(this.lines[index]))
            return;
        this.modified = true;
        this.lines[index] = line;
    }

    public HandlerList getHandlers() {
        return handlers;
    }

    private static final HandlerList handlers = new HandlerList();

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
