package syric.speleogenesis.lush;

import com.blackgear.cavesandcliffs.common.blocks.*;
import com.blackgear.cavesandcliffs.core.other.tags.CCBBlockTags;
import com.blackgear.cavesandcliffs.core.registries.CCBBlocks;
import com.google.common.collect.ArrayListMultimap;
import net.minecraft.block.*;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.IPacket;
import net.minecraft.network.play.server.SSpawnObjectPacket;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.state.properties.DoubleBlockHalf;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.vector.Vector3i;
import net.minecraft.world.World;
import org.lwjgl.system.CallbackI;
import syric.speleogenesis.SpeleogenesisBlockTags;
import syric.speleogenesis.util.RandomGenerators;
import syric.speleogenesis.util.SpreadPattern;
import syric.speleogenesis.util.Util;
import net.minecraft.block.DoublePlantBlock;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static syric.speleogenesis.Speleogenesis.chatPrint;
import static syric.speleogenesis.util.Util.filter;
import static syric.speleogenesis.util.Util.randomFacing;

public class LushGeneratorEntity extends Entity {

    private final World world;

    private RayTraceResult traceResult;

    private BlockPos origin;
    private int spreadAttempts;
    private SpreadPattern pattern;
    //An enum for size/type of conversion? A tall cylinder? a beeg boi?


    //These don't really need to be maps, do they?
    private ConcurrentHashMap<BlockPos, Double> spreadMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<BlockPos, BlockState> finalPlacementMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<BlockPos, Double> distanceMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<BlockPos, Double> replaceCandidateMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<BlockPos, Double> ceilingMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<BlockPos, Double> floorMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<BlockPos, Double> floorCornerMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<BlockPos, Double> wallMap = new ConcurrentHashMap<>();
    //The keys are air blocks. The values are all adjacent wall blocks.
    private final ArrayListMultimap<BlockPos, BlockPos> wallAdjacentMap = ArrayListMultimap.create();

    //A way to designate part of a larger area as pond? Perhaps pondFloor takes in a subset of horizontal area in which it's allowed to work.

    private final ArrayList<BlockPos> waterBlocks = new ArrayList<>();
    //Governs clay blocks that can have decorations added.
    private final ArrayList<BlockPos> exposedClayBlocks = new ArrayList<>();
    //Governs floor moss that can have decorations added.
    private final ArrayList<BlockPos> floorMossBlocks = new ArrayList<>();
    //Governs ceiling moss that can have decorations added.
    private final ArrayList<BlockPos> ceilingMossBlocks = new ArrayList<>();
    //Govern nonexposed moss and clay blocks
    private final ArrayList<BlockPos> nonexposedMossBlocks = new ArrayList<>();
    private final ArrayList<BlockPos> nonexposedClayBlocks = new ArrayList<>();
    private final ArrayList<BlockPos> bareCeiling = new ArrayList<>();



    private boolean isOutdoors = false;
    private boolean plantedTree = false;

    private boolean foundOrigin = false;
    private boolean completedSpread = false;
    private boolean processedMap = false;
    private boolean generateFloor = false;
    private boolean generateCeiling = false;
    private boolean generateDecorations =false;
    private boolean placed = false;
    private boolean setBiome = false;

    private double rippleDist = 0.0;

    private int ticks = 0;


    public LushGeneratorEntity(EntityType<?> type, World world) {
        super(type, world);
        this.world = world;
    }

    public void setTraceResult(RayTraceResult result) {
        traceResult = result;
//        chatPrint("Lush Generator actually exists", world);
    }

    public void tick() {
        super.tick();

        if (world.isClientSide) {
            return;
        }

        ticks++;

        if (generateDecorations && !placed) {
            rippleDist += 0.2;
            spawnRipple(rippleDist);
            if (rippleDist >= 30) {
                placed = true;
                chatPrint("Finished placing blocks", world);
            }
        }


        if (ticks >= 5) {
            ticks = 0;
//            chatPrint("Lush generator is ticking! This should happen every 2 seconds.", world);

            //Decide what step to do next
            //Find the origin
            if (!foundOrigin) {
                findOrigin();
                chatPrint("Found origin", world);
            }

            //Plant azalea if outdoors
            else if (isOutdoors) {
                //Plant an azalea tree
                if (!plantedTree) {
                    plantAzaleaTree(origin);
                    plantedTree = true;
                }
                else {
                    chatPrint("All done, deleting entity", world);
                    this.kill();
                }
            }

            //Spread into an area
            else if (!completedSpread) {
                if (pattern == null) {
                    pattern = new SpreadPattern(world, origin, 10000, 30);
                }
                //Modify this. Too laggy = smaller, too long = larger.
                int currentSize = pattern.blockFillingMap.size();
                pattern.growBlockMap(50000);
                if (pattern.blockFillingMap.size() == currentSize) {
                    spreadAttempts++;
                } else {
                    spreadAttempts = 0;
                }
                if (spreadAttempts > 3 || pattern.done) {
//                    chatPrint("Can't spread any further", world);
                    completedSpread = true;
                    this.spreadMap = pattern.returnMapSpreadDist();
                }
            }

            //Remove all blocks that are too far away
            //Get all replaceable blocks adjacent to blocks in the map
            //Split adjacent blocks by position
            //
            else if (!processedMap) {
                cullMap();
                createCandidateMap();
                divideCandidateMap();
                processedMap = true;
                chatPrint("Processed map", world);
            }

            //Place floor: moss, clay, water
            //FIGURE OUT WHAT REPLACES WHAT AND WHAT DOESN'T
            else if (!generateFloor) {
                if (!floorMap.isEmpty() || !floorCornerMap.isEmpty()) {
                    if (random.nextDouble() < 0.3) {
                        pondFloor();
                    } else {
                        mossFloor();
                    }
                }
                generateFloor = true;
                chatPrint("Generated floor", world);
            }

            //Place ceiling moss blocks
            else if (!generateCeiling) {
                if (!ceilingMap.isEmpty()) {
                    mossCeiling();
                }
                generateCeiling = true;
                chatPrint("Generated ceiling", world);
            }

            //Place decorations: vines, dripleaf, grass, etc.
            else if (!generateDecorations) {
                wallDecorations();
                mossFloorDecorations();
                pondFloorDecorations();
                ceilingDecorations();
                generateDecorations = true;
                chatPrint("Generated decorations", world);
            }

//            //Remove any dropped items
//            else if (!removedItems) {
////                removeItems();
//                removedItems = true;
//                chatPrint("Removed items", world);
//            }


            else if (placed) {
                chatPrint("All done, deleting entity", world);
                this.kill();
            }


        }
    }


    private void findOrigin() {
        if (traceResult == null) {
//            chatPrint("Unfinished lush generator found, committing seppuku", world);
            this.kill();
            return;
        }

        Vector3i posvector = new Vector3i(traceResult.getLocation().x, traceResult.getLocation().y, traceResult.getLocation().z);
        BlockPos hitPos = new BlockPos(posvector);
        World world = this.level;
        int balance = 0; //Each solid block above adds 1, each below adds 2.
        for (int i = -3; i <3; i++) {
            if (i >= 0 && world.getBlockState(hitPos.above(i)).isSolidRender(world, hitPos.above(i))) {
                balance ++;
//                world.setBlock(hitPos.above(i), Blocks.LAPIS_BLOCK.defaultBlockState(), 3);
            } else if (i < 0 && world.getBlockState(hitPos.above(i)).isSolidRender(world, hitPos.above(i))) {
                balance --;
//                world.setBlock(hitPos.above(i), Blocks.REDSTONE_BLOCK.defaultBlockState(), 3);
            }
        }
        boolean hitCeiling = balance > 0;
//        chatPrint("HitPos: " + hitPos.getX() + ", " + hitPos.getY() + ", " + hitPos.getZ(), world);
//        chatPrint("Hit ceiling?: " + hitCeiling, world);
        //Find the height of the ceiling, or if it's outdoors.

        int ceilingHeight = 0;
        boolean outdoors = false;
        if (!hitCeiling) {
            boolean stop = false;
            int distance = 0;
            while (!stop) {
                if (world.getBlockState(hitPos.above(distance)).isSolidRender(world, hitPos.above(distance))) {
                    stop = true;
                    ceilingHeight = distance;
                } else if (distance < 100) {
                    distance++;
                } else {
                    stop = true;
                    outdoors = true;
                }
            }
        } else {
            boolean stop = false;
            int distance = 1;
            while (!stop) {
                if (world.getBlockState(hitPos.below(distance)).isSolidRender(world, hitPos.below(distance))) {
                    stop = true;
                    ceilingHeight = distance-1;
                } else if (distance < 100) {
                    distance++;
                } else {
                    stop = true;
                    outdoors = true;
                }
            }
        }
//        chatPrint("Ceiling Height: " + ceilingHeight, world);

        if (outdoors) {
            //Plant an azalea tree
            foundOrigin = true;
            outdoors = true;
            origin = hitPos;
        }


        //Choose a block four blocks vertically from the surface, or halfway between floor and ceiling.

        int originY = 0;
        if (ceilingHeight > 8) {
            originY = hitCeiling ? hitPos.getY() - 4 : hitPos.getY() + 4;
        } else {
            originY = hitCeiling ? hitPos.getY() - ceilingHeight/2: hitPos.getY() + ceilingHeight/2;
        }

        //MAKE SURE ORIGINPOS IS IN AIR/NONSOLID!
        BlockPos originPos = new BlockPos(hitPos.getX(), originY, hitPos.getZ());
//        chatPrint("Origin pos : " + hitPos.getX() + ", " + originY + ", " + hitPos.getZ(), world);
        this.level.addParticle(ParticleTypes.HAPPY_VILLAGER, hitPos.getX(), originY, hitPos.getZ(), 0.0D, 0.0D, 0.0D);

        foundOrigin = true;
        origin = originPos;
    }

    private void cullMap() {
        for (Map.Entry<BlockPos, Double> entry : spreadMap.entrySet()) {
            //First, cull by spread distance
            if (entry.getValue() > 30) {
                spreadMap.remove(entry.getKey());
            }
            //Then, cull by absolute distance. Use the ovoid version.
            else if (Util.ovoidDistance(entry.getKey(), origin) > 20) {
                spreadMap.remove(entry.getKey());
            }
        }
    }

    private void createCandidateMap() {
        for (Map.Entry<BlockPos, Double> entry : spreadMap.entrySet()) {
            for (Direction direction : Direction.values()) {
                BlockPos candidatePos = entry.getKey().relative(direction);
                boolean replaceable = world.getBlockState(candidatePos).getBlock().is(CCBBlockTags.LUSH_GROUND_REPLACEABLE) || world.getBlockState(candidatePos).is(Blocks.GRAVEL);
                boolean notAVine = !(world.getBlockState(candidatePos).getBlock() instanceof ICaveVines);


                if (replaceable && notAVine) {
                    replaceCandidateMap.putIfAbsent(candidatePos, candidatePos.distSqr(origin));
                }
            }
        }
    }

    private void divideCandidateMap() {
        for (Map.Entry<BlockPos, Double> entry : replaceCandidateMap.entrySet()) {
            boolean isCeiling = spreadMap.containsKey(entry.getKey().below());
            boolean isFloor = spreadMap.containsKey(entry.getKey().above());

            //All ceiling is ceiling
            if (isCeiling) {
                ceilingMap.putIfAbsent(entry.getKey(), entry.getValue());
            }
            //All floor is floor
            else if (isFloor) {
                boolean flushFloor = true;
                for (Direction direction : Direction.values()) {
                    if (direction != Direction.UP && direction != Direction.DOWN) {
                        //I think this was causing the overhang error.
                        if (filter(entry.getKey().relative(direction), world)) {
                            flushFloor = false;
                            break;
                        }
                    }
                }
                //If not next to any spreadable blocks, it's flush in the floor. Put it in floor map.
                if (flushFloor) {
                    floorMap.putIfAbsent(entry.getKey(), entry.getValue());
                }
                //Otherwise it's a floor corner.
                else {
                    floorCornerMap.putIfAbsent(entry.getKey(), entry.getValue());
                }
            }
            //It's a wall.
            else {
                wallMap.putIfAbsent(entry.getKey(), entry.getValue());
                //Put adjacent air blocks into the multimap.
                for (Direction direction : Direction.values()) {
                    if (direction != Direction.UP && direction != Direction.DOWN) {
                        if (spreadMap.containsKey(entry.getKey().relative(direction))) {
                            wallAdjacentMap.put(entry.getKey().relative(direction), entry.getKey());
                        }
                    }
                }
            }
        }
    }

    private void mossFloor() {
        //Elsewhere, replace floor and floorcorner with patches of moss.
        //Basically everything should be moss.

        //Add everything in floorMap and floorCornerMap except water, clay, and moss.
        for (BlockPos pos : floorMap.keySet()) {
            if (!world.getBlockState(pos).is(Blocks.WATER) && !world.getBlockState(pos).is(Blocks.CLAY) && !world.getBlockState(pos).is(CCBBlocks.MOSS_BLOCK.get())) {
                floorMossBlocks.add(pos);
            }
        }
        for (BlockPos pos : floorCornerMap.keySet()) {
            if (!world.getBlockState(pos).is(Blocks.WATER) && !world.getBlockState(pos).is(Blocks.CLAY) && !world.getBlockState(pos).is(CCBBlocks.MOSS_BLOCK.get())) {
                floorMossBlocks.add(pos);
            }
        }

        //Add extra moss
        ArrayList<BlockPos> convertedMossBlocks = new ArrayList<>();
        for (BlockPos pos : floorMossBlocks) {
            //Replace walls adjacent to air above moss with moss.
            for (int i = 1; i <= RandomGenerators.MossFloorUpAdj(); i++) {
                if (spreadMap.containsKey(pos.above(i))) {
                    nonexposedMossBlocks.addAll(wallAdjacentMap.get(pos.above(i)));
                }
            }

            //Replace walls below moss with moss.
            if (wallMap.containsKey(pos.below())) {
                for (int i = 1; i <= RandomGenerators.MossDown(); i++) {
                    if (wallMap.containsKey(pos.below(i))) {
                        nonexposedMossBlocks.add(pos.below(i));
                    }
                }
            }
            //Replace adjacent non-underwater clay with moss
            for (Direction direction : Direction.values()) {
                if (world.getBlockState(pos.relative(direction)).is(Blocks.CLAY) || exposedClayBlocks.contains(pos.relative(direction)) || nonexposedClayBlocks.contains(pos.relative(direction))) {
                    if (!world.getBlockState(pos.relative(direction).above()).getMaterial().isLiquid() && !waterBlocks.contains(pos.relative(direction).above())) {
                        convertedMossBlocks.add(pos.relative(direction));
                    }
                }
            }


            placementMaps(pos, CCBBlocks.MOSS_BLOCK.get().defaultBlockState(), defaultDistance(pos));
        }
        for (BlockPos pos : convertedMossBlocks) {
            placementMaps(pos, CCBBlocks.MOSS_BLOCK.get().defaultBlockState(), defaultDistance(pos));
            floorMossBlocks.add(pos);
            if (world.getBlockState(pos.above()).is(SpeleogenesisBlockTags.DRIPLEAF)) {
                removeClayDecorations(pos);
            }
        }

        //Don't add nonexposed moss to the final placement map yet; it'll be added in the ceiling phase
    }

    private void pondFloor() {

        //Replace floor with water
        for (Map.Entry<BlockPos, Double> entry : floorMap.entrySet()) {
            //Don't replace moss or clay.
            boolean notMoss = !world.getBlockState(entry.getKey()).is(CCBBlocks.MOSS_BLOCK.get());

            boolean notClay = !world.getBlockState(entry.getKey()).is(Blocks.CLAY);

            //if it is clay, override and allow placement if the clay is adjacent to water. This allows it to merge with adjacent pools.
            //Also, check it for decorations.
            if (!notClay) {
                for (Direction direction : Direction.values()) {
                    if (world.getBlockState(entry.getKey().relative(direction)).is(Blocks.WATER)) {
                        //Handle dripleaf
                        if (world.getBlockState(entry.getKey().above()).is(CCBBlocks.SMALL_DRIPLEAF.get())) {
                            placementMaps(entry.getKey().above(), Blocks.AIR.defaultBlockState(), defaultDistance(entry.getKey())+1);
                            placementMaps(entry.getKey().above(2), Blocks.AIR.defaultBlockState(), defaultDistance(entry.getKey())+1);
                        } else if (world.getBlockState(entry.getKey().above()).is(CCBBlocks.BIG_DRIPLEAF_STEM.get())) {
                            Direction facing = world.getBlockState(entry.getKey().above()).getValue(BigDripleafStemBlock.FACING);
                            placementMaps(entry.getKey().above(), CCBBlocks.BIG_DRIPLEAF_STEM.get().defaultBlockState().setValue(BigDripleafStemBlock.FACING, facing).setValue(BlockStateProperties.WATERLOGGED, true), defaultDistance(entry.getKey())+1);
                        }
                        notClay = true;
                        break;
                    }
                }
            }

            if (notMoss && notClay) {
                waterBlocks.add(entry.getKey());
            }
        }

        //Add clay below and around water
        ArrayList<BlockPos> tempClayList = new ArrayList<>();
        ArrayList<BlockPos> additionalClayList = new ArrayList<>();
        for (BlockPos pos : waterBlocks) {
            for (Direction direction : Direction.values()) {
                if (direction != Direction.UP && !waterBlocks.contains(pos.relative(direction))) {
                    //Try to put clay there.
                    //only 50% chance to replace moss, unless it's underwater.
                    if (world.getBlockState(pos.relative(direction)).is(CCBBlocks.MOSS_BLOCK.get()) && direction != Direction.DOWN) {
                        if (random.nextBoolean()) {
                            continue;
                        } else {
                            //Remove any decorations and add it to the tempClayList
                            //Temporarily removed ability to replace moss!
//                            removeMossDecorations(pos.relative(direction));
//                            tempClayList.add(pos.relative(direction));
                        }
                    }
                    else {
                        //Make the block clay
                        tempClayList.add(pos.relative(direction));
                    }
                }
            }
        }

        //Add all floor corners except moss (0% chance)
        for (BlockPos pos : floorCornerMap.keySet()) {
            if (!world.getBlockState(pos).is(CCBBlocks.MOSS_BLOCK.get())) {
                tempClayList.add(pos);
            } else if (random.nextBoolean()) {
                //It's moss. Remove it and any decorations on it, then add to the exposedClayBlocks map.
                //Temporarily removed ability to replace moss!
//                removeMossDecorations(pos);
//                tempClayList.add(pos);
            }
        }

        //Edge water blocks have a chance of being replaced with clay. This runs twice.
        for (int i = 0; i < 2; i++) {
            for (Iterator<BlockPos> it = waterBlocks.iterator(); it.hasNext(); ) {
                BlockPos pos = it.next();
                int nonWaterBlocks = 0;
                for (Direction direction : Util.horizontalDirections()) {
                    if (!waterBlocks.contains(pos.relative(direction))) {
                        nonWaterBlocks++;
                    }
                }
                if (RandomGenerators.replaceWaterBoolean(nonWaterBlocks)) {
                    it.remove();
                    tempClayList.add(pos);
//                    additionalClayList.add(pos);
                }
            }
        }


        //Two 15% chances to remove a random whole pool.
        for (int i = 0; i < 2; i++) {
            if (random.nextDouble() < .15) {
                Object[] posArray = waterBlocks.toArray();
                if (posArray.length == 0) {
                    break;
                }
                BlockPos randomPos = (BlockPos) posArray[random.nextInt(posArray.length)];
                ArrayList<BlockPos> toRemove = SpreadPattern.getContiguous(randomPos, waterBlocks);

                //If it's more than 30 blocks, 70% chance of just removing 20-60%.
                if (toRemove.size() > 30 && random.nextDouble() < 0.7) {
                    BlockPos furthestPos = randomPos;

                    //Find the block in the pool that's the furthest away from the one selected, to ensure we start at the edge.
                    double dist = 0;
                    for (BlockPos pos : toRemove) {
                        if (pos.distSqr(furthestPos) > dist) {
                            dist = pos.distSqr(furthestPos);
                            furthestPos = pos;
                        }
                    }
                    double numberToRemove = (random.nextInt(5) + 2) * (double) toRemove.size() / 10.0;
                    toRemove = SpreadPattern.getContiguousLimited(furthestPos, waterBlocks, (int) numberToRemove);
                }
                for (BlockPos removePos : toRemove) {
                    waterBlocks.remove(removePos);
                    tempClayList.add(removePos);
//                    additionalClayList.add(removePos);
                }
            }
        }

        //There should only be one clay map until here. Part of the 'add additional clay' cycle through all clay should be adding the block to the
        //exposedClayMap if it's exposed. None of the additional clay adds it.

        //Generate thickening pads
        //0-1 patches of radius 2-5
        ArrayList<BlockPos> thickClayLayer = new ArrayList<>();
        if (random.nextBoolean()) {
            Object[] posArray = floorMap.keySet().toArray();
            if (posArray.length != 0) {
                //Pick a spot
                BlockPos randomPos = (BlockPos) posArray[random.nextInt(posArray.length)];
                int size = random.nextInt(40)+5;
                //Make a new SpreadPattern flat pattern
                SpreadPattern pattern = new SpreadPattern(world, randomPos, size, size);
                pattern.flatMap();
                //Add mapped blocks to the thick spot
                for (BlockPos pos : pattern.returnMap().keySet()) {
                    for (int j = -3; j <= 3; j++) {
                        thickClayLayer.add(pos.above(j));
                    }
                }            }
        }

        //ADDING ADDITIONAL CLAY
        for (BlockPos pos : tempClayList) {

            //Replace walls above clay with clay.
            if (wallMap.containsKey(pos.above())) {
                for (int i = 1; i <= RandomGenerators.ClayUp(); i++) {
                    if (wallMap.containsKey(pos.above(i))) {
                        additionalClayList.add(pos.above(i));
                    }
                }
            }

            //Replace walls adjacent to air above clay with clay.
            for (int i = 1; i <= RandomGenerators.ClayUpAdj(); i++) {
                if (spreadMap.containsKey(pos.above(i))) {
                    additionalClayList.addAll(wallAdjacentMap.get(pos.above(i)));
                }
            }

            //Replace walls below clay with clay.
            if (wallMap.containsKey(pos.below())) {
                for (int i = 1; i <= RandomGenerators.ClayDown(); i++) {
                    if (wallMap.containsKey(pos.below(i))) {
                        additionalClayList.add(pos.below(i));
                    }
                }
            }

            //In patches, thicken clay to two blocks.
            if (thickClayLayer.contains(pos) && world.getBlockState(pos.below()).getBlock().is(CCBBlockTags.LUSH_GROUND_REPLACEABLE) && !tempClayList.contains(pos.below())) {
                additionalClayList.add(pos.below());
            }

            //If the clay is exposed, add it to the exposedClayMap for later decoration.
            //"Exposed": block above is air or water. 50% chance of requiring spreadMap.
            //Small dripleaf should require the block above that to be air!
            boolean exposed = false;
            Material above = world.getBlockState(pos.above()).getMaterial();
            if (above == Material.AIR || above == Material.WATER || waterBlocks.contains(pos.above())) {
                if (spreadMap.containsKey(pos.above()) || waterBlocks.contains(pos.above()) || random.nextBoolean()) {
                    exposed = true;
                }
            }
            if (exposed) {
                exposedClayBlocks.add(pos);
            } else {
                nonexposedClayBlocks.add(pos);
            }
        }

        nonexposedClayBlocks.addAll(additionalClayList);

        //Add water and clay to placement
        for (BlockPos pos : waterBlocks) {
            placementMaps(pos, Blocks.WATER.defaultBlockState(), defaultDistance(pos));
        }
        for (BlockPos pos : exposedClayBlocks) {
            placementMaps(pos, Blocks.CLAY.defaultBlockState(), defaultDistance(pos));
        }
        for (BlockPos pos : nonexposedClayBlocks) {
            placementMaps(pos, Blocks.CLAY.defaultBlockState(), defaultDistance(pos));
        }
//        for (BlockPos pos : additionalClayList) {
//            placementMaps(pos, Blocks.OBSIDIAN.defaultBlockState());
//        }
    }

    private void mossCeiling() {
        //Remove patches from the ceiling map to be stone
        //Specifically, remove 1-5 patches of 3-12 blocks.
        ArrayList<BlockPos> additionalCeilingMoss = new ArrayList<>();
        for (int i = 0; i < random.nextInt(5)+1; i++) {

            Object[] posArray = ceilingMap.keySet().toArray();
            if (posArray.length == 0) {
                break;
            }

            //Pick a spot
            //Maybe just make a flat spread instead of several worms.
            BlockPos randomPos = (BlockPos) posArray[random.nextInt(posArray.length)];
            //Run 2-5 threads from that spot
            for (int k = 0; k <= random.nextInt(4)+1; k++) {
                //For each thread, size between 3 and 12
                int size = random.nextInt(10)+3;

                //Make a new SpreadPattern worm thing to delete patches of that size
                SpreadPattern pattern = new SpreadPattern(world, randomPos, size, size);
                pattern.wormMap();
                //Add mapped blocks to the bald spot
                for (BlockPos pos : pattern.returnMap().keySet()) {
                    for (int j = -2; j <= 2; j++) {
                        bareCeiling.add(pos.above(j));
                    }
                }
            }
        }

        //In patches, make the ceiling moss layer twice as thicc
        //0-1 patches of radius 2-5
        ArrayList<BlockPos> thickMossCeiling = new ArrayList<>();
        if (random.nextBoolean()) {
            Object[] posArray = ceilingMap.keySet().toArray();
            if (posArray.length != 0) {
                //Pick a spot
                BlockPos randomPos = (BlockPos) posArray[random.nextInt(posArray.length)];
                int size = random.nextInt(40)+5;
                //Make a new SpreadPattern flat pattern
                SpreadPattern pattern = new SpreadPattern(world, randomPos, size, size);
                pattern.flatMap();
                //Add mapped blocks to the thick spot
                for (BlockPos pos : pattern.returnMap().keySet()) {
                    for (int j = -3; j <= 3; j++) {
                        thickMossCeiling.add(pos.above(j));
                    }
                }
            }
        }

        //Replace the rest of the ceiling with moss
        for (BlockPos pos : ceilingMap.keySet()) {
            if (world.getBlockState(pos).is(CCBBlocks.MOSS_BLOCK.get())) {
                continue;
            }
            if (!bareCeiling.contains(pos)) {
                ceilingMossBlocks.add(pos);

                //Add 1-3 wall blocks above ceiling blocks
                if (wallMap.containsKey(pos.above())) {
                    for (int i = 1; i <= RandomGenerators.MossCeilingUp(); i++) {
                        if (wallMap.containsKey(pos.above(i))) {
                            additionalCeilingMoss.add(pos.above(i));
                        }
                    }
                }

                //Thicken in the patches
                if (thickMossCeiling.contains(pos) && world.getBlockState(pos.above()).getBlock().is(CCBBlockTags.LUSH_GROUND_REPLACEABLE)) {
                    additionalCeilingMoss.add(pos.above());
                }

                //Add wall blocks adjacent to below air blocks
                for (int i = 1; i <= RandomGenerators.MossCeilingDownAdj(); i++) {
                    if (spreadMap.containsKey(pos.below(i))) {
                        additionalCeilingMoss.addAll(wallAdjacentMap.get(pos.below(i)));
                    }
                }
            }
        }


        nonexposedMossBlocks.addAll(additionalCeilingMoss);

        //Register moss to finalPlacementMap
        for (BlockPos pos : ceilingMossBlocks) {
            placementMaps(pos, CCBBlocks.MOSS_BLOCK.get().defaultBlockState(), defaultDistance(pos));
        }
        for (BlockPos pos : nonexposedMossBlocks) {
            placementMaps(pos, CCBBlocks.MOSS_BLOCK.get().defaultBlockState(), defaultDistance(pos));
        }

    }

    private void mossFloorDecorations() {
        //When you replace stuff with moss, add chances of azalea, flowering azalea, grass, tall grass, and moss carpets.
        //8% azalea, 2% flowering
        //55% grass, 10% tall grass
        //20% moss carpet

        for (BlockPos pos : floorMossBlocks) {
            //Only place if it's still moss and also the spot above is a valid target
            //Full list of placement requirements:
            //Block is still moss in final placement map.
            boolean stillMoss = finalPlacementMap.get(pos) == CCBBlocks.MOSS_BLOCK.get().defaultBlockState();
            //Block is a replaceable block
            boolean replaceable = Util.replaceableOrAir(world, pos);
            //The block above is air
            boolean isAir = world.getBlockState(pos.above()).getMaterial() == Material.AIR;
            //The block above isn't flowing liquid. (Is there a better way to say this?)
            boolean notInFlow = world.getBlockState(pos.above()).getMaterial() != Material.WATER || world.getBlockState(pos.above()).getFluidState().isSource();
            //The block above is in the spreadmap (50% chance to waive)
            boolean spreadmap = spreadMap.containsKey(pos.above()) || random.nextBoolean();

            //Checks if there's enough space to place double grass
            boolean doubleHigh = world.getBlockState(pos.above(2)).getMaterial() == Material.AIR;


            if (stillMoss && replaceable && isAir && notInFlow && spreadmap) {
                double d = random.nextDouble();
                if (d < 0.08) {
                    //Azalea
                    placementMaps(pos.above(), CCBBlocks.AZALEA.get().defaultBlockState(), defaultDistance(pos)+1);
                }
                else if (d < 0.1) {
                    //Flowering azalea
                    placementMaps(pos.above(), CCBBlocks.FLOWERING_AZALEA.get().defaultBlockState(), defaultDistance(pos)+1);
                }
                else if (d < .65) {
                    //Grass
                    placementMaps(pos.above(),Blocks.GRASS.defaultBlockState(), defaultDistance(pos)+1);
                }
                else if (d < .75) {
                    //Tall grass
                    //If it doesn't fit, just normal grass
                    //Doesn't seem to be working?
                    if (doubleHigh) {
                        placementMaps(pos.above(),Blocks.TALL_GRASS.defaultBlockState().setValue(DoublePlantBlock.HALF, DoubleBlockHalf.LOWER), defaultDistance(pos)+1);
                        placementMaps(pos.above(2),Blocks.TALL_GRASS.defaultBlockState().setValue(DoublePlantBlock.HALF, DoubleBlockHalf.UPPER), defaultDistance(pos)+1);
                    } else {
                        placementMaps(pos.above(),Blocks.GRASS.defaultBlockState(), defaultDistance(pos)+1);
                    }
                }
                else if (d < .95) {
                    //Moss carpet
                    placementMaps(pos.above(),CCBBlocks.MOSS_CARPET.get().defaultBlockState(), defaultDistance(pos)+1);
                }


            }
        }

    }

    private void pondFloorDecorations() {
        //Dripleaf Rules:
        //Grows on clay. Chance is maybe 5% out of water, 15% underwater.
        //70-30 big/small.
        //Big ones are between 1 and 5 blocks tall.
        for (BlockPos pos : exposedClayBlocks) {
            //Full list of placement requirements:
            //Block is still clay in final placement map.
            boolean stillClay = finalPlacementMap.get(pos) == Blocks.CLAY.defaultBlockState();
            //Block is a replaceable block
            boolean replaceable = Util.replaceableOrAir(world, pos);
            //The block two blocks above isn't water or about to be water.
            boolean notDrowned = world.getBlockState(pos.above(2)).getMaterial() != Material.WATER && !waterBlocks.contains(pos.above(2));
            //The block above isn't flowing liquid. (Is there a better way to say this?)
            boolean notInFlow = world.getBlockState(pos.above()).getMaterial() != Material.WATER || world.getBlockState(pos.above()).getFluidState().isSource();
            //Block above is in fact air, water, or about to be water
            boolean exposedCheck = world.getBlockState(pos.above()).getMaterial() == Material.AIR || world.getBlockState(pos.above()).getMaterial() == Material.WATER || waterBlocks.contains(pos.above());

            if (stillClay && replaceable && notDrowned && notInFlow && exposedCheck) {
                double d = random.nextDouble();
                if (waterBlocks.contains(pos.above())) {
                    if (d < 0.1) {
                        placeDripleaf(pos);
                    }
                } else {
                    if (d < 0.05) {
                        placeDripleaf(pos);
                    }
                }
            }
        }
    }

    private void ceilingDecorations() {
        //Chance of spore blossoms and cave vines everywhere (including removed patches)
        //15% chance of cave vine
        //0.5% chance of spore blossom.
        for (BlockPos pos : ceilingMap.keySet()) {
            //Only place if it's a new moss block, or a removed patch that isn't currently moss.
            boolean place = false;
            if (ceilingMossBlocks.contains(pos)) {
                place = true;
            } else if (bareCeiling.contains(pos) && !world.getBlockState(pos).is(CCBBlocks.MOSS_BLOCK.get())) {
                place = true;
            }

            if (place) {
                double d = random.nextDouble();
                //Spore blossoms only place on moss
                if (d < 0.005 && ceilingMossBlocks.contains(pos)) {
                    placementMaps(pos.below(), CCBBlocks.SPORE_BLOSSOM.get().defaultBlockState(), defaultDistance(pos)+1);
                } else if (d < 0.15) {
                    placeCaveVine(pos);
                }
            }
        }
    }

    private void wallDecorations() {
        //Add regular vines and glow lichen to the ceiling and walls
        //About 1% chance each, but they spawn in a small patch.

        //Work from the wall list, but make sure to only place in the spread map!

        ArrayList<BlockPos> targetPositions = new ArrayList<>();
        for (BlockPos pos : ceilingMap.keySet()) {
            targetPositions.add(pos.below());
        }
        targetPositions.addAll(wallAdjacentMap.keySet());

        for (BlockPos pos : targetPositions) {
            if (finalPlacementMap.containsKey(pos)) {
                continue;
            }
            if (!filter(pos, world)) {
                continue;
            }
            if (random.nextDouble() < 0.01) {
                placePatch(pos, true, targetPositions);
            } else if (random.nextDouble() < 0.02) {
                placePatch(pos, false, targetPositions);
            }
        }
    }

    private void spawnAtOnce() {
        //Don't forget not to replace unreplaceables!\
        for (Map.Entry<BlockPos, BlockState> entry : finalPlacementMap.entrySet()) {
            if ((Util.replaceableOrAir(world, entry.getKey())) && !entry.getValue().is(SpeleogenesisBlockTags.CAVE_DECORATIONS)) {
                world.setBlock(entry.getKey(), entry.getValue(), 3);
                finalPlacementMap.remove(entry);
            }
        }
        for (Map.Entry<BlockPos, BlockState> entry : finalPlacementMap.entrySet()) {
            if (Util.replaceableOrAir(world, entry.getKey())) {
                world.setBlock(entry.getKey(), entry.getValue(), 2);
            }
        }

    }

    //Should the ripple be 3D or horizontal distance only?
    private void spawnRipple(double dist) {
        for (Map.Entry<BlockPos, Double> entry : distanceMap.entrySet()) {
            if (!finalPlacementMap.containsKey(entry.getKey())) {
                chatPrint("Tried to place a block not in the placement map", world);
            }
            else if (entry.getValue() < dist) {
                if (Util.replaceableOrAir(world, entry.getKey())) {
                    world.setBlock(entry.getKey(), finalPlacementMap.get(entry.getKey()), 2);
                }
                finalPlacementMap.remove(entry.getKey());
                distanceMap.remove(entry.getKey());
            }
        }
    }

    private void setBiome() {}

    private void removeItems() {}

    //Only if I can't get the biome to work
    private void spawnAxolotls() {}




    private void placeDripleaf(BlockPos pos) {
        //40-60 big/small.
        //Big ones are between 1 and 5 blocks tall.
        Random random = new Random();
        boolean big = random.nextDouble() < 0.4;
        Direction face = randomFacing();

        if (big) {
//            chatPrint("Marking big dripleaf", world);
            int height = RandomGenerators.dripleafHeight(ceilingHeightSubmergedClay(pos));
            if (height > 1) {
                for (int i = 1; i < height; i++) {
                    BlockState state = CCBBlocks.BIG_DRIPLEAF_STEM.get().defaultBlockState().setValue(BigDripleafStemBlock.FACING, face);
                    if (waterBlocks.contains(pos.above()) && i == 1) {
                        state = state.setValue(BlockStateProperties.WATERLOGGED, true);
                    }
                    placementMaps(pos.above(i), state, defaultDistance(pos)+i);
                }
            }
            BlockState state = CCBBlocks.BIG_DRIPLEAF.get().defaultBlockState().setValue(BigDripleafBlock.FACING, face);
            if (waterBlocks.contains(pos.above()) && height == 1) {
                state = state.setValue(BlockStateProperties.WATERLOGGED, true);
            }
            placementMaps(pos.above(height), state, defaultDistance(pos)+height);

        } else {
            //Don't place if the ceiling's too low.
            if (ceilingHeightSubmergedClay(pos) < 2) {
                return;
            }
//            chatPrint("Marking small dripleaf", world);
            BlockState state = CCBBlocks.SMALL_DRIPLEAF.get().defaultBlockState().setValue(SmallDripleafBlock.HALF, DoubleBlockHalf.LOWER).setValue(SmallDripleafBlock.FACING, face);
            if (waterBlocks.contains(pos.above())) {
                state = state.setValue(BlockStateProperties.WATERLOGGED, true);
            }
            placementMaps(pos.above(), state, defaultDistance(pos)+1);
            placementMaps(pos.above(2),CCBBlocks.SMALL_DRIPLEAF.get().defaultBlockState().setValue(DoublePlantBlock.HALF, DoubleBlockHalf.UPPER).setValue(SmallDripleafBlock.FACING, face), defaultDistance(pos)+1);
        }


    }

    //20% chance of cave vine.
    //10% of vine blocks have berries.
    private void placeCaveVine(BlockPos pos) {
        int vineLength = RandomGenerators.caveVineLength(ceilingHeight(pos));


        if (finalPlacementMap.containsKey(pos.below(vineLength))) {
//            chatPrint("Aborting cave vine for intersecting another feature", world);
            return;
        }


        for (int i = 1; i < vineLength; i++) {
            if (random.nextDouble() < 0.11) {
                placementMaps(pos.below(i), CCBBlocks.CAVE_VINES_PLANT.get().defaultBlockState().setValue(ICaveVines.BERRIES, true), defaultDistance(pos)+i);
            } else {
                placementMaps(pos.below(i), CCBBlocks.CAVE_VINES_PLANT.get().defaultBlockState().setValue(ICaveVines.BERRIES, false), defaultDistance(pos)+i);
            }
        }
        if (random.nextDouble() < 0.11) {
            placementMaps(pos.below(vineLength), CCBBlocks.CAVE_VINES.get().defaultBlockState().setValue(ICaveVines.BERRIES, true).setValue(AbstractPlantStemBlock.AGE, 25), defaultDistance(pos)+vineLength);
        } else {
            placementMaps(pos.below(vineLength), CCBBlocks.CAVE_VINES.get().defaultBlockState().setValue(ICaveVines.BERRIES, false).setValue(AbstractPlantStemBlock.AGE, 25), defaultDistance(pos)+vineLength);
        }

    }

    private void placePatch(BlockPos pos, boolean lichen, ArrayList<BlockPos> targetPositions) {
        //Sizes appear to be always 1
        int size = RandomGenerators.patchSize();
        SpreadPattern pattern = new SpreadPattern(world, pos, size, size);
        pattern.growBlockMap(100);
        for (BlockPos targetPos : pattern.returnMap().keySet()) {
            if (!world.getBlockState(targetPos).isAir(world, targetPos)) {
                continue;
            }
            if (!targetPositions.contains(targetPos)) {
                continue;
            }
            BlockState placeState = lichen ? CCBBlocks.GLOW_LICHEN.get().defaultBlockState(): Blocks.VINE.defaultBlockState();
            for (Direction direction : Direction.values()) {
                if (lichen) {
                    if (direction != Direction.DOWN && world.getBlockState(targetPos.relative(direction)).is(SpeleogenesisBlockTags.GLOW_LICHEN_PLACEMENT)) {
                        placeState = ((GlowLichenBlock)CCBBlocks.GLOW_LICHEN.get()).withDirection(placeState, world, targetPos, direction);
                    }
                } else {
                    if (direction != Direction.DOWN && VineBlock.isAcceptableNeighbour(world, targetPos.relative(direction), direction)) {
                        placeState = placeState.setValue(VineBlock.getPropertyForFace(direction), true);
                    }
                }
            }
//            placementMaps(targetPos, Blocks.LAPIS_BLOCK.defaultBlockState());
            placementMaps(targetPos, placeState, defaultDistance(pos)+2);
        }
    }

//    private void placeLichen(BlockPos pos) {
//        BlockPos.Mutable mutable = pos.mutable();
//        Iterator var7 = Arrays.stream(Direction.values()).toList().iterator();
//
//        Direction direction;
//        BlockState blockState;
//        do {
//            if (!var7.hasNext()) {
//                return false;
//            }
//
//            direction = (Direction)var7.next();
//            blockState = reader.getBlockState(mutable.setWithOffset(pos, direction));
//        } while(!config.canGrowOn(blockState.getBlock()));
//
//        GlowLichenBlock glowLichenBlock = (GlowLichenBlock)CCBBlocks.GLOW_LICHEN.get();
//        BlockState directionalState = glowLichenBlock.withDirection(state, reader, pos, direction);
//        if (directionalState == null) {
//            return false;
//        } else {
//            reader.setBlock(pos, directionalState, 3);
//            reader.getChunk(pos).markPosForPostprocessing(pos);
//            if (random.nextFloat() < config.chanceOfSpreading) {
//                glowLichenBlock.trySpreadRandomly(directionalState, reader, pos, direction, random, true);
//            }
//
//            return true;
//        }
//    }

    private void plantAzaleaTree(BlockPos pos) {

    }

    private int ceilingHeight(BlockPos pos) {
        BlockPos origin = pos;
        if (!filter(pos, world)) {
            if (spreadMap.containsKey(pos.below())) {
                origin = pos.below();
            } else if (spreadMap.containsKey(pos.above())) {
                origin = pos.above();
            } else {
                return 0;
            }
        }

        ArrayList<BlockPos> column = new ArrayList<>();
        column.add(origin);
        boolean stop = false;
        while (!stop) {
            boolean tentativeStop = true;
            ArrayList<BlockPos> toAdd = new ArrayList<>();
            for (BlockPos colpos : column) {
                if (!column.contains(colpos.below()) && filter(colpos.below(), world)) {
                    toAdd.add(colpos.below());
                    tentativeStop = false;
                } else if (!column.contains(colpos.above()) && filter(colpos.above(), world)) {
                    toAdd.add(colpos.above());
                    tentativeStop = false;
                }
            }
            stop = tentativeStop;
            column.addAll(toAdd);
        }

        return column.size();

    }

    //Takes in the pos of a piece of clay and returns the height, ignoring the block above.
    private int ceilingHeightSubmergedClay(BlockPos pos) {
        int output = 1;
        while (true) {
            if (world.getBlockState(pos.above(output + 1)).getMaterial() == Material.AIR) {
                output ++;
            } else {
                return output;
            }
        }
    }

    private void removeMossDecorations(BlockPos pos) {
        BlockState blockAbove = world.getBlockState(pos.above());
        Block bab = blockAbove.getBlock();
        if (blockAbove.getMaterial() == Material.AIR) {
            return;
        }
        if (bab == CCBBlocks.AZALEA.get() || bab == CCBBlocks.MOSS_CARPET.get() || bab == CCBBlocks.FLOWERING_AZALEA.get() || bab == Blocks.GRASS) {
            placementMaps(pos.above(), Blocks.GLASS.defaultBlockState(), defaultDistance(pos)+1);
        } else if (bab == Blocks.TALL_GRASS) {
            placementMaps(pos.above(), Blocks.GLASS.defaultBlockState(), defaultDistance(pos)+1);
            placementMaps(pos.above(2), Blocks.GLASS.defaultBlockState(), defaultDistance(pos)+1);
        }
    }

    private void removeClayDecorations(BlockPos pos) {
        for (int i = 1; i < 6; i++) {
            if (world.getBlockState(pos.above(i)).is(SpeleogenesisBlockTags.DRIPLEAF)) {
                placementMaps(pos.above(i), Blocks.AIR.defaultBlockState(), defaultDistance(pos));
            } else {
                return;
            }
        }

    }


    private void placementMaps(BlockPos pos, BlockState state, double dist) {
        finalPlacementMap.put(pos, state);
        distanceMap.put(pos, dist);
    }

    private double defaultDistance(BlockPos pos) {
        return Math.sqrt(pos.distSqr(origin));
    }


    @Override
    protected void defineSynchedData() {

    }

    @Override
    protected void readAdditionalSaveData(CompoundNBT nbt) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundNBT nbt) {
    }

    @Override
    public IPacket<?> getAddEntityPacket() {
        return new SSpawnObjectPacket(this);
    }
}
