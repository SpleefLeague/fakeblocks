package com.spleefleague.fakeblocks.packet.adapters;

import com.comphenix.packetwrapper.WrapperPlayClientUseItem;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.spleefleague.fakeblocks.FakeBlocks;
import com.spleefleague.fakeblocks.packet.FakeBlockHandler;
import com.spleefleague.fakeblocks.representations.FakeBlock;
import java.util.Set;
import org.bukkit.Location;

/**
 *
 * @author balsfull
 */
public class BlockPlaceAdapter extends PacketAdapter {

    private final FakeBlockHandler handler;

    public BlockPlaceAdapter(FakeBlockHandler handler) {
        super(FakeBlocks.getInstance(), ListenerPriority.NORMAL, new PacketType[]{PacketType.Play.Client.USE_ITEM});
        this.handler = handler;
    }

    @Override
    public void onPacketReceiving(PacketEvent event) {
        WrapperPlayClientUseItem wrapper = new WrapperPlayClientUseItem(event.getPacket());
        if (wrapper.getLocation().getY() < 0) {
            return;
        }
        Location loc = wrapper.getLocation().toVector().toLocation(event.getPlayer().getWorld());
        int chunkX = wrapper.getLocation().getX() >> 4;
        int chunkZ = wrapper.getLocation().getZ() >> 4;
        Set<FakeBlock> fakeBlocks = handler.getFakeBlocksForChunk(event.getPlayer(), chunkX, chunkZ);
        if (fakeBlocks != null) {
            for (FakeBlock fakeBlock : fakeBlocks) {
                if (blockEqual(fakeBlock.getLocation(), loc)) {
                    event.setCancelled(true);
                    break;
                }
            }
        }
    }

    @Override
    public void onPacketSending(PacketEvent event) {

    }

    private boolean blockEqual(Location loc1, Location loc2) {
        if ((loc1.getX() + 0.5) / 1.0 == (loc2.getX() + 0.5) / 1.0 && (loc1.getZ() + 0.5) / 1.0 == (loc2.getZ() + 0.5) / 1.0 && loc1.getY() / 1.0 == loc2.getY() / 1.0) {
            return true;
        }
        return false;
    }
}