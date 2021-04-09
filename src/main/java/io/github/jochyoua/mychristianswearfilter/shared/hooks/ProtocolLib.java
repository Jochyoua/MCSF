package io.github.jochyoua.mychristianswearfilter.shared.hooks;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.reflect.StructureModifier;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import io.github.jochyoua.mychristianswearfilter.MCSF;
import io.github.jochyoua.mychristianswearfilter.shared.Data;
import io.github.jochyoua.mychristianswearfilter.shared.Manager;
import io.github.jochyoua.mychristianswearfilter.shared.User;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.Listener;

import java.util.Objects;
import java.util.logging.Level;

public class ProtocolLib implements Listener {
    private final MCSF plugin;
    @Getter
    @Setter
    private boolean enabled = true;

    public ProtocolLib(MCSF plugin) {
        this.plugin = plugin;
    }

    public void register() {
        Manager manager = plugin.getManager();
        ProtocolManager protocolManager = ProtocolLibrary.getProtocolManager();
        protocolManager.addPacketListener(new PacketAdapter(plugin, ListenerPriority.MONITOR, PacketType.Play.Server.CHAT) {
            @Override
            public void onPacketSending(PacketEvent e) {
                if (!plugin.getConfig().getBoolean("settings.only filter players.enabled")) {
                    try {
                        User user = new User(manager, e.getPlayer().getUniqueId());
                        PacketContainer packet = e.getPacket();
                        StructureModifier<WrappedChatComponent> chatComponents = packet.getChatComponents();
                        for (WrappedChatComponent component : chatComponents.getValues()) {
                            if (component != null) {
                                if (!component.getJson().isEmpty()) {
                                    String string = manager.clean(component.getJson(), false, user.status() ? manager.reloadPattern(Data.Filters.BOTH) : manager.reloadPattern(Data.Filters.GLOBAL), Data.Filters.ALL);
                                    if (!string.trim().isEmpty()) {
                                        component.setJson(string);
                                        packet.getChatComponents().write(0, component);
                                    }
                                }
                            }
                        }
                    } catch (Exception ex) {
                        FileConfiguration language = manager.getPlugin().getLanguage();
                        manager.getPlugin().getLogger().log(Level.SEVERE, "Failure: {message}"
                                .replaceAll("(?i)\\{message}|(?i)%message%",
                                        Objects.requireNonNull(language.getString("variables.error.execute_failure"))
                                                .replaceAll("(?i)\\{feature}", "Chat Filtering (FULL CHAT)")), e);
                        setEnabled(false);
                        ex.printStackTrace();
                        manager.getPlugin().getLogger().warning("Failure: {message}".replaceAll("(?i)\\{message}", Objects.requireNonNull(language.getString("variables.error.execute_failure_link"))) + "\nonly filter players has been temporarily enabled.");
                        Manager.debug("Failed to load ProtocolLib!: " + ex.getMessage(), false, Level.WARNING);
                        ProtocolLibrary.getProtocolManager().removePacketListener(this);
                    }
                }
            }
        });
    }
}
