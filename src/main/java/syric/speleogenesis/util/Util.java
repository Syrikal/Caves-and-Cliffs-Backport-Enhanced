package syric.speleogenesis.util;

import com.blackgear.cavesandcliffs.core.other.tags.CCBBlockTags;
import net.minecraft.block.BlockState;
import net.minecraft.block.material.Material;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import syric.speleogenesis.SpeleogenesisBlockTags;

import java.util.ArrayList;
import java.util.Random;

public class Util {


    //Used primarily for spreading.
    public static boolean filter(BlockPos pos, World world) {
        if (world.getBlockState(pos).getBlock().is(SpeleogenesisBlockTags.CAVE_DECORATIONS)) {
            return true;
        }
        if (world.getBlockState(pos).getMaterial().isSolid()) {
            return false;
        }
        if (world.getFluidState(pos).isSource()) {
            return false;
        }
        else {
            return true;
        }
    }


    public static ArrayList<Direction> directionsShuffled() {
        Random random = new Random();
        ArrayList<Direction> output = new ArrayList<>();
        boolean done = false;
        while (!done) {
            Direction newDir = Direction.getRandom(random);
            if (!output.contains(newDir)) {
                output.add(newDir);
                if (output.size() == 6) {
                    done = true;
                }
            }
        }
        return output;
    }

    public static ArrayList<Direction> horizontalDirectionsShuffled() {
        Random random = new Random();
        ArrayList<Direction> output = new ArrayList<>();
        while (true) {
            Direction newDir = Direction.getRandom(random);
            if (!output.contains(newDir) && newDir.getAxis() != Direction.Axis.Y) {
                output.add(newDir);
                if (output.size() == 4) {
                    return output;
                }
            }
        }
    }

    public static Direction randomFacing() {
        Random random = new Random();
        int dir = random.nextInt(4);
        if (dir == 0) {
            return Direction.NORTH;
        } else if (dir == 1) {
            return Direction.EAST;
        } else if (dir == 2) {
            return Direction.SOUTH;
        } else {
            return Direction.WEST;
        }
    }

    public static BlockState placeVine(World world, BlockPos pos) {
        return null;
    }

    public static BlockState placeLichen(World world, BlockPos pos) {
        return null;
    }

    public static double ovoidDistance(BlockPos pos, BlockPos center) {
        double x = Math.abs(pos.getX() - center.getX());
        double z = Math.abs(pos.getZ() - center.getZ());
        double y = Math.abs(pos.getY() - center.getY()) * 0.3;
        return Math.sqrt(x * x + z * z + y * y);
    }

    public static Direction[] horizontalDirections() {
        return new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};
    }

    public static Boolean replaceableOrAir(World world, BlockPos pos) {
        return world.getBlockState(pos).is(CCBBlockTags.LUSH_GROUND_REPLACEABLE) || world.getBlockState(pos).getMaterial() == Material.AIR;
    }


}
