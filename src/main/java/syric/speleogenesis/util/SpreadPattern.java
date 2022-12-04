package syric.speleogenesis.util;

import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import static syric.speleogenesis.Speleogenesis.chatPrint;
import static syric.speleogenesis.util.Util.directionsShuffled;
import static syric.speleogenesis.util.Util.filter;

public class SpreadPattern {

    private final World world;
    private final int numberBlocks;
    private final BlockPos origin;
    private final int maxRadius;
    public final ConcurrentHashMap<BlockPos, Integer> blockFillingMap = new ConcurrentHashMap<BlockPos, Integer>();
    public final ConcurrentHashMap<BlockPos, Double> blockDistanceMap = new ConcurrentHashMap<BlockPos, Double>();
    public boolean done;

    public SpreadPattern(World world, BlockPos origin, int numberOfBlocks, int maxRadius) {
        this.world = world;
        this.origin = origin;
        this.numberBlocks = numberOfBlocks;
        this.maxRadius = maxRadius;
    }


    public void growBlockMap(int spreads) {
        if (blockFillingMap.isEmpty()) {
            blockFillingMap.put(origin, numberBlocks);
            blockDistanceMap.put(origin, 0.0);
        }
        int maxCyclesCountdown = numberBlocks * 2;
        int counter = spreads;


        //Iterate until done spreading (complete) OR run out of time (maxCyclesCountdown)
        while (maxCyclesCountdown > 0) {
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
                    for (Direction direction : directionsShuffled()) {
                        if (blockFillingMap.get(pos) == 1) {
                            break;
                        }
                        //Get a candidate adjacent block
                        BlockPos candidate = pos.relative(direction);
                        //IDEA: spread more than one point at a time? One-sixth current points, min 1, max whatever will equalize
                        //If it can be spread to, send a point that way. This includes blocks already listed which are less full than itself.
                        if (filter(candidate, world)) {
                            //If it's already in the map, only spread if it's less full than the one doing the spreading!
                            if (blockFillingMap.containsKey(candidate)) {
                                if (blockFillingMap.get(candidate) < blockFillingMap.get(pos)) {
                                    blockFillingMap.put(candidate, blockFillingMap.get(candidate) + 1);
                                    blockFillingMap.put(pos, blockFillingMap.get(pos) - 1);
                                    counter--;
                                }
                                //If it's not in the map yet, spread one point to it.
                            } else {
                                blockFillingMap.put(candidate, 1);
                                blockFillingMap.put(pos, blockFillingMap.get(pos) - 1);
                                blockDistanceMap.putIfAbsent(candidate, blockDistanceMap.get(pos)+1);
                                if (blockDistanceMap.get(pos)+1 > maxRadius) {
//                                    chatPrint("Spread too far, calling off", world);
                                    done = true;
                                    return;
                                }
                                counter--;
                            }
                            if (counter <= 0) {
//                                chatPrint("Spread pattern. Pattern now has " + blockFillingMap.entrySet().size() + " blocks in it.", world);
                                return;
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
            if (tentativeSuccess) {
                done = true;
                return;
            }

            //Reduce countdown
            maxCyclesCountdown --;
        }

        //THE LOOP IS OVER! The map should now be full of block positions with 1 block of output in each.
    }

    public ConcurrentHashMap<BlockPos, Double> returnMap() {
        for (BlockPos pos : blockFillingMap.keySet()) {
            if (blockFillingMap.get(pos) > 1) {
//                chatPrint("Returning a spread block with " + blockFillingMap.get(pos) + " blocks inside it!", world);
            }
            //Set every block's distance to the minimum of its actual distance and its recorded distance
            blockDistanceMap.put(pos, Math.min(distance(pos), blockDistanceMap.get(pos)));
        }
        return blockDistanceMap;
    }

    public ConcurrentHashMap<BlockPos, Double> returnMapSpreadDist() {
        return blockDistanceMap;
    }

    private float distance(BlockPos pos) {
        double xdist = Math.pow(origin.getX() - pos.getX(), 2);
        double zdist = Math.pow(origin.getZ() - pos.getZ(), 2);
        double ydist = Math.pow(origin.getY() - pos.getY(), 2);
        return (float) Math.sqrt(xdist + zdist + ydist);
    }

    public static ArrayList<BlockPos> getContiguous(BlockPos pos, List<BlockPos> list) {
        ArrayList<BlockPos> adjacent = new ArrayList<>();
//        ArrayList<BlockPos> notChecked = new ArrayList<>();
        adjacent.add(pos);
        boolean done = false;
        int countdown = list.size();
        while (!done && countdown > 0) {
            ArrayList<BlockPos> toAdd = new ArrayList<>();
            countdown --;
            boolean tentativeDone = true;
            for (BlockPos newpos : adjacent) {
                for (Direction direction: Direction.values()) {
                    BlockPos candidate = newpos.relative(direction);
                    if (list.contains(candidate) && !adjacent.contains(candidate)) {
                        toAdd.add(candidate);
                        tentativeDone = false;
                    }
                }
//                adjacent.add(newpos);
//                notChecked.remove(newpos);
            }
            adjacent.addAll(toAdd);
            done = tentativeDone;
        }
        return adjacent;
    }

    public static ArrayList<BlockPos> getContiguousLimited(BlockPos pos, List<BlockPos> list, int maxBlocks) {
        ArrayList<BlockPos> adjacent = new ArrayList<>();
//        ArrayList<BlockPos> notChecked = new ArrayList<>();
        adjacent.add(pos);
        boolean done = false;
        int countdown = list.size();
        int added = 0;
        while (!done && countdown > 0 && added < maxBlocks) {
            ArrayList<BlockPos> toAdd = new ArrayList<>();
            countdown --;
            boolean tentativeDone = true;
            for (BlockPos newpos : adjacent) {
                for (Direction direction: Direction.values()) {
                    BlockPos candidate = newpos.relative(direction);
                    if (list.contains(candidate) && !adjacent.contains(candidate)) {
                        toAdd.add(candidate);
                        tentativeDone = false;
                        added++;
                    }
                }
//                adjacent.add(newpos);
//                notChecked.remove(newpos);
            }
            adjacent.addAll(toAdd);
            done = tentativeDone;
        }
        return adjacent;
    }

    public void wormMap() {
        int maxCyclesCountdown = numberBlocks * 2;
        BlockPos focusPos = origin;
        blockFillingMap.put(focusPos, numberBlocks);


        //Iterate until done spreading (complete) OR run out of time (maxCyclesCountdown)
        while (maxCyclesCountdown > 0) {

            if (blockFillingMap.get(focusPos) == 0) {
                done = true;
                break;
            } else {
                Direction dir = Direction.getRandom(new Random());
                if (dir.getAxis() == Direction.Axis.Y) {
                    continue;
                }
                BlockPos candidate = focusPos.relative(dir);
                if (blockFillingMap.containsKey(candidate)) {
                    blockFillingMap.put(candidate, blockFillingMap.get(focusPos));
                    blockFillingMap.put(focusPos, 1);
                    focusPos = candidate;
                } else {
                    blockFillingMap.put(candidate, blockFillingMap.get(focusPos) - 1);
                    blockFillingMap.put(focusPos, 1);
                    focusPos = candidate;
                }
            }
            maxCyclesCountdown--;
        }
        for (BlockPos pos : blockFillingMap.keySet()) {
            blockDistanceMap.put(pos, (double) distance(pos));
        }
    }

    public void flatMap() {
        if (blockFillingMap.isEmpty()) {
            blockFillingMap.put(origin, numberBlocks);
            blockDistanceMap.put(origin, 0.0);
        }
        int maxCyclesCountdown = numberBlocks * 2;

        //Iterate until done spreading (complete) OR run out of time (maxCyclesCountdown)
        while (maxCyclesCountdown > 0) {
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
                    for (Direction direction : directionsShuffled()) {
                        //Not up or down
                        if (direction == Direction.DOWN || direction == Direction.UP) {
                            continue;
                        }

                        //Get a candidate adjacent block
                        BlockPos candidate = pos.relative(direction);
                        //IDEA: spread more than one point at a time? One-sixth current points, min 1, max whatever will equalize
                        //If it can be spread to, send a point that way. This includes blocks already listed which are less full than itself.
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
                            if (blockDistanceMap.get(pos)+1 > maxRadius) {
//                                    chatPrint("Spread too far, calling off", world);
                                done = true;
                                return;
                            }
                        }
                    }
                    //Once you're done iterating over directions from this position, move on to the next block in the map
                }
                //If there's nothing to spread from this location, don't bother and move on to the next block in the map
            }

            //Once finished iterating over current blocks:
            //Check to see if we're done, i.e. there was no spreading to do
            if (tentativeSuccess) {
                done = true;
                return;
            }

            //Reduce countdown
            maxCyclesCountdown --;
        }
    }

}
