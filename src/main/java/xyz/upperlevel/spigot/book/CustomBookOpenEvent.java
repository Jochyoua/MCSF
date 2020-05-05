package xyz.upperlevel.spigot.book;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;

@SuppressWarnings("ALL")
public class CustomBookOpenEvent extends Event implements Cancellable {
  private static final HandlerList handlers = new HandlerList();
  
  private boolean cancelled;
  
  private final Player player;
  
  private Hand hand;
  
  private ItemStack book;
  
  public boolean isCancelled() {
    return this.cancelled;
  }
  
  public void setCancelled(boolean cancelled) {
    this.cancelled = cancelled;
  }
  
  public Player getPlayer() {
    return this.player;
  }
  
  public Hand getHand() {
    return this.hand;
  }
  
  public void setHand(Hand hand) {
    this.hand = hand;
  }
  
  public ItemStack getBook() {
    return this.book;
  }
  
  public void setBook(ItemStack book) {
    this.book = book;
  }
  
  public CustomBookOpenEvent(Player player, ItemStack book, boolean offHand) {
    this.player = player;
    this.book = book;
    this.hand = offHand ? Hand.OFF_HAND : Hand.MAIN_HAND;
  }
  
  public HandlerList getHandlers() {
    return handlers;
  }
  
  public static HandlerList getHandlerList() {
    return handlers;
  }
  
  public enum Hand {
    MAIN_HAND, OFF_HAND;
  }
}
