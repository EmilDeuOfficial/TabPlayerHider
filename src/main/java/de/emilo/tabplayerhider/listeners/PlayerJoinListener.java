package de.emilo.tabplayerhider.listeners;

import de.emilo.tabplayerhider.TabPlayerHider;
import de.emilo.tabplayerhider.managers.HideManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerJoinListener implements Listener {

    private final TabPlayerHider plugin;
    private final HideManager hideManager;

    public PlayerJoinListener(TabPlayerHider plugin, HideManager hideManager) {
        this.plugin = plugin;
        this.hideManager = hideManager;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player joined = event.getPlayer();
        hideManager.trackPlayer(joined);
        PacketInterceptor.install(joined, hideManager);

        // 1-tick delay: server sends existing PlayerInfo to the joining client before
        // this event fires, so we clean them up one tick later.
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            hideManager.applyHidingTo(joined);
            if (hideManager.isHidden(joined)) hideManager.hideFromAll(joined);
        }, 1L);
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        // The server re-sends player info over several ticks during dimension transitions.
        // The interceptor handles packets in-flight, but some versions send PlayerInfo via
        // a stored ChannelHandlerContext that skips our handler — the delayed task is the
        // reliable fallback.
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            hideManager.applyHidingTo(player);
            if (hideManager.isHidden(player)) hideManager.hideFromAll(player);
        }, 10L);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        hideManager.untrackPlayer(event.getPlayer());
    }
}
