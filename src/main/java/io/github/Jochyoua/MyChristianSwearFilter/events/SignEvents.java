package io.github.Jochyoua.MyChristianSwearFilter.events;

import io.github.Jochyoua.MyChristianSwearFilter.MCSF;
import io.github.Jochyoua.MyChristianSwearFilter.shared.Types;
import io.github.Jochyoua.MyChristianSwearFilter.shared.Utils;
import io.github.Jochyoua.MyChristianSwearFilter.signcheck.ProtocolUtils;
import io.github.Jochyoua.MyChristianSwearFilter.signcheck.SignViewEvent;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.BlockPosition;
import com.comphenix.protocol.wrappers.nbt.NbtCompound;
import com.comphenix.protocol.wrappers.nbt.NbtFactory;
import io.github.Jochyoua.MyChristianSwearFilter.MCSF;
import io.github.Jochyoua.MyChristianSwearFilter.shared.Types;
import io.github.Jochyoua.MyChristianSwearFilter.shared.Utils;
import io.github.Jochyoua.MyChristianSwearFilter.signcheck.ProtocolUtils;
import io.github.Jochyoua.MyChristianSwearFilter.signcheck.SignViewEvent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;

public class SignEvents implements Listener {
    MCSF mcsf;
    Utils utils;

    public SignEvents(MCSF plugin, Utils utils) {
        this.mcsf = plugin;
        this.utils = utils;
        if (utils.supported("signcheck")) {
            try {
                ProtocolLibrary.getProtocolManager().addPacketListener((new PacketAdapter(plugin, PacketType.Play.Server.TILE_ENTITY_DATA, PacketType.Play.Server.MAP_CHUNK) {
                    @Override
                    public void onPacketSending(PacketEvent event) {
                        try {
                            if (event.getPacket().getType() == PacketType.Play.Server.TILE_ENTITY_DATA) {
                                onTileEntityData(event);
                            } else {
                                onMapChunk(event);
                            }
                        } catch (Exception e) {
                            FileConfiguration language = mcsf.getLanguage();
                            plugin.getLogger().log(Level.SEVERE, "Failure: {message}"
                                    .replaceAll("(?i)\\{message}|(?i)%message%",
                                            Objects.requireNonNull(language.getString("variables.error.execute_failure"))
                                                    .replaceAll("(?i)\\{feature}", "Sign Filtering")), e);
                            plugin.getLogger().log(Level.INFO, "Failure: {message}".replaceAll("(?i)\\{message}", Objects.requireNonNull(language.getString("variables.error.execute_failure_link"))));
                            ProtocolLibrary.getProtocolManager().removePacketListener(this);
                        }
                    }

                    private void onTileEntityData(PacketEvent event) {
                        PacketContainer packet = event.getPacket();
                        assert ProtocolUtils.Packet.TileEntityData.isTileEntityDataPacket(packet);
                        if (!ProtocolUtils.Packet.TileEntityData.isUpdateSignPacket(packet))
                            return;
                        Player player = event.getPlayer();
                        BlockPosition blockPosition = ProtocolUtils.Packet.TileEntityData.getBlockPosition(packet);
                        Location location = new Location(player.getWorld(), blockPosition.getX(), blockPosition.getY(), blockPosition.getZ());
                        NbtCompound signData = ProtocolUtils.Packet.TileEntityData.getTileEntityData(packet);
                        String[] rawLines = ProtocolUtils.TileEntity.Sign.getText(signData);
                        SignViewEvent signSendEvent = callSignEvent(player, location, rawLines);
                        if (signSendEvent.isCancelled()) {
                            event.setCancelled(true);
                        } else if (signSendEvent.isModified()) {
                            String[] newLines = signSendEvent.getLines();
                            PacketContainer outgoingPacket = packet.shallowClone();
                            NbtCompound outgoingSignData = NbtFactory.ofCompound(signData.getName());
                            for (String key : signData.getKeys())
                                outgoingSignData.put(key, signData.getValue(key));
                            ProtocolUtils.TileEntity.Sign.setText(outgoingSignData, newLines);
                            ProtocolUtils.Packet.TileEntityData.setTileEntityData(outgoingPacket, outgoingSignData);
                            event.setPacket(outgoingPacket);
                        }
                    }

                    private void onMapChunk(PacketEvent event) {
                        PacketContainer packet = event.getPacket();
                        assert ProtocolUtils.Packet.MapChunk.isMapChunkPacket(packet);
                        Player player = event.getPlayer();
                        World world = player.getWorld();
                        PacketContainer outgoingPacket = null;
                        List<Object> outgoingTileEntitiesData = null;
                        boolean removedSignData = false;
                        List<Object> tileEntitiesData = ProtocolUtils.Packet.MapChunk.getTileEntitiesData(packet);
                        for (int index = 0, size = tileEntitiesData.size(); index < size; index++) {
                            Object nmsTileEntityData = tileEntitiesData.get(index);
                            NbtCompound tileEntityData = NbtFactory.fromNMSCompound(nmsTileEntityData);
                            if (ProtocolUtils.TileEntity.Sign.isTileEntitySignData(tileEntityData)) {
                                int x = ProtocolUtils.TileEntity.getX(tileEntityData);
                                int y = ProtocolUtils.TileEntity.getY(tileEntityData);
                                int z = ProtocolUtils.TileEntity.getZ(tileEntityData);
                                Location location = new Location(world, x, y, z);
                                String[] rawLines = ProtocolUtils.TileEntity.Sign.getText(tileEntityData);
                                SignViewEvent signSendEvent = callSignEvent(player, location, rawLines);
                                if (signSendEvent.isCancelled() || signSendEvent.isModified()) {
                                    if (outgoingPacket == null) {
                                        outgoingPacket = packet.shallowClone();
                                        outgoingTileEntitiesData = new ArrayList(tileEntitiesData);
                                        ProtocolUtils.Packet.MapChunk.setTileEntitiesData(outgoingPacket, outgoingTileEntitiesData);
                                    }
                                    if (signSendEvent.isCancelled()) {
                                        outgoingTileEntitiesData.set(index, null);
                                        removedSignData = true;
                                    } else if (signSendEvent.isModified()) {
                                        String[] newLines = signSendEvent.getLines();
                                        if (outgoingPacket == null) {
                                            outgoingPacket = packet.shallowClone();
                                            outgoingTileEntitiesData = new ArrayList(tileEntitiesData);
                                            ProtocolUtils.Packet.MapChunk.setTileEntitiesData(outgoingPacket, outgoingTileEntitiesData);
                                        }
                                        NbtCompound outgoingSignData = NbtFactory.ofCompound(tileEntityData.getName());
                                        for (String key : tileEntityData.getKeys())
                                            outgoingSignData.put(key, tileEntityData.getValue(key));
                                        ProtocolUtils.TileEntity.Sign.setText(outgoingSignData, newLines);
                                        outgoingTileEntitiesData.set(index, outgoingSignData.getHandle());
                                    }
                                }
                            }
                        }
                        if (outgoingPacket != null) {
                            if (removedSignData) {
                                outgoingTileEntitiesData.removeIf(Objects::isNull);
                            }
                            event.setPacket(outgoingPacket);
                        }
                    }

                    private SignViewEvent callSignEvent(Player player, Location location, String[] rawLines) {
                        SignViewEvent signSendEvent = new SignViewEvent(player, location, rawLines);
                        Bukkit.getPluginManager().callEvent(signSendEvent);
                        return signSendEvent;
                    }
                }));
                plugin.getServer().getPluginManager().registerEvents(this, plugin);
                Bukkit.getConsoleSender().sendMessage("Support for SignCheck is just fine!");
            } catch (Exception e) {
                FileConfiguration language = plugin.getLanguage();
                plugin.getLogger().log(Level.SEVERE, "Failure: {message}"
                        .replaceAll("(?i)\\{message}|(?i)%message%",
                                Objects.requireNonNull(language.getString("variables.error.execute_failure"))
                                        .replaceAll("(?i)\\{feature}", "Sign Filtering")), e);
                plugin.getLogger().log(Level.INFO, "Failure: {message}".replaceAll("(?i)\\{message}", Objects.requireNonNull(language.getString("variables.error.execute_failure_link"))));
                HandlerList.unregisterAll(this);
            }
        } else {
            utils.send(Bukkit.getConsoleSender(), plugin.getLanguage().getString("variables.failure").replaceAll("(?i)\\{message}|(?i)%message%", plugin.getLanguage().getString("variables.error.unsupported")).replaceAll("(?i)\\{feature}", "Sign Filtering"));
        }
    }

    // Sign Events
    @EventHandler
    public void viewSign(SignViewEvent event) {
        try {
            if (!(event.getLine(0).equalsIgnoreCase("{\"text\":\"\"}")
                    && event.getLine(1).equalsIgnoreCase("{\"text\":\"\"}")
                    && event.getLine(2).equalsIgnoreCase("{\"text\":\"\"}")
                    && event.getLine(3).equalsIgnoreCase("{\"text\":\"\"}"))) {
                String lines = String.join("_", event.getLines());
                if (utils.status(event.getPlayer().getUniqueId())) {
                    if (!utils.isclean(lines, "both")) {
                        lines = utils.clean(lines, true, false, "both", Types.Filters.SIGNS);
                    }
                }
                event.setLines(lines.split("_"));
            }
        } catch (Exception e) {
            FileConfiguration language = mcsf.getLanguage();
            mcsf.getLogger().log(Level.SEVERE, "Failure: {message}"
                    .replaceAll("(?i)\\{message}|(?i)%message%",
                            Objects.requireNonNull(language.getString("variables.error.execute_failure"))
                                    .replaceAll("(?i)\\{feature}", "Sign Filtering")), e);
            mcsf.getLogger().log(Level.INFO, "Failure: {message}".replaceAll("(?i)\\{message}", Objects.requireNonNull(language.getString("variables.error.execute_failure_link"))));
            HandlerList.unregisterAll(this);
        }
    }
}
