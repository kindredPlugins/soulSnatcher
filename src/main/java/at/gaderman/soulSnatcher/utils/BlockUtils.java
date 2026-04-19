package at.gaderman.soulSnatcher.utils;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class BlockUtils {

    public static boolean isLocationInBox(Location location, Location firstCorner, Location secondCorner) {
        double xMin = Math.min(firstCorner.getX(), secondCorner.getX()) - 1;
        double xMax = Math.max(firstCorner.getX(), secondCorner.getX()) + 1;
        double yMin = Math.min(firstCorner.getY(), secondCorner.getY()) - 1;
        double yMax = Math.max(firstCorner.getY(), secondCorner.getY()) + 1;
        double zMin = Math.min(firstCorner.getZ(), secondCorner.getZ()) - 1;
        double zMax = Math.max(firstCorner.getZ(), secondCorner.getZ()) + 1;

        double x = location.getX();
        double y = location.getY();
        double z = location.getZ();

        return (x >= xMin && x <= xMax) &&
                (y >= yMin && y <= yMax) &&
                (z >= zMin && z <= zMax);
    }

    public static @Nullable Block checkForBlock(Location corner1, Location corner2, Predicate<Block> checkCondition) {
        return checkForBlocks(corner1, corner2, checkCondition, 1).stream().findFirst().orElse(null);
    }

    public static @NonNull List<Block> checkForBlocks(Location corner1, Location corner2, Predicate<Block> checkCondition) {
        return checkForBlocks(corner1, corner2, checkCondition, Integer.MAX_VALUE);
    }

    public static @NonNull List<Block> checkForBlocks(Location corner1, Location corner2, Predicate<Block> checkCondition, int sizeLimit) {
        if (corner1.getWorld() != corner2.getWorld()) {
            throw new IllegalArgumentException("Both corners must be in the same world.");
        }

        List<Block> blocks = new ArrayList<>();
        World world = corner1.getWorld();

        int minX = Math.min(corner1.getBlockX(), corner2.getBlockX());
        int minY = Math.min(corner1.getBlockY(), corner2.getBlockY());
        int minZ = Math.min(corner1.getBlockZ(), corner2.getBlockZ());
        int maxX = Math.max(corner1.getBlockX(), corner2.getBlockX());
        int maxY = Math.max(corner1.getBlockY(), corner2.getBlockY());
        int maxZ = Math.max(corner1.getBlockZ(), corner2.getBlockZ());

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    Block block = world.getBlockAt(x, y, z);
                    if (checkCondition.test(block)) {
                        blocks.add(block);

                        if (blocks.size() >= sizeLimit)
                            return blocks;
                    }
                }
            }
        }

        return blocks;
    }

    public static @Nullable Location findSpreadLocation(Location center, int xz, int y) {
        for (int i = 0; i < 16; i++) {
            int dx = (int) ((0.5 - Math.random()) * (xz * 2));
            int dz = (int) ((0.5 - Math.random()) * (xz * 2));

            Location candidate = center.clone().add(dx, y, dz);
            while (candidate.getY() >= center.getY() - 2 * y && candidate.getY() > center.getWorld().getMinHeight() &&
                    candidate.getBlock().isPassable()) {
                candidate.subtract(0, 1, 0);
            }
            candidate.add(0, 1, 0);

            Block feet = candidate.getBlock();
            Block head = feet.getRelative(BlockFace.UP);
            if (!feet.getRelative(BlockFace.DOWN).isPassable() && feet.isPassable() && head.isPassable()) {
                return candidate;
            }
        }
        return null; // no valid spot found
    }

}
