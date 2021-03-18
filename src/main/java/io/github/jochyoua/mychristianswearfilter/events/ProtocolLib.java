package io.github.jochyoua.mychristianswearfilter.events;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.reflect.StructureModifier;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import io.github.jochyoua.mychristianswearfilter.shared.Types;
import io.github.jochyoua.mychristianswearfilter.shared.User;
import io.github.jochyoua.mychristianswearfilter.shared.Manager;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.chat.ComponentSerializer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

import java.util.Objects;
import java.util.UUID;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ProtocolLib implements Listener {
    static boolean value = true;

    public ProtocolLib(Manager manager) {
        if (!manager.getProvider().getConfig().getBoolean("settings.only filter players.enabled")) {
            ProtocolLibrary.getProtocolManager().addPacketListener(new PacketAdapter(manager.getProvider(), PacketType.Play.Server.CHAT) {
                @Override
                public void onPacketSending(PacketEvent event) {
                    try {
                        Player player = event.getPlayer();
                        UUID ID = player.getUniqueId();
                        PacketContainer packet = event.getPacket();
                        StructureModifier<WrappedChatComponent> chatComponents = packet.getChatComponents();
                        for (WrappedChatComponent component : chatComponents.getValues()) {
                            if (component != null) {
                                if (!component.getJson().isEmpty()) {
                                    String string;
                                    String message = BaseComponent.toLegacyText(ComponentSerializer.parse(component.getJson()));
                                    if (!(message.trim().length() <= 4)) {
                                        if (new User(manager, ID).status() || manager.getProvider().getConfig().getBoolean("settings.filtering.force"))
                                            string = manager.clean(message, false, true, Stream.of(manager.getRegex(), manager.getGlobalRegex()).collect(Collectors.toList()).get(0), Types.Filters.ALL);
                                        else
                                            string = manager.clean(message, false, true, manager.getGlobalRegex(), Types.Filters.ALL);
                                        if (string == null || string.isEmpty()) {
                                            return;
                                        }
                                        if (!string.trim().isEmpty()) {
                                            String json = Manager.JSONUtil.toJSON(string);
                                            component.setJson(json);
                                            chatComponents.writeSafely(0, component);
                                        }
                                    }
                                }
                            }
                        }
                        if (!isEnabled())
                            setEnabled(true);
                    } catch (Exception e) {
                        FileConfiguration language = manager.getProvider().getLanguage();
                        manager.getProvider().getLogger().log(Level.SEVERE, "Failure: {message}"
                                .replaceAll("(?i)\\{message}|(?i)%message%",
                                        language.getString("variables.error.execute_failure")
                                                .replaceAll("(?i)\\{feature}", "Chat Filtering (FULL CHAT)")), e);
                        manager.getProvider().getLogger().log(Level.INFO, "Failure: {message}".replaceAll("(?i)\\{message}", Objects.requireNonNull(language.getString("variables.error.execute_failure_link"))) + "\nonly filter players has been temporarily enabled.");
                        setEnabled(false);
                        ProtocolLibrary.getProtocolManager().removePacketListener(this);
                    }
                }
            });
        }
    }

    public static boolean isEnabled() {
        return value;
    }

    private static void setEnabled(boolean val) {
        value = val;
    }
}
