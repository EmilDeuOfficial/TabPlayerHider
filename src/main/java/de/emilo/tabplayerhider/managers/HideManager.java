package de.emilo.tabplayerhider.managers;

import de.emilo.tabplayerhider.TabPlayerHider;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket.Action;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket.Entry;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class HideManager {

    // ClientboundPlayerInfoUpdatePacket(EnumSet<Action>, List<Entry>)  — private
    @SuppressWarnings("unchecked")
    private static final Constructor<ClientboundPlayerInfoUpdatePacket> PACKET_CTOR;

    // Entry(UUID, boolean)  — package-private; boolean = listed flag
    @SuppressWarnings("unchecked")
    private static final Constructor<Entry> ENTRY_CTOR;

    static {
        Constructor<ClientboundPlayerInfoUpdatePacket> pktCtor = null;
        Constructor<Entry> entryCtor = null;
        try {
            pktCtor = (Constructor<ClientboundPlayerInfoUpdatePacket>)
                ClientboundPlayerInfoUpdatePacket.class
                    .getDeclaredConstructor(EnumSet.class, List.class);
            pktCtor.setAccessible(true);
        } catch (Exception ignored) {}
        try {
            entryCtor = (Constructor<Entry>)
                Entry.class.getDeclaredConstructor(UUID.class, boolean.class);
            entryCtor.setAccessible(true);
        } catch (Exception ignored) {}
        PACKET_CTOR = pktCtor;
        ENTRY_CTOR  = entryCtor;
    }

    private final TabPlayerHider plugin;
    private final List<String> hiddenNames;

    // Read from Netty I/O thread — must be thread-safe
    private final Set<UUID> hiddenUUIDs = ConcurrentHashMap.newKeySet();

    private boolean enabled;

    public HideManager(TabPlayerHider plugin) {
        this.plugin = plugin;
        this.hiddenNames = new ArrayList<>(plugin.getConfig().getStringList("hidden-players"));
        this.enabled = plugin.getConfig().getBoolean("enabled", true);
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (isHidden(p)) hiddenUUIDs.add(p.getUniqueId());
        }
    }

    // --- UUID tracking (main thread only) ---

    public void trackPlayer(Player player) {
        if (isHidden(player)) hiddenUUIDs.add(player.getUniqueId());
    }

    public void untrackPlayer(Player player) {
        hiddenUUIDs.remove(player.getUniqueId());
    }

    /** Called from the Netty I/O thread — must not use Bukkit API. */
    public boolean isHiddenUUID(UUID uuid) {
        return enabled && hiddenUUIDs.contains(uuid);
    }

    // --- State ---

    public boolean isEnabled() { return enabled; }

    public boolean isHidden(Player player) {
        return hiddenNames.contains(player.getName());
    }

    // --- Toggle ---

    public void enable() {
        enabled = true;
        plugin.getConfig().set("enabled", true);
        plugin.saveConfig();
        for (Player viewer : Bukkit.getOnlinePlayers()) applyHidingTo(viewer);
    }

    public void disable() {
        enabled = false;
        plugin.getConfig().set("enabled", false);
        plugin.saveConfig();
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (isHidden(online)) showInTabList(online);
        }
    }

    // --- List management ---

    public boolean addHidden(String name) {
        if (hiddenNames.contains(name)) return false;
        hiddenNames.add(name);
        saveNamesToConfig();
        Player target = Bukkit.getPlayerExact(name);
        if (target != null) {
            hiddenUUIDs.add(target.getUniqueId());
            if (enabled) hideFromAll(target);
        }
        return true;
    }

    public boolean removeHidden(String name) {
        if (!hiddenNames.remove(name)) return false;
        saveNamesToConfig();
        Player target = Bukkit.getPlayerExact(name);
        if (target != null) {
            hiddenUUIDs.remove(target.getUniqueId());
            showInTabList(target);
        }
        return true;
    }

    public void reload() {
        hiddenNames.clear();
        hiddenNames.addAll(plugin.getConfig().getStringList("hidden-players"));
        enabled = plugin.getConfig().getBoolean("enabled", true);
        hiddenUUIDs.clear();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (isHidden(p)) hiddenUUIDs.add(p.getUniqueId());
        }
        for (Player viewer : Bukkit.getOnlinePlayers()) applyHidingTo(viewer);
    }

    // --- Packet helpers ---

    /** Hides all currently hidden online players from the given viewer's tab list. */
    public void applyHidingTo(Player viewer) {
        if (!enabled) return;
        List<UUID> toHide = new ArrayList<>();
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (isHidden(online) && !online.equals(viewer)) toHide.add(online.getUniqueId());
        }
        if (!toHide.isEmpty()) sendUnlisted(viewer, toHide);
    }

    /** Hides the target from every other online player's tab list. */
    public void hideFromAll(Player target) {
        List<UUID> uuids = List.of(target.getUniqueId());
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            if (!viewer.equals(target)) sendUnlisted(viewer, uuids);
        }
    }

    /** Re-lists the target in every other online player's tab list. */
    public void showInTabList(Player target) {
        var packet = makeListed(target.getUniqueId(), true);
        if (packet == null) return;
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            if (!viewer.equals(target)) {
                ((CraftPlayer) viewer).getHandle().connection.send(packet);
            }
        }
    }

    /** Sends UPDATE_LISTED=false to the viewer for the given UUIDs.
     *  Does NOT remove them from the player registry — skin/nametag remain intact. */
    public void sendUnlisted(Player viewer, List<UUID> uuids) {
        var packet = makeUnlisted(uuids);
        if (packet != null) {
            ((CraftPlayer) viewer).getHandle().connection.send(packet);
        }
    }

    /** Builds an UPDATE_LISTED=false packet for the given UUIDs via reflection. */
    public ClientboundPlayerInfoUpdatePacket makeUnlisted(List<UUID> uuids) {
        if (PACKET_CTOR == null || ENTRY_CTOR == null) return null;
        try {
            List<Entry> entries = new ArrayList<>();
            for (UUID id : uuids) entries.add(ENTRY_CTOR.newInstance(id, false));
            return PACKET_CTOR.newInstance(EnumSet.of(Action.UPDATE_LISTED), entries);
        } catch (Exception e) { return null; }
    }

    private ClientboundPlayerInfoUpdatePacket makeListed(UUID uuid, boolean listed) {
        if (PACKET_CTOR == null || ENTRY_CTOR == null) return null;
        try {
            return PACKET_CTOR.newInstance(
                EnumSet.of(Action.UPDATE_LISTED),
                List.of(ENTRY_CTOR.newInstance(uuid, listed))
            );
        } catch (Exception e) { return null; }
    }

    public List<String> getHiddenNames() {
        return new ArrayList<>(hiddenNames);
    }

    private void saveNamesToConfig() {
        plugin.getConfig().set("hidden-players", hiddenNames);
        plugin.saveConfig();
    }
}
