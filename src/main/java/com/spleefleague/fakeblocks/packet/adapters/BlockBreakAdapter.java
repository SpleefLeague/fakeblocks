package com.spleefleague.fakeblocks.packet.adapters;

import com.comphenix.packetwrapper.WrapperPlayClientBlockDig;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.spleefleague.fakeblocks.FakeBlocks;
import com.spleefleague.fakeblocks.events.FakeBlockBreakEvent;
import com.spleefleague.fakeblocks.packet.FakeBlockHandler;
import com.spleefleague.fakeblocks.packet.FakeBlock;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.minecraft.server.v1_12_R1.EntityPlayer;
import net.minecraft.server.v1_12_R1.Packet;
import net.minecraft.server.v1_12_R1.PacketPlayOutWorldEvent;
import net.minecraft.server.v1_12_R1.SoundEffectType;
import org.bukkit.craftbukkit.v1_12_R1.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_12_R1.util.CraftMagicNumbers;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;

/**
 *
 * @author balsfull
 */
public class BlockBreakAdapter extends PacketAdapter {

    private final FakeBlockHandler handler;

    private final Map<Material, SoundEffectType> breakSounds;

    public BlockBreakAdapter(FakeBlockHandler handler) {
        super(FakeBlocks.getInstance(), ListenerPriority.NORMAL, new PacketType[]{PacketType.Play.Client.BLOCK_DIG});
        this.handler = handler;
        breakSounds = generateBreakSounds();
    }

    @Override
    public void onPacketReceiving(PacketEvent event) {
        WrapperPlayClientBlockDig wrapper = new WrapperPlayClientBlockDig(event.getPacket());
        if (wrapper.getStatus() != EnumWrappers.PlayerDigType.START_DESTROY_BLOCK && wrapper.getStatus() != EnumWrappers.PlayerDigType.ABORT_DESTROY_BLOCK && wrapper.getStatus() != EnumWrappers.PlayerDigType.STOP_DESTROY_BLOCK) {
            return;
        }
        if (event.getPlayer().getLocation().multiply(0.0).add(wrapper.getLocation().toVector()).getBlock().getType() == Material.AIR && (wrapper.getStatus() == EnumWrappers.PlayerDigType.STOP_DESTROY_BLOCK || wrapper.getStatus() == EnumWrappers.PlayerDigType.START_DESTROY_BLOCK)) {
            Location loc = wrapper.getLocation().toVector().toLocation(event.getPlayer().getWorld());
            FakeBlock broken = null;
            Chunk chunk = loc.getChunk();
            Set<FakeBlock> fakeBlocks = handler.getFakeBlocksForChunk(event.getPlayer(), chunk.getX(), chunk.getZ());
            if (fakeBlocks != null) {
                for (FakeBlock block : fakeBlocks) {
                    if (blockEqual(loc, block.getLocation())) {
                        broken = block;
                        break;
                    }
                }
            }
            if (broken != null) {
                if (wrapper.getStatus() == EnumWrappers.PlayerDigType.STOP_DESTROY_BLOCK || broken.getType() == Material.SNOW_BLOCK && event.getPlayer().getItemInHand() != null && event.getPlayer().getItemInHand().getType() == Material.DIAMOND_SPADE || event.getPlayer().getGameMode() == GameMode.CREATIVE) {
                    FakeBlockBreakEvent fbbe = new FakeBlockBreakEvent(broken, event.getPlayer());
                    Bukkit.getPluginManager().callEvent((Event) fbbe);
                    if (fbbe.isCancelled()) {
                        event.getPlayer().sendBlockChange(fbbe.getBlock().getLocation(), fbbe.getBlock().getType(), fbbe.getBlock().getDamageValue());
                    }
                    else {
                        broken.setType(Material.AIR);
                        for (Player subscriber : handler.getSubscribers(broken)) {
                            if (subscriber == event.getPlayer()) {
                                continue;
                            }
                            subscriber.sendBlockChange(fbbe.getBlock().getLocation(), Material.AIR, (byte) 0);
                            sendBreakParticles(subscriber, broken);
                            sendBreakSound(subscriber, broken);
                        }
                    }
                }
                else {
                    event.setCancelled(true);
                }
            }
        }
    }

    @Override
    public void onPacketSending(PacketEvent event) {
    }

    private void sendBreakParticles(Player p, FakeBlock block) {
        PacketPlayOutWorldEvent packet = new PacketPlayOutWorldEvent(2001, new net.minecraft.server.v1_12_R1.BlockPosition(block.getX(), block.getY(), block.getZ()), 80, false);
        ((CraftPlayer) p).getHandle().playerConnection.sendPacket((Packet) packet);
    }

    private void sendBreakSound(Player p, FakeBlock b) {
        EntityPlayer entity = ((CraftPlayer) p).getHandle();
        SoundEffectType effectType = breakSounds.get(b.getType());
        entity.a(effectType.d(), effectType.a() * 0.15f, effectType.b());
    }

    private boolean blockEqual(Location loc1, Location loc2) {
        if ((loc1.getX() + 0.5) / 1.0 == (loc2.getX() + 0.5) / 1.0 && (loc1.getZ() + 0.5) / 1.0 == (loc2.getZ() + 0.5) / 1.0 && loc1.getY() / 1.0 == loc2.getY() / 1.0) {
            return true;
        }
        return false;
    }

    private Map<Material, SoundEffectType> generateBreakSounds() {
        Map<Material, SoundEffectType> breakSounds = new HashMap<>();
        for (net.minecraft.server.v1_12_R1.Block block : net.minecraft.server.v1_12_R1.Block.REGISTRY) {
            try {
                Field effectField = net.minecraft.server.v1_12_R1.Block.class.getDeclaredField("stepSound");
                effectField.setAccessible(true);
                SoundEffectType effectType = (SoundEffectType) effectField.get((Object) block);
                breakSounds.put(CraftMagicNumbers.getMaterial((net.minecraft.server.v1_12_R1.Block) block), effectType);
            } catch (IllegalAccessException | IllegalArgumentException | NoSuchFieldException | SecurityException ex) {
                Logger.getLogger(FakeBlockHandler.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return breakSounds;
    }
}
