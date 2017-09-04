package com.spleefleague.fakeblocks;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import org.bukkit.plugin.java.JavaPlugin;

/**
 *
 * @author balsfull
 */
public class FakeBlocks extends JavaPlugin {

    private static FakeBlocks instance;
    private ProtocolManager manager;
    
    @Override
    public void onEnable() {
        instance = this;
        manager = ProtocolLibrary.getProtocolManager();
    }
    
    public ProtocolManager getProtocolManager() {
        return manager;
    }
    
    public static FakeBlocks getInstance() {
        return instance;
    }
}
