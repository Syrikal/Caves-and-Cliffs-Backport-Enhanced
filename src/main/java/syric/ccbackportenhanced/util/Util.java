package syric.ccbackportenhanced.util;

import com.blackgear.cavesandcliffs.common.blocks.ICaveVines;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Random;

public class Util {


    public static boolean filter(BlockPos pos, World world) {
        if (world.getBlockState(pos).getBlock() instanceof ICaveVines) {
            return true;
        }
        return (!world.getBlockState(pos).getMaterial().isSolid()) && !world.getFluidState(pos).isSource();
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



}
