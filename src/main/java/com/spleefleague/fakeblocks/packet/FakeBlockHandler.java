package com.spleefleague.fakeblocks.packet;

import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketAdapter;
import com.spleefleague.fakeblocks.FakeBlocks;
import com.spleefleague.fakeblocks.chunk.FakeBlockCache;
import com.spleefleague.fakeblocks.chunk.MultiBlockChangeHandler;
import com.spleefleague.fakeblocks.packet.adapters.BlockBreakAdapter;
import com.spleefleague.fakeblocks.packet.adapters.BlockPlaceAdapter;
import com.spleefleague.fakeblocks.packet.adapters.ChunkDataAdapter;
import com.spleefleague.fakeblocks.packet.adapters.UnloadChunkAdapter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class FakeBlockHandler implements Listener {

    private PacketAdapter chunkData;
    private PacketAdapter unloadChunk;
    private PacketAdapter breakController;
    private PacketAdapter placeController;
    private final ProtocolManager manager = FakeBlocks.getInstance().getProtocolManager();
    private final Map<UUID, Set<FakeArea>> fakeAreas;
    private final Map<UUID, FakeBlockCache> fakeBlockCache;
    private final MultiBlockChangeHandler mbchandler;

    private FakeBlockHandler() {
        this.fakeBlockCache = new HashMap<>();
        this.fakeAreas = new HashMap<>();
        this.initPacketListeners();
        this.mbchandler = MultiBlockChangeHandler.init();
        Bukkit.getOnlinePlayers().stream().filter(player -> !fakeAreas.containsKey(player.getUniqueId())).forEach(player -> {
            fakeAreas.put(player.getUniqueId(), new HashSet());
            fakeBlockCache.put(player.getUniqueId(), new FakeBlockCache());
        });
    }

    /**
     * @return The used MultiBlockChangeHandler
     */
    public MultiBlockChangeHandler getMultiBlockChangeHandler() {
        return mbchandler;
    }

    public Set<FakeBlock> getFakeBlocksForChunk(Player player, int x, int z) {
        return fakeBlockCache.get(player.getUniqueId()).getBlocks(x, z);
    }

    public Set<FakeBlock> getFakeBlocksForChunks(Player player, int[] x, int[] z) {
        return fakeBlockCache.get(player.getUniqueId()).getBlocks(x, z);
    }
    
    public void addArea(FakeArea area, Player... players) {
        addArea(area, true, players);
    }

    public void addArea(FakeArea area, boolean update, Player... players) {
        for (Player player : players) {
            fakeAreas.get(player.getUniqueId()).add(area);
            fakeBlockCache.get(player.getUniqueId()).addArea(area);
        }
        if (update) {
            mbchandler.changeBlocks(area.getBlocks().toArray(new FakeBlock[0]), players);
        }
    }
    
    public void removeArea(FakeArea area) {
        removeArea(area, true);
    }
    
    public void removeArea(FakeArea area, boolean update) {
        Collection<Player> updated = new ArrayList<>();
        fakeAreas.entrySet().forEach(e -> {
            if(e.getValue().contains(area)) {
                e.getValue().remove(area);
                Player p = Bukkit.getPlayer(e.getKey());
                if(p != null) {
                    updated.add(p);
                }
            }
        });
        recalculateCache(updated.toArray(new Player[0]));
        if (update) {
            mbchandler.changeBlocks(area.getBlocks().toArray(new FakeBlock[0]), updated.toArray(new Player[0]));
        }
    }

    public void removeArea(FakeArea area, Player... players) {
        removeArea(area, true, players);
    }

    public void removeArea(FakeArea area, boolean update, Player... players) {
        for (Player player : players) {
            fakeAreas.get(player.getUniqueId()).remove(area);
            recalculateCache(player);
        }
        if (update) {
            Collection<FakeBlock> fblocks = area.getBlocks();
            Block[] blocks = new Block[fblocks.size()];
            int i = 0;
            for (FakeBlock fblock : fblocks) {
                blocks[i++] = fblock.getLocation().getBlock();
            }
            mbchandler.changeBlocks(blocks, Material.AIR, players);
        }
    }

    public Collection<Player> getSubscribers(FakeBlock block) {
        HashSet<Player> players = new HashSet<>();
        playerLoop:
        for (Player player : Bukkit.getOnlinePlayers()) {
            for (FakeArea area : fakeAreas.get(player.getUniqueId())) {
                if (!area.getBlocks().contains(block)) {
                    continue;
                }
                players.add(player);
                continue playerLoop;
            }
        }
        return players;
    }

    public void addBlock(FakeBlock block, Player... players) {
        addBlock(block, true, players);
    }

    public void addBlock(FakeBlock block, boolean update, Player... players) {
        for (Player player : players) {
            fakeAreas.get(player.getUniqueId()).add(block);
            fakeBlockCache.get(player.getUniqueId()).addBlocks(block);
            if (!update) {
                continue;
            }
            player.sendBlockChange(block.getLocation(), block.getType(), (byte)0);
        }
    }

    public void removeBlock(FakeBlock block, Player... players) {
        removeBlock(block, true, players);
    }

    public void removeBlock(FakeBlock block, boolean update, Player... players) {
        for (Player player : players) {
            fakeAreas.get(player.getUniqueId()).remove(block);
            recalculateCache(player);
            if (!update) {
                continue;
            }
            player.sendBlockChange(block.getLocation(), Material.AIR, (byte)0);
        }
    }

    public void update(FakeArea area) {
        Collection<FakeBlock> changed = new HashSet<>(area.getBlocks());
        changed.removeIf(fb -> !fb.isModified());
        if(changed.isEmpty()) {
            return;
        }
        HashSet<Player> players = new HashSet<>();
        for (Map.Entry<UUID, Set<FakeArea>> entry : fakeAreas.entrySet()) {
            Player player;
            if (!entry.getValue().contains(area) || (player = Bukkit.getPlayer((UUID) entry.getKey())) == null) {
                continue;
            }
            players.add(player);
        }
        mbchandler.changeBlocks(changed.toArray(new FakeBlock[0]), players.toArray(new Player[0]));
        changed.forEach(fb -> fb.setSaved());
    }

    public void stop() {
        manager.removePacketListener(chunkData);
        manager.removePacketListener(unloadChunk);
        manager.removePacketListener(breakController);
        manager.removePacketListener(placeController);
        HandlerList.unregisterAll(this);
    }

    public static FakeBlockHandler init() {
        FakeBlockHandler instance = new FakeBlockHandler();
        Bukkit.getPluginManager().registerEvents(instance, FakeBlocks.getInstance());
        return instance;
    }

    private void initPacketListeners() {
        this.chunkData = new ChunkDataAdapter(this);
        this.unloadChunk = new UnloadChunkAdapter(this);
        this.breakController = new BlockBreakAdapter(this);
        placeController = new BlockPlaceAdapter(this);
        manager.addPacketListener(this.chunkData);
        manager.addPacketListener(this.unloadChunk);
        manager.addPacketListener(this.breakController);
        manager.addPacketListener(this.placeController);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (!fakeAreas.containsKey(event.getPlayer().getUniqueId())) {
            fakeAreas.put(event.getPlayer().getUniqueId(), new HashSet());
            fakeBlockCache.put(event.getPlayer().getUniqueId(), new FakeBlockCache());
        }
        else {
            recalculateCache(event.getPlayer());
        }
    }

    private void recalculateCache(Player... players) {
        for (Player player : players) {
            FakeBlockCache cache = fakeBlockCache.get(player.getUniqueId());
            cache.clear();
            for (FakeArea area : fakeAreas.get(player.getUniqueId())) {
                if (area instanceof FakeBlock) {
                    cache.addBlocks((FakeBlock) area);
                    continue;
                }
                cache.addArea(area);
            }
        }
    }
}