package io.github.cjustinn.instancedworlds.Parties;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

public class PartyStatsListener implements Listener {

    private Party group;

    public PartyStatsListener(Party party) {
        this.group = party;
    }

    @EventHandler
    public void onPlayerDamaged(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        if (group.playerIsInParty(((Player) event.getEntity()).getUniqueId())) {
            int health = (int) (((Player) event.getEntity()).getHealth() - event.getDamage()) < 0 ? 0 : (int) Math.ceil(((Player) event.getEntity()).getHealth() - event.getDamage());
            group.updatePlayerScore((Player) event.getEntity(), health);
        }
    }

    @EventHandler
    public void onPlayerHealed(EntityRegainHealthEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        if (group.playerIsInParty(((Player) event.getEntity()).getUniqueId())) {
            int health = (int) (((Player) event.getEntity()).getHealth() + event.getAmount()) > 20 ? 20 : (int) Math.ceil(((Player) event.getEntity()).getHealth() + event.getAmount());
            group.updatePlayerScore((Player) event.getEntity(), health);
        }
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        if (group.playerIsInParty(event.getPlayer().getUniqueId())) {
            group.updatePlayerScore(event.getPlayer(), 20);
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (group.playerIsInParty(event.getPlayer().getUniqueId())) {
            group.updatePlayerScore(event.getPlayer(), 0);
        }
    }

    public void shutdown() {
        HandlerList.unregisterAll(this);
    }
}
