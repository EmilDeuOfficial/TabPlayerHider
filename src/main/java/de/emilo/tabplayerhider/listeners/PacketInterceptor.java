package de.emilo.tabplayerhider.listeners;

import de.emilo.tabplayerhider.managers.HideManager;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

public class PacketInterceptor extends ChannelDuplexHandler {

    private static final String KEY = "tabplayerhider";

    private final HideManager hideManager;

    private PacketInterceptor(HideManager hideManager) {
        this.hideManager = hideManager;
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        super.write(ctx, msg, promise);

        if (!(msg instanceof ClientboundPlayerInfoUpdatePacket packet)) return;

        // Collect hidden players that appear in this packet
        List<UUID> hidden = packet.entries().stream()
            .map(ClientboundPlayerInfoUpdatePacket.Entry::profileId)
            .filter(hideManager::isHiddenUUID)
            .toList();

        if (hidden.isEmpty()) return;

        // Let the original packet go through (so skin data / player registry stays intact),
        // then immediately follow with UPDATE_LISTED=false so they don't show in the tab list.
        var unlistedPacket = hideManager.makeUnlisted(hidden);
        if (unlistedPacket != null) {
            ctx.writeAndFlush(unlistedPacket);
        }
    }

    public static void install(Player player, HideManager hideManager) {
        try {
            var channel = ((CraftPlayer) player).getHandle().connection.connection.channel;
            if (channel.pipeline().get(KEY) != null) return;
            channel.pipeline().addBefore("packet_handler", KEY, new PacketInterceptor(hideManager));
        } catch (Exception ignored) {}
    }

    public static void uninstall(Player player) {
        try {
            var channel = ((CraftPlayer) player).getHandle().connection.connection.channel;
            if (channel.pipeline().get(KEY) != null) channel.pipeline().remove(KEY);
        } catch (Exception ignored) {}
    }
}
