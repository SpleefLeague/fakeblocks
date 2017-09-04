package com.spleefleague.fakeblocks.packet.adapters;

import com.comphenix.packetwrapper.WrapperPlayServerUnloadChunk;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.spleefleague.fakeblocks.FakeBlocks;
import com.spleefleague.fakeblocks.chunk.MultiBlockChangeHandler;
import com.spleefleague.fakeblocks.packet.FakeBlockHandler;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;

/**
 *
 * @author balsfull
 */
public class UnloadChunkAdapter extends PacketAdapter {

    private final FakeBlockHandler handler;
    private final MultiBlockChangeHandler mbchandler;

    public UnloadChunkAdapter(FakeBlockHandler handler) {
        super(FakeBlocks.getInstance(), ListenerPriority.NORMAL, new PacketType[]{PacketType.Play.Server.UNLOAD_CHUNK});
        this.handler = handler;
        this.mbchandler = handler.getMultiBlockChangeHandler();
    }

    @Override
    public void onPacketReceiving(PacketEvent event) {
    }

    @Override
    public void onPacketSending(PacketEvent event) {
        WrapperPlayServerUnloadChunk wpsuc = new WrapperPlayServerUnloadChunk(event.getPacket());
        Bukkit.getScheduler().runTask(FakeBlocks.getInstance(), () -> {
            Chunk chunk = event.getPlayer().getWorld().getChunkAt(wpsuc.getChunkX(), wpsuc.getChunkZ());
            mbchandler.removeChunk(event.getPlayer(), chunk);
        });
    }
}
