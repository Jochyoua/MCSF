package com.github.Jochyoua.MCSF.events;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.reflect.StructureModifier;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.github.Jochyoua.MCSF.MCSF;
import com.github.Jochyoua.MCSF.shared.Types;
import com.github.Jochyoua.MCSF.shared.Utils;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

import java.util.Objects;
import java.util.UUID;
import java.util.logging.Level;

public class ProtocolLib implements Listener {
    static boolean value = true;
    private static void setEnabled(boolean val){
        value = val;
    }
    public static boolean isEnabled(){
        return value;
    }
    public ProtocolLib(MCSF mcsf, Utils utils) {
        ProtocolLibrary.getProtocolManager().addPacketListener(new PacketAdapter(mcsf, PacketType.Play.Server.CHAT) {
            @Override
            public void onPacketSending(PacketEvent event) {
                try {
                    Player player = event.getPlayer();
                    UUID ID = player.getUniqueId();
                    PacketContainer packet = event.getPacket();
                    StructureModifier<WrappedChatComponent> chatComponents = packet.getChatComponents();
                    for (WrappedChatComponent component : chatComponents.getValues()) {
                        if (plugin.getConfig().getBoolean("settings.filtering.force") || utils.status(ID)) {
                            plugin.reloadConfig();
                            if (component != null) {
                                if (!component.getJson().isEmpty()) {
                                    if (!utils.isclean(component.getJson())) {
                                        String string = utils.clean(component.getJson(), false, true, Types.Filters.ALL);
                                        if (string == null) {
                                            return;
                                        }
                                        component.setJson(string);
                                        chatComponents.write(0, component);
                                    }
                                }
                            }
                        }
                    }
                    if(!isEnabled())
                        setEnabled(true);
                } catch (Exception e) {
                    FileConfiguration language = mcsf.getLanguage();
                    plugin.getLogger().log(Level.SEVERE, "Failure: {message}"
                            .replaceAll("(?i)\\{message}|(?i)%message%",
                                    Objects.requireNonNull(language.getString("variables.error.execute_failure"))
                                            .replaceAll("(?i)\\{feature}", "Chat Filtering (FULL CHAT)")), e);
                    plugin.getLogger().log(Level.INFO, "Failure: {message}".replaceAll("(?i)\\{message}", Objects.requireNonNull(language.getString("variables.error.execute_failure_link")))+"\nonly filter players has been temporarily enabled.");
                    setEnabled(false);
                    ProtocolLibrary.getProtocolManager().removePacketListener(this);
                }
            }
        });
    }
}
