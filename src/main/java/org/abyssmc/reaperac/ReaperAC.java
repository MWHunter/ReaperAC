package org.abyssmc.reaperac;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import org.abyssmc.reaperac.checks.movement.MovementVelocityCheck;
import org.abyssmc.reaperac.events.anticheat.GenericMovementCheck;
import org.abyssmc.reaperac.events.bukkit.PlayerJoinLeaveListener;
import org.abyssmc.reaperac.events.bukkit.PlayerLagback;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;

public final class ReaperAC extends JavaPlugin {
    public static HashMap<Player, GrimPlayer> playerGrimHashMap = new HashMap<>();
    public static Plugin plugin;
    ProtocolManager manager;

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    @Override
    public void onEnable() {
        // Plugin startup logic
        plugin = this;
        manager = ProtocolLibrary.getProtocolManager();

        registerEvents();
        registerPackets();
        registerChecks();

        for (Player player : Bukkit.getOnlinePlayers()) {
            playerGrimHashMap.put(player, new GrimPlayer(player));
        }

    }

    public void registerEvents() {
        Bukkit.getPluginManager().registerEvents(new PlayerJoinLeaveListener(), this);
        Bukkit.getPluginManager().registerEvents(new PlayerLagback(), this);
        Bukkit.getPluginManager().registerEvents(new MovementVelocityCheck(), this);
    }

    // My hope is to have everything async by using packets!
    public void registerPackets() {
        new GenericMovementCheck(this, manager);
    }

    public void registerChecks() {
        //GenericMovementCheck.registerCheck(new MovementVelocityCheck());
        //GenericMovementCheck.registerCheck(new Timer());
    }
}