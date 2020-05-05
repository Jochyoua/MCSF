package xyz.upperlevel.spigot.book;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.chat.ComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

@SuppressWarnings("ALL")
public final class NmsBookHelper {
  private static int major, minor;
  
  private static final String version = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];
  
  private static final boolean doubleHands;
  
  private static final Class<?> craftMetaBookClass;
  
  private static final Field craftMetaBookField;
  
  private static final Method chatSerializerA;
  
  private static final Method craftPlayerGetHandle;
  
  private static final Method entityPlayerOpenBook;
  
  private static final Object[] hands;
  
  private static final Method nmsItemStackSave;
  
  private static final Constructor<?> nbtTagCompoundConstructor;
  
  private static final Method craftItemStackAsNMSCopy;
  
  static {
    Pattern pattern = Pattern.compile("v([0-9]+)_([0-9]+)");
    Matcher m = pattern.matcher(version);
    if (m.find()) {
      major = Integer.parseInt(m.group(1));
      minor = Integer.parseInt(m.group(2));
    } else {
      throw new IllegalStateException("Cannot parse version \"" + version + "\", make sure it follows \"v<major>_<minor>...\"");
    } 
    doubleHands = (major <= 1 && minor >= 9);
    try {
      craftMetaBookClass = getCraftClass("inventory.CraftMetaBook");
      craftMetaBookField = craftMetaBookClass.getDeclaredField("pages");
      craftMetaBookField.setAccessible(true);
      Class<?> chatSerializer = getNmsClass("IChatBaseComponent$ChatSerializer", false);
      if (chatSerializer == null)
        chatSerializer = getNmsClass("ChatSerializer"); 
      chatSerializerA = chatSerializer.getDeclaredMethod("a", new Class[] { String.class });
      Class<?> craftPlayerClass = getCraftClass("entity.CraftPlayer");
      craftPlayerGetHandle = craftPlayerClass.getMethod("getHandle", new Class[0]);
      Class<?> entityPlayerClass = getNmsClass("EntityPlayer");
      Class<?> itemStackClass = getNmsClass("ItemStack");
      if (doubleHands) {
        Method openBookMethod;
        Class<?> enumHandClass = getNmsClass("EnumHand");
        try {
          openBookMethod = entityPlayerClass.getMethod("a", new Class[] { itemStackClass, enumHandClass });
        } catch (NoSuchMethodException e) {
          openBookMethod = entityPlayerClass.getMethod("openBook", new Class[] { itemStackClass, enumHandClass });
        } 
        entityPlayerOpenBook = openBookMethod;
        hands = enumHandClass.getEnumConstants();
      } else {
        entityPlayerOpenBook = entityPlayerClass.getMethod("openBook", new Class[] { itemStackClass });
        hands = null;
      } 
      Class<?> craftItemStackClass = getCraftClass("inventory.CraftItemStack");
      craftItemStackAsNMSCopy = craftItemStackClass.getMethod("asNMSCopy", new Class[] { ItemStack.class });
      Class<?> nmsItemStackClazz = getNmsClass("ItemStack");
      Class<?> nbtTagCompoundClazz = getNmsClass("NBTTagCompound");
      nmsItemStackSave = nmsItemStackClazz.getMethod("save", new Class[] { nbtTagCompoundClazz });
      nbtTagCompoundConstructor = nbtTagCompoundClazz.getConstructor(new Class[0]);
    } catch (Exception e) {
      throw new IllegalStateException("Cannot initiate reflections for " + version, e);
    } 
  }
  
  public static void setPages(BookMeta meta, BaseComponent[][] components) {
    try {
      List<Object> pages = (List<Object>)craftMetaBookField.get(meta);
      pages.clear();
      for (BaseComponent[] c : components) {
        String json = ComponentSerializer.toString(c);
        pages.add(chatSerializerA.invoke(null, new Object[] { json }));
      } 
    } catch (Exception e) {
      throw new UnsupportedVersionException(e);
    } 
  }
  
  public static void openBook(Player player, ItemStack book, boolean offHand) {
    try {
      if (doubleHands) {
        entityPlayerOpenBook.invoke(toNms(player), new Object[] { nmsCopy(book), hands[offHand ? 1 : 0] });
      } else {
        entityPlayerOpenBook.invoke(toNms(player), new Object[] { nmsCopy(book) });
      } 
    } catch (Exception e) {
      throw new UnsupportedVersionException(e);
    } 
  }
  
  public static BaseComponent[] itemToComponents(ItemStack item) {
    return jsonToComponents(itemToJson(item));
  }
  
  public static BaseComponent[] jsonToComponents(String json) {
    return new BaseComponent[] { (BaseComponent)new TextComponent(json) };
  }
  
  private static String itemToJson(ItemStack item) {
    try {
      Object nmsItemStack = nmsCopy(item);
      Object emptyTag = nbtTagCompoundConstructor.newInstance(new Object[0]);
      Object json = nmsItemStackSave.invoke(nmsItemStack, new Object[] { emptyTag });
      return json.toString();
    } catch (Exception e) {
      throw new UnsupportedVersionException(e);
    } 
  }
  
  public static class UnsupportedVersionException extends RuntimeException {
    public String getVersion() {
      return this.version;
    }
    
    private final String version = NmsBookHelper.version;
    
    public UnsupportedVersionException(Exception e) {
      super("Error while executing reflections, submit to developers the following log (version: " + NmsBookHelper
          .version + ")", e);
    }
  }
  
  public static Object toNms(Player player) throws InvocationTargetException, IllegalAccessException {
    return craftPlayerGetHandle.invoke(player, new Object[0]);
  }
  
  public static Object nmsCopy(ItemStack item) throws InvocationTargetException, IllegalAccessException {
    return craftItemStackAsNMSCopy.invoke(null, new Object[] { item });
  }
  
  public static Class<?> getNmsClass(String className, boolean required) {
    try {
      return Class.forName("net.minecraft.server." + version + "." + className);
    } catch (ClassNotFoundException e) {
      if (required)
        throw new RuntimeException("Cannot find NMS class " + className, e); 
      return null;
    } 
  }
  
  public static Class<?> getNmsClass(String className) {
    return getNmsClass(className, false);
  }
  
  private static Class<?> getCraftClass(String path) {
    try {
      return Class.forName("org.bukkit.craftbukkit." + version + "." + path);
    } catch (ClassNotFoundException e) {
      throw new RuntimeException("Cannot find CraftBukkit class at path: " + path, e);
    } 
  }
}
