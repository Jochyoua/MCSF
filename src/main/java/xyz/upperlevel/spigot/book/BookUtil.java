package xyz.upperlevel.spigot.book;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Achievement;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.beans.ConstructorProperties;
import java.util.*;

@SuppressWarnings("ALL")
public final class BookUtil {
  private static final boolean canTranslateDirectly = false;
  
  public static void openPlayer(Player p, ItemStack book) {
    CustomBookOpenEvent event = new CustomBookOpenEvent(p, book, false);
    Bukkit.getPluginManager().callEvent(event);
    if (event.isCancelled())
      return; 
    p.closeInventory();
    ItemStack hand = p.getItemInHand();
    p.setItemInHand(event.getBook());
    p.updateInventory();
    NmsBookHelper.openBook(p, event.getBook(), (event.getHand() == CustomBookOpenEvent.Hand.OFF_HAND));
    p.setItemInHand(hand);
    p.updateInventory();
  }
  
  public static BookBuilder writtenBook() {
    return new BookBuilder(new ItemStack(Material.WRITTEN_BOOK));
  }
  
  public static class BookBuilder {
    private final BookMeta meta;
    
    private final ItemStack book;
    
    public BookBuilder(ItemStack book) {
      this.book = book;
      this.meta = (BookMeta)book.getItemMeta();
    }
    
    public BookBuilder title(String title) {
      this.meta.setTitle(title);
      return this;
    }
    
    public BookBuilder author(String author) {
      this.meta.setAuthor(author);
      return this;
    }
    
    public BookBuilder pagesRaw(String... pages) {
      this.meta.setPages(pages);
      return this;
    }
    
    public BookBuilder pagesRaw(List<String> pages) {
      this.meta.setPages(pages);
      return this;
    }
    
    public BookBuilder pages(BaseComponent[]... pages) {
      NmsBookHelper.setPages(this.meta, pages);
      return this;
    }
    
    public BookBuilder pages(List<BaseComponent[]> pages) {
      NmsBookHelper.setPages(this.meta, pages.<BaseComponent[]>toArray(new BaseComponent[0][]));
      return this;
    }
    
    public BookBuilder generation(BookMeta.Generation generation) {
      this.meta.setGeneration(generation);
      return this;
    }
    
    public ItemStack build() {
      this.book.setItemMeta((ItemMeta)this.meta);
      return this.book;
    }
  }



  public static interface ClickAction {
    ClickEvent.Action action();
    
    String value();
    
    static ClickAction runCommand(String command) {
      return new SimpleClickAction(ClickEvent.Action.RUN_COMMAND, command);
    }
    
    @Deprecated
    static ClickAction suggestCommand(String command) {
      return new SimpleClickAction(ClickEvent.Action.SUGGEST_COMMAND, command);
    }
    
    static ClickAction openUrl(String url) {
      if (url.startsWith("http://") || url.startsWith("https://"))
        return new SimpleClickAction(ClickEvent.Action.OPEN_URL, url); 
      throw new IllegalArgumentException("Invalid url: \"" + url + "\", it should start with http:// or https://");
    }
    
    static ClickAction changePage(int page) {
      return new SimpleClickAction(ClickEvent.Action.CHANGE_PAGE, Integer.toString(page));
    }
    
    public static class SimpleClickAction implements ClickAction {
      private final ClickEvent.Action action;
      
      private final String value;
      
      @ConstructorProperties({"action", "value"})
      public SimpleClickAction(ClickEvent.Action action, String value) {
        this.action = action;
        this.value = value;
      }
      
      public ClickEvent.Action action() {
        return this.action;
      }
      
      public String value() {
        return this.value;
      }
    }
  }
  
  public static interface HoverAction {
    HoverEvent.Action action();
    
    BaseComponent[] value();
    
    static HoverAction showText(BaseComponent... text) {
      return new SimpleHoverAction(HoverEvent.Action.SHOW_TEXT, text);
    }
    
    static HoverAction showText(String text) {
      return new SimpleHoverAction(HoverEvent.Action.SHOW_TEXT, new BaseComponent[] { (BaseComponent)new TextComponent(text) });
    }
    
    static HoverAction showItem(BaseComponent... item) {
      return new SimpleHoverAction(HoverEvent.Action.SHOW_ITEM, item);
    }
    
    static HoverAction showItem(ItemStack item) {
      return new SimpleHoverAction(HoverEvent.Action.SHOW_ITEM, NmsBookHelper.itemToComponents(item));
    }
    
    static HoverAction showEntity(BaseComponent... entity) {
      return new SimpleHoverAction(HoverEvent.Action.SHOW_ENTITY, entity);
    }
    
    static HoverAction showEntity(UUID uuid, String type, String name) {
      return new SimpleHoverAction(HoverEvent.Action.SHOW_ENTITY, 
          NmsBookHelper.jsonToComponents("{id:\"" + uuid + "\",type:\"" + type + "\"name:\"" + name + "\"}"));
    }
    
    static HoverAction showEntity(Entity entity) {
      return showEntity(entity.getUniqueId(), entity.getType().getName(), entity.getName());
    }
    
    static HoverAction showAchievement(String achievementId) {
      return new SimpleHoverAction(HoverEvent.Action.SHOW_ACHIEVEMENT, new BaseComponent[] { (BaseComponent)new TextComponent("achievement." + achievementId) });
    }
    
    static HoverAction showAchievement(Achievement achievement) {
      return showAchievement(AchievementUtil.toId(achievement));
    }
    
    static HoverAction showStatistic(String statisticId) {
      return new SimpleHoverAction(HoverEvent.Action.SHOW_ACHIEVEMENT, new BaseComponent[] { (BaseComponent)new TextComponent("statistic." + statisticId) });
    }
    
    public static class SimpleHoverAction implements HoverAction {
      private final HoverEvent.Action action;
      
      private final BaseComponent[] value;
      
      public HoverEvent.Action action() {
        return this.action;
      }
      
      public BaseComponent[] value() {
        return this.value;
      }
      
      public SimpleHoverAction(HoverEvent.Action action, BaseComponent... value) {
        this.action = action;
        this.value = value;
      }
    }
  }
}
