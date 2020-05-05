package xyz.upperlevel.spigot.book;

import java.util.EnumMap;
import java.util.Map;
import org.bukkit.Achievement;

@SuppressWarnings("ALL")
public final class AchievementUtil {
  private static final Map<Achievement, String> achievements = new EnumMap<Achievement, String>(Achievement.class) {
    
    };
  
  public static String toId(Achievement achievement) {
    return achievements.get(achievement);
  }
}
