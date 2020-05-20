package com.github.Jochyoua.MCSF.signcheck;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.BlockPosition;
import com.comphenix.protocol.wrappers.nbt.NbtCompound;
import com.comphenix.protocol.wrappers.nbt.NbtFactory;
import com.comphenix.protocol.wrappers.nbt.NbtWrapper;
import com.github.Jochyoua.MCSF.Main;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class SignPacketListener extends PacketAdapter {

    public SignPacketListener() {
        super(PacketAdapter.params()
                .plugin(Main.getInstance())
                .serverSide()
                .listenerPriority(ListenerPriority.NORMAL)
                .types(PacketType.Play.Server.TILE_ENTITY_DATA, PacketType.Play.Server.MAP_CHUNK));
    }

    public void onPacketSending(PacketEvent event) {
        if (event.getPacket().getType() == PacketType.Play.Server.TILE_ENTITY_DATA) {
            onTileEntityData(event);
        } else {
            onMapChunk(event);
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
                        outgoingTileEntitiesData.set(index, ((NbtWrapper)outgoingSignData).getHandle());
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
        Bukkit.getPluginManager().callEvent((Event)signSendEvent);
        return signSendEvent;
    }
}
