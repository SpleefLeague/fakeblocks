package com.spleefleague.fakeblocks.packet.adapters;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.ChunkCoordIntPair;
import com.spleefleague.fakeblocks.FakeBlocks;
import com.spleefleague.fakeblocks.chunk.ChunkPacketUtil;
import com.spleefleague.fakeblocks.chunk.MultiBlockChangeHandler;
import com.spleefleague.fakeblocks.packet.FakeBlockHandler;
import com.spleefleague.fakeblocks.packet.FakeBlock;
import java.util.Set;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;

/**
 *
 * @author balsfull
 */
public class ChunkDataAdapter extends PacketAdapter {

    private final FakeBlockHandler handler;
    private final MultiBlockChangeHandler mbchandler;
    
    public ChunkDataAdapter(FakeBlockHandler handler) {
        super(FakeBlocks.getInstance(), ListenerPriority.NORMAL, new PacketType[]{PacketType.Play.Server.MAP_CHUNK});
        this.handler = handler;
        this.mbchandler = handler.getMultiBlockChangeHandler();
    }

    @Override
    public void onPacketReceiving(PacketEvent event) {
    }

    /**
     * Play Server Map Chunk packet
     * @param event Packet Event
     */
    @Override
    public void onPacketSending(PacketEvent event) {
        PacketContainer packetContainer = event.getPacket();
        ChunkCoordIntPair chunkCoord = packetContainer.getChunkCoordIntPairs().read(0);
        Bukkit.getScheduler().runTask(FakeBlocks.getInstance(), () -> {
            Chunk chunk = event.getPlayer().getWorld().getChunkAt(chunkCoord.getChunkX(), chunkCoord.getChunkZ());
            mbchandler.addChunk(event.getPlayer(), chunk);
        });
        Set<FakeBlock> blocks = handler.getFakeBlocksForChunk(event.getPlayer(), chunkCoord.getChunkX(), chunkCoord.getChunkZ());
        if (blocks != null) {
            ChunkPacketUtil.setBlocksPacketMapChunk(event.getPlayer().getWorld(), event.getPacket(), blocks);
        }
    }
}
