package io.github.jochyoua.mychristianswearfilter.shared.hooks;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.reflect.StructureModifier;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import io.github.jochyoua.mychristianswearfilter.MCSF;
import io.github.jochyoua.mychristianswearfilter.shared.Manager;
import io.github.jochyoua.mychristianswearfilter.shared.Types;
import io.github.jochyoua.mychristianswearfilter.shared.User;
import lombok.Getter;
import lombok.Setter;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.chat.ComponentSerializer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

import java.util.Objects;
import java.util.UUID;
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
        ProtocolLibrary.getProtocolManager().addPacketListener(new PacketAdapter(plugin, PacketType.Play.Server.CHAT) {
            @Override
            public void onPacketSending(PacketEvent event) {
                if (plugin.getConfig().getBoolean("settings.only filter players.enabled")) {
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
                                        if (new User(manager, ID).status() || manager.getProvider().getConfig().getBoolean("settings.filtering.force")) {
                                            string = manager.clean(message, false, true, manager.reloadPattern(Types.Filters.BOTH), Types.Filters.ALL);
                                        } else
                                            string = manager.clean(message, false, true, manager.reloadPattern(Types.Filters.GLOBAL), Types.Filters.ALL);
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
                    } catch (Exception e) {
                        FileConfiguration language = manager.getProvider().getLanguage();
                        manager.getProvider().getLogger().log(Level.SEVERE, "Failure: {message}"
                                .replaceAll("(?i)\\{message}|(?i)%message%",
                                        language.getString("variables.error.execute_failure")
                                                .replaceAll("(?i)\\{feature}", "Chat Filtering (FULL CHAT)")), e);
                        setEnabled(false);
                        manager.getProvider().getLogger().log(Level.INFO, "Failure: {message}".replaceAll("(?i)\\{message}", Objects.requireNonNull(language.getString("variables.error.execute_failure_link"))) + "\nonly filter players has been temporarily enabled.");
                        ProtocolLibrary.getProtocolManager().removePacketListener(this);
                    }
                }
            }
        });
    }
}
