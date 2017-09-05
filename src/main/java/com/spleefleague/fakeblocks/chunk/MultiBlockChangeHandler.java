package com.spleefleague.fakeblocks.chunk;

import com.comphenix.packetwrapper.WrapperPlayServerMultiBlockChange;
import com.comphenix.protocol.wrappers.ChunkCoordIntPair;
import com.comphenix.protocol.wrappers.MultiBlockChangeInfo;
import com.comphenix.protocol.wrappers.WrappedBlockData;
import com.spleefleague.fakeblocks.FakeBlocks;
import com.spleefleague.fakeblocks.representations.FakeBlock;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_12_R1.CraftChunk;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 *
 * @author Jonas
 */
public class MultiBlockChangeHandler implements Listener {

    private HashMap<UUID, Collection<Chunk>> loadedChunks;

    private MultiBlockChangeHandler() {
        loadedChunks = new HashMap<>();
        Bukkit.getPluginManager().registerEvents(this, FakeBlocks.getInstance());
        for (Player player : Bukkit.getOnlinePlayers()) {
            loadedChunks.put(player.getUniqueId(), new HashSet<>());
        }
    }
    
    public static MultiBlockChangeHandler init() {
        MultiBlockChangeHandler instance = new MultiBlockChangeHandler();
        Bukkit.getPluginManager().registerEvents(instance, FakeBlocks.getInstance());
        return instance;
    }

    private void sendMultiBlockChange(MultiBlockChangeData mbcd, List<Player> affected) {
        if (!affected.isEmpty()) {
            World world = affected.get(0).getWorld();
            net.minecraft.server.v1_12_R1.Chunk chunk = ((CraftChunk) world.getChunkAt(mbcd.getChunkX(), mbcd.getChunkZ())).getHandle();
            WrapperPlayServerMultiBlockChange wrapper = new WrapperPlayServerMultiBlockChange();
            wrapper.setChunk(new ChunkCoordIntPair(chunk.locX, chunk.locZ));
            wrapper.setRecords(mbcd.getData());
            for (Player player : affected) {
                if (player != null && loadedChunks.containsKey(player.getUniqueId()) && loadedChunks.get(player.getUniqueId()).contains(mbcd.getChunk())) {
                    wrapper.sendPacket(player.getPlayer());
                }
            }
        }
    }

    public void changeBlocks(FakeBlock[] blocks, Player... affected) {
        changeBlocks(blocks, Arrays.asList(affected));
    }

    public void changeBlocks(FakeBlock[] blocks, List<Player> affected) {
        HashMap<Chunk, MultiBlockChangeData> changes = new HashMap<>();
        if (blocks != null) {
            for (FakeBlock block : blocks) {
                if (block != null) {
                    MultiBlockChangeData data;
                    if (!changes.containsKey(block.getChunk())) {
                        data = new MultiBlockChangeData(block.getChunk());
                        changes.put(block.getChunk(), data);
                    } else {
                        data = changes.get(block.getChunk());
                    }
                    data.addBlock(block.getX(), block.getY(), block.getZ(), block.getType());
                }
            }
            changes.values().stream().forEach((mbcd) -> {
                sendMultiBlockChange(mbcd, affected);
            });
        }
    }

    public void changeBlocks(Block[] blocks, Material to, Player... affected) {
        changeBlocks(blocks, to, Arrays.asList(affected));
    }

    public void changeBlocks(Block[] blocks, Material to, List<Player> affected) {
        HashMap<Chunk, MultiBlockChangeData> changes = new HashMap<>();
        if (blocks != null) {
            for (Block block : blocks) {
                if (block != null) {
                    MultiBlockChangeData data;
                    if (!changes.containsKey(block.getChunk())) {
                        data = new MultiBlockChangeData(block.getChunk());
                        changes.put(block.getChunk(), data);
                    } else {
                        data = changes.get(block.getChunk());
                    }
                    data.addBlock(block.getX(), block.getY(), block.getZ(), to);
                }
            }
            changes.values().stream().forEach((mbcd) -> {
                sendMultiBlockChange(mbcd, affected);
            });
        }
    }

    public void changeBlocks(Location pos1, Location pos2, Material to, List<Player> affected) {
        changeBlocks(getBlocksInArea(pos1, pos2).toArray(new Block[0]), to, affected);
    }

    public void changeBlocks(Location pos1, Location pos2, Material to, Player... affected) {
        changeBlocks(pos1, pos2, to, Arrays.asList(affected));
    }

    public Set<Chunk> getChunks(Block[] blocks) {
        Set<Chunk> chunks = new HashSet<>();
        for (Block block : blocks) {
            chunks.add(block.getChunk());
        }
        return chunks;
    }

    public Set<Chunk> getChunks(FakeBlock[] blocks) {
        Set<Chunk> chunks = new HashSet<>();
        for (FakeBlock block : blocks) {
            chunks.add(block.getChunk());
        }
        return chunks;
    }

    public Set<Chunk> getChunks(Location pos1, Location pos2) {
        Set<Chunk> chunks = new HashSet<>();
        Location low = new Location(pos1.getWorld(), Math.min(pos1.getBlockX(), pos2.getBlockX()), Math.min(pos1.getBlockY(), pos2.getBlockY()), Math.min(pos1.getBlockZ(), pos2.getBlockZ()));
        Location high = new Location(pos1.getWorld(), Math.max(pos1.getBlockX(), pos2.getBlockX()), Math.max(pos1.getBlockY(), pos2.getBlockY()), Math.max(pos1.getBlockZ(), pos2.getBlockZ()));
        for (int x = low.getBlockX(); x <= high.getBlockX(); x++) {
            for (int z = low.getBlockZ(); z <= high.getBlockZ(); z++) {
                chunks.add(new Location(low.getWorld(), x, 0, z).getChunk());
            }
        }
        return chunks;
    }

    public HashSet<Block> getBlocksInArea(Location pos1, Location pos2) {
        Location low = new Location(pos1.getWorld(), Math.min(pos1.getBlockX(), pos2.getBlockX()), Math.min(pos1.getBlockY(), pos2.getBlockY()), Math.min(pos1.getBlockZ(), pos2.getBlockZ()));
        Location high = new Location(pos1.getWorld(), Math.max(pos1.getBlockX(), pos2.getBlockX()), Math.max(pos1.getBlockY(), pos2.getBlockY()), Math.max(pos1.getBlockZ(), pos2.getBlockZ()));
        HashSet<Block> blocks = new HashSet<>();
        for (int x = low.getBlockX(); x <= high.getBlockX(); x++) {
            for (int y = low.getBlockY(); y <= high.getBlockY(); y++) {
                for (int z = low.getBlockZ(); z <= high.getBlockZ(); z++) {
                    blocks.add(new Location(pos1.getWorld(), x, y, z).getBlock());
                }
            }
        }
        return blocks;
    }

    @EventHandler
    public void onJoin(PlayerLoginEvent event) {
        loadedChunks.put(event.getPlayer().getUniqueId(), new HashSet<>());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        loadedChunks.remove(event.getPlayer().getUniqueId());
    }

    public void addChunk(Player player, Chunk chunk) {
        loadedChunks.get(player.getUniqueId()).add(chunk);
    }

    public void removeChunk(Player player, Chunk chunk) {
        if (loadedChunks.containsKey(player.getUniqueId())) {
            loadedChunks.get(player.getUniqueId()).remove(chunk);
        }
    }
    
    private static class MultiBlockChangeData {

        private Collection<MultiBlockChangeInfo> data = new HashSet<>();
        private final Chunk chunk;

        public MultiBlockChangeData(Chunk chunk) {
            this.chunk = chunk;
        }

        public void addBlock(int x, int y, int z, Material m) {
            x &= 15;
            z &= 15;
            WrappedBlockData bdata = WrappedBlockData.createData(m);
            MultiBlockChangeInfo mbci = new MultiBlockChangeInfo((short) (x << 12 | z << 8 | y), bdata, ChunkCoordIntPair.getConverter().getSpecific(chunk));
            data.add(mbci);
        }

        public MultiBlockChangeInfo[] getData() {
            return data.toArray(new MultiBlockChangeInfo[data.size()]);
        }

        public int getChunkX() {
            return chunk.getX();
        }

        public int getChunkZ() {
            return chunk.getZ();
        }

        public Chunk getChunk() {
            return chunk;
        }

        public short getChangedBlocks() {
            return (short) data.size();
        }
    }
}
