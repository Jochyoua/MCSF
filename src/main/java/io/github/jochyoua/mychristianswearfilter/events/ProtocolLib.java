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
import io.github.jochyoua.mychristianswearfilter.shared.Utils;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

import java.util.Objects;
import java.util.UUID;
import java.util.logging.Level;

public class ProtocolLib implements Listener {
    static boolean value = true;

    public ProtocolLib(Utils utils) {
        if (!utils.getProvider().getConfig().getBoolean("settings.only filter players.enabled")) {
            ProtocolLibrary.getProtocolManager().addPacketListener(new PacketAdapter(utils.getProvider(), PacketType.Play.Server.CHAT) {
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
                                    if (new User(utils, ID).status() || utils.getProvider().getConfig().getBoolean("settings.filtering.force"))
                                        string = utils.clean(component.getJson(), false, true, utils.getBoth(), Types.Filters.ALL);
                                    else
                                        string = utils.clean(component.getJson(), false, true, utils.getGlobalRegex(), Types.Filters.ALL);
                                    if (string == null) {
                                        return;
                                    }
                                    component.setJson(string);
                                    chatComponents.writeSafely(0, component);
                                }
                            }
                        }
                        if (!isEnabled())
                            setEnabled(true);
                    } catch (Exception e) {
                        FileConfiguration language = utils.getProvider().getLanguage();
                        utils.getProvider().getLogger().log(Level.SEVERE, "Failure: {message}"
                                .replaceAll("(?i)\\{message}|(?i)%message%",
                                        language.getString("variables.error.execute_failure")
                                                .replaceAll("(?i)\\{feature}", "Chat Filtering (FULL CHAT)")), e);
                        utils.getProvider().getLogger().log(Level.INFO, "Failure: {message}".replaceAll("(?i)\\{message}", Objects.requireNonNull(language.getString("variables.error.execute_failure_link"))) + "\nonly filter players has been temporarily enabled.");
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
