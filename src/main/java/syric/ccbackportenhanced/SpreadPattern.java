package syric.ccbackportenhanced;

import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

public class SpreadPattern {

    private final World world;
    private final int numberBlocks;
    private final BlockPos origin;

    public SpreadPattern(World world, BlockPos origin, int numberOfBlocks) {
        this.world = world;
        this.origin = origin;
        this.numberBlocks = numberOfBlocks;
    }

    public boolean filter(BlockPos pos) {
        return !world.getBlockState(pos).isSolidRender(world, pos) && !world.getFluidState(pos).isSource();
    }

    private ArrayList<Direction> allShuffled() {
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


    public Map<BlockPos, Double> blockMap() {
        ConcurrentHashMap<BlockPos, Integer> blockFillingMap = new ConcurrentHashMap<BlockPos, Integer>();
        ConcurrentHashMap<BlockPos, Double> blockDistanceMap = new ConcurrentHashMap<BlockPos, Double>();
        blockFillingMap.put(origin, numberBlocks);
        blockDistanceMap.put(origin, 0.0);
        int maxCyclesCountdown = numberBlocks * 2;
        boolean success = false;

        //Iterate until done spreading (success) OR run out of time (maxCyclesCountdown)
        while (!success && maxCyclesCountdown > 0) {
            //Each iteration, assume you're done until you find somewhere you aren't
            boolean tentativeSuccess = true;
            //For every block currently in our list:
            for (Map.Entry<BlockPos, Integer> entry : blockFillingMap.entrySet()) {
                //The block itself
                BlockPos pos = entry.getKey();
                //If it has stuff inside to spread:
                if (entry.getValue() > 1) {
                    //We're not done
                    tentativeSuccess = false;
                    //For each direction, randomly:
                    for (Direction direction : allShuffled()) {
                        //Get a candidate adjacent block
                        BlockPos candidate = pos.relative(direction);
                        //If it can be spread to, send a point that way. This includes blocks already listed which are less full than itself.
                        if (filter(candidate)) {
                            //If it's already in the map, only spread if it's less full than the one doing the spreading!
                            if (blockFillingMap.containsKey(candidate)) {
                                if (blockFillingMap.get(candidate) < blockFillingMap.get(pos)) {
                                    blockFillingMap.put(candidate, blockFillingMap.get(candidate) + 1);
                                    blockFillingMap.put(pos, blockFillingMap.get(pos) - 1);
                                }
                                //If it's not in the map yet, spread one point to it.
                            } else {
                                blockFillingMap.put(candidate, 1);
                                blockFillingMap.put(pos, blockFillingMap.get(pos) - 1);
                                blockDistanceMap.putIfAbsent(candidate, blockDistanceMap.get(pos)+1);
                            }
                        }
                        //If it's not replaceable, don't try to spread there
                    }
                    //Once you're done iterating over directions from this position, move on to the next block in the map
                }
                //If there's nothing to spread from this location, don't bother and move on to the next block in the map
            }

            //Once finished iterating over current blocks:
            //Check to see if we're done, i.e. there was no spreading to do
            success = tentativeSuccess;

            //Reduce countdown
            maxCyclesCountdown --;
        }

        //THE LOOP IS OVER! The map should now be full of block positions with 1 block of output in each.

        //Check if any blocks failed to finish spreading. Also reduce block distance if necessary.
        for (BlockPos pos : blockFillingMap.keySet()) {
            if (blockFillingMap.get(pos) > 1) {
//                chatPrint("Returning a spread block with " + blockFillingMap.get(pos) + " blocks inside it!", world);
            }
            //Set every block's distance to the minimum of its actual distance and its recorded distance
            blockDistanceMap.put(pos, Math.min(distance(pos), blockDistanceMap.get(pos)));
        }
        return blockDistanceMap;
    }

    private float distance(BlockPos pos) {
        double xdist = Math.pow(origin.getX() - pos.getX(), 2);
        double zdist = Math.pow(origin.getZ() - pos.getZ(), 2);
        double ydist = Math.pow(origin.getY() - pos.getY(), 2);
        return (float) Math.sqrt(xdist + zdist + ydist);
    }
}
