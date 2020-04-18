package com.spleefleague.fakeblocks.packet.adapters;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
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
import net.minecraft.server.v1_15_R1.Block;
import net.minecraft.server.v1_15_R1.IBlockData;
import net.minecraft.server.v1_15_R1.SoundEffect;
import net.minecraft.server.v1_15_R1.SoundEffectType;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.craftbukkit.v1_15_R1.util.CraftMagicNumbers;
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
        PacketContainer packetContainer = event.getPacket();
        if (packetContainer.getPlayerDigTypes().read(0) != EnumWrappers.PlayerDigType.START_DESTROY_BLOCK && packetContainer.getPlayerDigTypes().read(0) != EnumWrappers.PlayerDigType.ABORT_DESTROY_BLOCK && packetContainer.getPlayerDigTypes().read(0) != EnumWrappers.PlayerDigType.STOP_DESTROY_BLOCK) {
            return;
        }
        if (event.getPlayer().getLocation().multiply(0.0).add(packetContainer.getBlockPositionModifier().read(0).toVector()).getBlock().getType() == Material.AIR && (packetContainer.getPlayerDigTypes().read(0) == EnumWrappers.PlayerDigType.STOP_DESTROY_BLOCK || packetContainer.getPlayerDigTypes().read(0) == EnumWrappers.PlayerDigType.START_DESTROY_BLOCK)) {
            Location loc = packetContainer.getBlockPositionModifier().read(0).toVector().toLocation(event.getPlayer().getWorld());
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
                if (packetContainer.getPlayerDigTypes().read(0) == EnumWrappers.PlayerDigType.STOP_DESTROY_BLOCK || broken.getType() == Material.SNOW_BLOCK && event.getPlayer().getItemInHand() != null && event.getPlayer().getInventory().getItemInMainHand().getType() == Material.DIAMOND_SHOVEL || event.getPlayer().getGameMode() == GameMode.CREATIVE) {
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
        p.spawnParticle(Particle.BLOCK_DUST, block.getLocation(), 80);
        /*
        PacketPlayOutWorldEvent packet = new PacketPlayOutWorldEvent(2001, 
                new net.minecraft.server.v1_12_R1.BlockPosition(block.getX(), block.getY(), block.getZ()), 80, false);
        ((CraftPlayer) p).getHandle().playerConnection.sendPacket((Packet) packet);
        */
    }

    private void sendBreakSound(Player p, FakeBlock b) {
        SoundEffectType effectType = breakSounds.get(b.getType());
        System.out.println("Sending block sound " + effectType);
        p.playSound(b.getLocation(), effectType.d().toString(), effectType.a() * 0.15f, effectType.b());
    }

    private boolean blockEqual(Location loc1, Location loc2) {
        if ((loc1.getX() + 0.5) / 1.0 == (loc2.getX() + 0.5) / 1.0 && (loc1.getZ() + 0.5) / 1.0 == (loc2.getZ() + 0.5) / 1.0 && loc1.getY() / 1.0 == loc2.getY() / 1.0) {
            return true;
        }
        return false;
    }

    private Map<Material, SoundEffectType> generateBreakSounds() {
        Map<Material, SoundEffectType> breakSounds = new HashMap<>();
        for (IBlockData blockData : Block.REGISTRY_ID) {
            try {
                Block block = blockData.getBlock();
                Field effectField = Block.class.getDeclaredField("stepSound");
                effectField.setAccessible(true);
                SoundEffectType effectType = (SoundEffectType) effectField.get((Object) block);
                breakSounds.put(CraftMagicNumbers.getMaterial((Block) block), effectType);
            } catch (IllegalAccessException | IllegalArgumentException | NoSuchFieldException | SecurityException ex) {
                Logger.getLogger(FakeBlockHandler.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return breakSounds;
    }
}
