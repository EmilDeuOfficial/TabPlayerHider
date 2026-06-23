package de.emilo.tabplayerhider;

import de.emilo.tabplayerhider.commands.TabHideCommand;
import de.emilo.tabplayerhider.listeners.PacketInterceptor;
import de.emilo.tabplayerhider.listeners.PlayerJoinListener;
import de.emilo.tabplayerhider.managers.HideManager;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class TabPlayerHider extends JavaPlugin {

    private HideManager hideManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        hideManager = new HideManager(this);

        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this, hideManager), this);

        var cmd = new TabHideCommand(this, hideManager);
        getCommand("tabplayerhide").setExecutor(cmd);
        getCommand("tabplayerhide").setTabCompleter(cmd);

        // Install interceptors for players already online (plugin reload / late load)
        for (Player online : getServer().getOnlinePlayers()) {
            hideManager.trackPlayer(online);
            PacketInterceptor.install(online, hideManager);
            hideManager.applyHidingTo(online);
            if (hideManager.isHidden(online)) hideManager.hideFromAll(online);
        }

        getLogger().info("TabPlayerHider enabled – " + hideManager.getHiddenNames().size() + " player(s) configured.");
    }

    @Override
    public void onDisable() {
        getLogger().info("TabPlayerHider disabled.");
    }

    public HideManager getHideManager() {
        return hideManager;
    }
}
