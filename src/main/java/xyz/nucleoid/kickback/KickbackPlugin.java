package xyz.nucleoid.kickback;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.KickedFromServerEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;

import java.util.concurrent.TimeUnit;

@Plugin(
        id = "kickback", name = "Kickback", version = "0.1.0",
        description = "Kick players back to another server when it comes online",
        authors = { "Gegy" },
        url = "https://nucleoid.xyz"
)
public final class KickbackPlugin {
    private final ProxyServer proxy;

    @Inject
    public KickbackPlugin(ProxyServer proxy) {
        this.proxy = proxy;
    }

    @Subscribe
    public void onKickedFromServer(KickedFromServerEvent event) {
        Player player = event.getPlayer();
        RegisteredServer fromServer = event.getServer();
        KickedFromServerEvent.ServerKickResult result = event.getResult();

        if (!event.kickedDuringServerConnect() && result instanceof KickedFromServerEvent.RedirectPlayer) {
            RegisteredServer redirectServer = ((KickedFromServerEvent.RedirectPlayer) result).getServer();
            this.scheduleReconnection(player, redirectServer, fromServer);
        }
    }

    private void scheduleReconnection(Player player, RegisteredServer fallbackServer, RegisteredServer returnServer) {
        this.proxy.getScheduler()
                .buildTask(this, () -> {
                    if (returnServer.getPlayersConnected().contains(player) || !fallbackServer.getPlayersConnected().contains(player)) {
                        return;
                    }

                    player.createConnectionRequest(returnServer).connect().handle((result, throwable) -> {
                        if (result == null || !result.isSuccessful()) {
                            this.scheduleReconnection(player, fallbackServer, returnServer);
                        }
                        return null;
                    });
                })
                .delay(10, TimeUnit.SECONDS)
                .schedule();
    }
}
