package syric.speleomancer.lush;

import com.blackgear.cavesandcliffs.common.blocks.*;
import com.blackgear.cavesandcliffs.core.other.tags.CCBBlockTags;
import com.blackgear.cavesandcliffs.core.registries.CCBBlocks;
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
import syric.speleomancer.SpeleomancerBlockTags;
import syric.speleomancer.util.SpreadPattern;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static syric.speleomancer.Speleomancer.chatPrint;
import static syric.speleomancer.util.Util.filter;
import static syric.speleomancer.util.Util.randomFacing;

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
    //Make a way to find the wall blocks adjacent to a given spread block, so that moss can get spread onto the walls.
    private final ConcurrentHashMap<BlockPos, Double> wallMap = new ConcurrentHashMap<>();

    //A way to designate part of a larger area as pond? Perhaps pondFloor takes in a subset of horizontal area in which it's allowed to work.

    private final ArrayList<BlockPos> waterBlocks = new ArrayList<>();
    private final ArrayList<BlockPos> clayBlocks = new ArrayList<>();
    private final ArrayList<BlockPos> floorMossBlocks = new ArrayList<>();
    private final ArrayList<BlockPos> ceilingMossBlocks = new ArrayList<>();


    private boolean isOutdoors = false;
    private boolean plantedTree = false;

    private boolean foundOrigin = false;
    private boolean completedSpread = false;
    private boolean processedMap = false;
    private boolean purgedDecorations = false;
    private boolean generateFloor = false;
    private boolean generateCeiling = false;
    private boolean generateDecorations =false;
    private boolean placed = false;
    private boolean setBiome = false;
    private boolean removedItems = false;

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

        ticks++;

        //Something to make it spread the actions out over ticks! "if tick % 10 == 0"?
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
                    plantAzalea(origin);
                }
                else {
                    chatPrint("All done, deleting entity", world);
                    this.kill();
                }
            }

            //Spread into an area
            else if (!completedSpread) {
                if (pattern == null) {
                    pattern = new SpreadPattern(world, origin, 4200, 15);
                }
                //Modify this. Too laggy = smaller, too long = larger.
                int currentSize = pattern.blockFillingMap.size();
                pattern.growBlockMap(50000);
                if (pattern.blockFillingMap.size() == currentSize) {
                    spreadAttempts++;
                }
                if (spreadAttempts > 3 || pattern.done) {
//                    chatPrint("Can't spread any further", world);
                    completedSpread = true;
                    this.spreadMap = pattern.returnMap();
                }
            }

            //Remove all blocks more than 10 blocks away
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

            //I wrote some code that deletes decorations above water. Perhaps all decorations should be purged first?
            //If I add 'air' to the placement map, some stuff that checks if stuff's in the placement map could break.
            //Investigate this.
            else if (!purgedDecorations) {
//                purgeDecorations();
                purgedDecorations = true;
                chatPrint("Purged decorations", world);
            }

            //Place floor: moss, clay, water
            //FIGURE OUT WHAT REPLACES WHAT AND WHAT DOESN'T
            else if (!generateFloor) {
                if (random.nextDouble() < 0.3) {
                    pondFloor();
                } else {
                    mossFloor();
                }
                generateFloor = true;
                chatPrint("Generated floor", world);
            }

            //Place ceiling moss blocks
            else if (!generateCeiling) {
                mossCeiling();
                generateCeiling = true;
                chatPrint("Generated ceiling", world);
            }

            //Place decorations: vines, dripleaf, grass, etc.
            else if (!generateDecorations) {
                mossFloorDecorations();
                pondFloorDecorations();
                ceilingDecorations();
//                wallDecorations();
                generateDecorations = true;
                chatPrint("Generated decorations", world);
            }

            else if (!placed) {
                spawnAtOnce();
                placed = true;
                chatPrint("Placed blocks", world);

//                rippleDist = rippleDist + 1.0;
//                spawnRipple(rippleDist);
//                if (rippleDist >= 15) {
//                    placed = true;
//                    chatPrint("Finished placing blocks", world);
//                }
            }

            //Remove any dropped items
            else if (!removedItems) {
//                removeItems();
                removedItems = true;
                chatPrint("Removed items", world);
            }


            else {
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
            if (entry.getValue() > 10) {
                spreadMap.remove(entry.getKey());
            }
        }
    }

    private void createCandidateMap() {
        for (Map.Entry<BlockPos, Double> entry : spreadMap.entrySet()) {
            for (Direction direction : Direction.values()) {
                BlockPos candidatePos = entry.getKey().relative(direction);
                boolean replaceable = world.getBlockState(candidatePos).getBlock().is(CCBBlockTags.LUSH_GROUND_REPLACEABLE);
                boolean notavine = !(world.getBlockState(candidatePos).getBlock() instanceof ICaveVines);


                if (replaceable && notavine) {
                    replaceCandidateMap.putIfAbsent(candidatePos, candidatePos.distSqr(origin));
                }
            }
        }
    }

    private void divideCandidateMap() {
        for (Map.Entry<BlockPos, Double> entry : replaceCandidateMap.entrySet()) {
            boolean up = filter(entry.getKey().above(), world);
            boolean down = filter(entry.getKey().below(), world);
            int numSidesExposed = 0;
            for (Direction direction : Direction.values()) {
                if (direction != Direction.UP && direction != Direction.DOWN) {
                    if (filter(entry.getKey().relative(direction), world)) {
                        numSidesExposed ++;
                    }
                }
            }


            if (down) {
                ceilingMap.putIfAbsent(entry.getKey(), entry.getValue());
            }
            else if (!up) {
                wallMap.putIfAbsent(entry.getKey(), entry.getValue());
            }
            else if (numSidesExposed == 0) {
                floorMap.putIfAbsent(entry.getKey(), entry.getValue());
            }
            else {
                floorCornerMap.putIfAbsent(entry.getKey(), entry.getValue());
            }
        }
    }

    private void mossFloor() {
        //Elsewhere, replace floor and floorcorner with patches of moss.
        //Basically everything should be moss.

        floorMossBlocks.addAll(floorMap.keySet());
        floorMossBlocks.addAll(floorCornerMap.keySet());

        ArrayList<BlockPos> toAdd = new ArrayList<>();
        ArrayList<BlockPos> toRemove = new ArrayList<>();
        for (BlockPos pos : floorMossBlocks) {
            //Remove any clay and water (don't replace them with moss)
            if (world.getBlockState(pos).is(Blocks.CLAY)) {
                toRemove.add(pos);
                clayBlocks.add(pos);
                continue;
            } else if (world.getBlockState(pos).is(Blocks.WATER)) {
                toRemove.add(pos);
                waterBlocks.add(pos);
                continue;
            }

            //Add a bit where it tries to add nearby wall blocks to toAdd.


            finalPlacementMap.put(pos, CCBBlocks.MOSS_BLOCK.get().defaultBlockState());
        }
        for (BlockPos pos : toRemove) {
            floorMossBlocks.remove(pos);
        }


        for (BlockPos pos : toAdd) {
            floorMossBlocks.add(pos);
            finalPlacementMap.put(pos, CCBBlocks.MOSS_BLOCK.get().defaultBlockState());
        }
    }

    private void pondFloor() {
        //Replace floor with water
        for (Map.Entry<BlockPos, Double> entry : floorMap.entrySet()) {
            //Don't replace moss.
            if (world.getBlockState(entry.getKey()).is(CCBBlocks.MOSS_BLOCK.get())) {
                floorMossBlocks.add(entry.getKey());
            } else {
                waterBlocks.add(entry.getKey());
            }
        }

        //Add clay below and around water
        for (BlockPos pos : waterBlocks) {
            for (Direction direction : Direction.values()) {
                if (direction != Direction.UP && !waterBlocks.contains(pos.relative(direction))) {
                    //Don't replace moss, unless it's under the water.
                    if (world.getBlockState(pos.relative(direction)).is(CCBBlocks.MOSS_BLOCK.get()) && direction != Direction.DOWN) {
                        floorMossBlocks.add(pos.relative(direction));
                    } else {
                        clayBlocks.add(pos.relative(direction));
                    }
                }
            }
        }
        clayBlocks.addAll(floorCornerMap.keySet());

        //Edge water blocks have a chance of being replaced with clay.
        Map<Integer, Double> removalChance = new HashMap<>();
        removalChance.put(0, 0.0);
        removalChance.put(1, 0.1);
        removalChance.put(2, 0.3);
        removalChance.put(3, 0.5);
        removalChance.put(4, 0.9);
        for (Iterator<BlockPos> it = waterBlocks.iterator(); it.hasNext(); ) {
            BlockPos pos = it.next();
            int nonwaterblocks = 0;
            for (Direction direction : Direction.values()) {
                if (direction.getAxis() != Direction.Axis.Y) {
                    if (!waterBlocks.contains(pos.relative(direction))) {
                        nonwaterblocks++;
                    }
                }
            }
            double remove = removalChance.get(nonwaterblocks);
            if (random.nextDouble() < remove) {
                it.remove();
                clayBlocks.add(pos);
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

                //If it's more than 30 blocks, 50% chance of just removing 20-60%.
                if (toRemove.size() > 30 && random.nextDouble() < 0.5) {
                    BlockPos furthestPos = randomPos;

                    //Find the block in the pool furthest away from the one selected, to ensure we start at the edge.
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
                    clayBlocks.add(removePos);
                }
            }
        }

        ArrayList<BlockPos> toAdd = new ArrayList<>();
        //Add water and clay to placement
        for (BlockPos pos : waterBlocks) {
            finalPlacementMap.put(pos, Blocks.WATER.defaultBlockState());
            //Delete decorations above water
            if (world.getBlockState(pos.above()).is(SpeleomancerBlockTags.CAVE_DECORATIONS)) {
                finalPlacementMap.put(pos.above(), Blocks.AIR.defaultBlockState());
            }
        }
        for (BlockPos pos : clayBlocks) {
            //Add wall blocks above
            if (wallMap.containsKey(pos.above())) {
                toAdd.add(pos.above());
            }

            //Add wall blocks adjacent to above blocks

            //3% chance of not replacing moss REMOVE!
            //3% chance of replacing non-underwater clay with moss DO THIS INSTEAD!
            if (random.nextDouble() < 0.97 || finalPlacementMap.get(pos) != CCBBlocks.MOSS_BLOCK.get().defaultBlockState() || waterBlocks.contains(pos.above())) {
                finalPlacementMap.put(pos, Blocks.CLAY.defaultBlockState());
            }
        }
        for (BlockPos pos : toAdd) {
            if (random.nextDouble() < 0.97 || finalPlacementMap.get(pos) != CCBBlocks.MOSS_BLOCK.get().defaultBlockState() || waterBlocks.contains(pos.above())) {
                finalPlacementMap.put(pos, Blocks.CLAY.defaultBlockState());
            }
            clayBlocks.add(pos);
        }
    }

    private void mossCeiling() {
        //Remove patches from the ceiling map to be stone (make smaller map that includes them for later)
        //Specifically, remove 1-5 patches of 3-12 blocks (size roll with disadvantage).
        ArrayList<BlockPos> mosslessCeiling = new ArrayList<>();
        for (int i = 0; i < random.nextInt(5)+1; i++) {

            Object[] posArray = ceilingMap.keySet().toArray();
            if (posArray.length == 0) {
                break;
            }
            BlockPos randomPos = (BlockPos) posArray[random.nextInt(posArray.length)];
//            int size = Math.min(random.nextInt(10)+3, random.nextInt(10)+3);
            int size = random.nextInt(10)+3;


            //Make a new SpreadPattern thing to delete patches of that size
            SpreadPattern pattern = new SpreadPattern(world, randomPos, size, size);
            pattern.wormMap();
            for (BlockPos pos : pattern.returnMap().keySet()) {
                for (int j = -2; j <= 2; j++) {
                    mosslessCeiling.add(pos.above(i));
                }
            }
        }

        //Replace the rest of the ceiling with moss
        for (BlockPos pos : ceilingMap.keySet()) {
            if (!mosslessCeiling.contains(pos)) {
                ceilingMossBlocks.add(pos);
            } else {
                //Testing:
                finalPlacementMap.put(pos, Blocks.REDSTONE_BLOCK.defaultBlockState());
            }
        }

        //Add 1-3 wall blocks above each mossy ceiling block to the moss list, if there are any
        //Also a chance of 1-2 nonwall blocks
        ArrayList<BlockPos> toAdd = new ArrayList<>();
        for (BlockPos pos : ceilingMossBlocks) {
            //Add 1-3 wall blocks above ceiling blocks
            if (wallMap.containsKey(pos.above())) {
                for (int i = 1; i <= random.nextInt(2) + 1; i++) {
                    if (wallMap.containsKey(pos.above(i))) {
                        toAdd.add(pos.above(i));
                    }
                }
            }
            else {
                //20% chance of adding solid blocks above
                if (random.nextDouble() < 0.2) {
                    if (world.getBlockState(pos.above()).getMaterial().isSolid()) {
                        toAdd.add(pos.above());
                    }
                    //20% chance of adding the block above that
                    if (random.nextDouble() < 0.2) {
                        if (world.getBlockState(pos.above(2)).getMaterial().isSolid()) {
                            toAdd.add(pos.above(2));
                        }
                    }
                }
            }
        }

        ceilingMossBlocks.addAll(toAdd);

        //Register moss to finalPlacementMap
        for (BlockPos pos : ceilingMossBlocks) {
            finalPlacementMap.put(pos, CCBBlocks.MOSS_BLOCK.get().defaultBlockState());
        }

    }

    private void mossFloorDecorations() {
        //When you replace stuff with moss, add chances of azalea, flowering azalea, grass, tall grass, and moss carpets.
        //8% azalea, 2% flowering
        //55% grass, 10% tall grass
        //20% moss carpet

        for (BlockPos pos : floorMossBlocks) {
            //Only place if it's still moss and also the spot above is a valid target
            if (finalPlacementMap.get(pos) == CCBBlocks.MOSS_BLOCK.get().defaultBlockState() && spreadMap.containsKey(pos.above())) {
                double d = random.nextDouble();
                if (d < 0.08) {
                    //Azalea
                    finalPlacementMap.put(pos.above(), CCBBlocks.AZALEA.get().defaultBlockState());
                }
                else if (d < 0.1) {
                    //Flowering azalea
                    finalPlacementMap.put(pos.above(), CCBBlocks.FLOWERING_AZALEA.get().defaultBlockState());
                }
                else if (d < .65) {
                    //Grass
                    finalPlacementMap.put(pos.above(),Blocks.GRASS.defaultBlockState());
                }
                else if (d < .75) {
                    //Tall grass
                    //If it doesn't fit, just normal grass
                    //Doesn't seem to be working?
                    if (spreadMap.containsKey(pos.above(2))) {
                        finalPlacementMap.put(pos.above(),Blocks.TALL_GRASS.defaultBlockState().setValue(DoublePlantBlock.HALF, DoubleBlockHalf.LOWER));
                        finalPlacementMap.put(pos.above(2),Blocks.TALL_GRASS.defaultBlockState().setValue(DoublePlantBlock.HALF, DoubleBlockHalf.UPPER));
                    } else {
                        finalPlacementMap.put(pos.above(),Blocks.GRASS.defaultBlockState());
                    }
                }
                else if (d < .95) {
                    //Moss carpet
                    finalPlacementMap.put(pos.above(),CCBBlocks.MOSS_CARPET.get().defaultBlockState());
                }


            }
        }

    }

    private void pondFloorDecorations() {
        //Dripleaf Rules:
        //Grows on clay. Chance is maybe 5% out of water, 15% underwater.
        //70-30 big/small.
        //Big ones are between 1 and 5 blocks tall.
        for (BlockPos pos : clayBlocks) {
            if (finalPlacementMap.get(pos) == Blocks.CLAY.defaultBlockState() && (spreadMap.containsKey(pos.above()) || waterBlocks.contains(pos.above()))) {
                double d = random.nextDouble();
                if (waterBlocks.contains(pos.above())) {
                    if (d < 0.15) {
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
        //Chance of spore blossoms and glowberry vines everywhere (including removed patches)
        //20% chance of glowberry vine. Usually 2-5 blocks long, but 10% of them can be up to 10 blocks long.
        //0.5% chance of spore blossom.
        for (BlockPos pos : ceilingMap.keySet()) {
            double d = random.nextDouble();
            if (d < 0.005) {
                finalPlacementMap.put(pos.below(), CCBBlocks.SPORE_BLOSSOM.get().defaultBlockState());
            } else if (d < 0.2) {
                placeCaveVine(pos);
            }
        }
    }

    private void wallDecorations() {
        //Add regular vines and glow lichen to the ceiling and walls
        //About 1% chance each, but they spawn in a small patch.

        //Work from the wall list, but make sure to only place in the spread map!

        ArrayList<BlockPos> ceilingAndWall = new ArrayList<>();
        ceilingAndWall.addAll(ceilingMap.keySet());
        ceilingAndWall.addAll(wallMap.keySet());

        for (BlockPos pos : ceilingAndWall) {
            if (random.nextDouble() < 0.01) {
                placePatch(pos, true);
            } else if (random.nextDouble() < 0.02) {
                placePatch(pos, false);
            }
        }
    }

    private void spawnAtOnce() {
        //Don't forget not to replace unreplaceables!
        for (Map.Entry<BlockPos, BlockState> entry : finalPlacementMap.entrySet()) {
            if (world.getBlockState(entry.getKey()).is(CCBBlockTags.LUSH_GROUND_REPLACEABLE) || world.getBlockState(entry.getKey()).getMaterial() == Material.AIR) {
                world.setBlock(entry.getKey(), entry.getValue(), 3);
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
                if (world.getBlockState(entry.getKey()).is(CCBBlockTags.LUSH_GROUND_REPLACEABLE) || world.getBlockState(entry.getKey()).getMaterial() == Material.AIR) {
                    world.setBlock(entry.getKey(), finalPlacementMap.get(entry.getKey()), 3);
                }
            }
        }
    }

    private void setBiome() {}

    private void removeItems() {}

    //Only if I can't get the biome to work
    private void spawnAxolotls() {}




    private void placeDripleaf(BlockPos pos) {
        //30-70 big/small.
        //Big ones are between 1 and 5 blocks tall.
        Random random = new Random();
        boolean big = random.nextDouble() < 0.3;
        Direction face = randomFacing();

        if (big) {
//            chatPrint("Marking big dripleaf", world);
            int height = Math.min(random.nextInt(5) + 1, random.nextInt(5) + 1);
            height = Math.min(height, ceilingHeightSubmergedClay(pos));
            finalPlacementMap.put(pos.above(height), CCBBlocks.BIG_DRIPLEAF.get().defaultBlockState().setValue(BigDripleafBlock.FACING, face));
            if (height > 1) {
                for (int i = 1; i < height; i++) {
                    finalPlacementMap.put(pos.above(i), CCBBlocks.BIG_DRIPLEAF_STEM.get().defaultBlockState().setValue(BigDripleafStemBlock.FACING, face));
                }
            }
        } else {
//            chatPrint("Marking small dripleaf", world);
            finalPlacementMap.put(pos.above(),CCBBlocks.SMALL_DRIPLEAF.get().defaultBlockState().setValue(DoublePlantBlock.HALF, DoubleBlockHalf.LOWER).setValue(SmallDripleafBlock.FACING, face));
            finalPlacementMap.put(pos.above(2),CCBBlocks.SMALL_DRIPLEAF.get().defaultBlockState().setValue(DoublePlantBlock.HALF, DoubleBlockHalf.LOWER).setValue(SmallDripleafBlock.FACING, face));
        }
        if (waterBlocks.contains(pos.above())) {
            finalPlacementMap.put(pos.above(), finalPlacementMap.get(pos.above()).setValue(BlockStateProperties.WATERLOGGED, true));
        }

    }

    //20% chance of glowberry vine. Usually 1-5 blocks long, but 10% of them can be up to 10 blocks long.
    //50% of vines have berries on 30% of their blocks.
    private void placeCaveVine(BlockPos pos) {
        int height = ceilingHeight(pos);
        int vineLength = 1;
        boolean hasBerries = random.nextBoolean();

        double d = random.nextDouble();
        //Place big cave vine
        if (d < 0.1) {
            vineLength += random.nextInt(10);
        }
        //Place small cave vine
        else {
            vineLength += random.nextInt(5);
        }
        vineLength = Math.min(vineLength, height);


        if (finalPlacementMap.containsKey(pos.below(vineLength))) {
//            chatPrint("Aborting cave vine for intersecting another feature", world);
            return;
        }


        for (int i = 1; i < vineLength; i++) {
            if (random.nextDouble() < 0.3 && hasBerries) {
                finalPlacementMap.put(pos.below(i), CCBBlocks.CAVE_VINES_PLANT.get().defaultBlockState().setValue(ICaveVines.BERRIES, true));
            } else {
                finalPlacementMap.put(pos.below(i), CCBBlocks.CAVE_VINES_PLANT.get().defaultBlockState().setValue(ICaveVines.BERRIES, false));
            }
        }
        if (random.nextDouble() < 0.3 && hasBerries) {
            finalPlacementMap.put(pos.below(vineLength), CCBBlocks.CAVE_VINES.get().defaultBlockState().setValue(ICaveVines.BERRIES, true).setValue(AbstractPlantStemBlock.AGE, 25));
        } else {
            finalPlacementMap.put(pos.below(vineLength), CCBBlocks.CAVE_VINES.get().defaultBlockState().setValue(ICaveVines.BERRIES, false).setValue(AbstractPlantStemBlock.AGE, 25));
        }

    }

    private void placePatch(BlockPos pos, Boolean lichen) {
        BlockPos origin = pos;
        boolean succeeded = false;

        //Sizes appear to be always 1
        int size = random.nextInt(10)+1;
        Block placeBlock = lichen ? CCBBlocks.GLOW_LICHEN.get() : Blocks.VINE;
        BlockState placeState = placeBlock.defaultBlockState();
        for (Direction direction : Direction.values()) {
            if (spreadMap.containsKey(pos.relative(direction))) {
                succeeded = true;
                origin = pos.relative(direction);
            }
        }
        if (succeeded)  {
            SpreadPattern pattern = new SpreadPattern(world, origin, size, size);
            pattern.growBlockMap(100);
            for (BlockPos targetPos : pattern.returnMap().keySet()) {
                //If adjacent to ceiling or wall
                boolean adjacent = false;
                for (Direction direction : Direction.values()) {
                    if (ceilingMap.containsKey(targetPos.relative(direction)) || wallMap.containsKey(targetPos.relative(direction))) {
                        adjacent = true;
                        break;
                    }
                }

                //If nothing is there yet
                boolean unoccupied = !finalPlacementMap.containsKey(pos);

                if (unoccupied && adjacent) {
                    finalPlacementMap.put(targetPos, Blocks.LAPIS_BLOCK.defaultBlockState());
//                    finalPlacementMap.put(targetPos, placeState);
                }
            }
        }

    }

    private void plantAzalea(BlockPos pos) {}


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

    private int ceilingHeightSubmergedClay(BlockPos pos) {
        pos = pos.above();
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
            column.addAll(toAdd);
            stop = tentativeStop;
        }

        return column.size();

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
