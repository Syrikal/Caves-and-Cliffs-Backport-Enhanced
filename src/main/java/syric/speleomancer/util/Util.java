package syric.speleomancer.util;

import net.minecraft.block.BlockState;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import syric.speleomancer.SpeleomancerBlockTags;

import java.util.ArrayList;
import java.util.Random;

public class Util {


    //Used primarily for spreading.
    public static boolean filter(BlockPos pos, World world) {
        if (world.getBlockState(pos).getBlock().is(SpeleomancerBlockTags.CAVE_DECORATIONS)) {
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


}
