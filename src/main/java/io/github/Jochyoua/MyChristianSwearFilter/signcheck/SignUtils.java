package io.github.Jochyoua.MyChristianSwearFilter.signcheck;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.BlockState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SignUtils {

    public static <T extends BlockState> List<T> getNearbyTileEntities(Location location, int chunkRadius, Class<T> type) {
        if (location == null || location.getWorld() == null || chunkRadius <= 0 || type == null)
            return Collections.emptyList();
        List<T> tileEntities = new ArrayList<>();
        World world = location.getWorld();
        Chunk center = location.getChunk();
        int startX = center.getX() - chunkRadius;
        int endX = center.getX() + chunkRadius;
        int startZ = center.getZ() - chunkRadius;
        int endZ = center.getZ() + chunkRadius;
        for (int chunkX = startX; chunkX <= endX; chunkX++) {
            for (int chunkZ = startZ; chunkZ <= endZ; chunkZ++) {
                if (world.isChunkLoaded(chunkX, chunkZ)) {
                    Chunk chunk = world.getChunkAt(chunkX, chunkZ);
                    for (BlockState tileEntity : chunk.getTileEntities()) {
                        if (type.isInstance(tileEntity))
                            tileEntities.add(type.cast(tileEntity));
                    }
                }
            }
        }
        return tileEntities;
    }
}
