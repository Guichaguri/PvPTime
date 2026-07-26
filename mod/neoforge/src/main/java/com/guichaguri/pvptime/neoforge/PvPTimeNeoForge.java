package com.guichaguri.pvptime.neoforge;

import com.guichaguri.pvptime.api.IPvPTimeAPI;
import com.guichaguri.pvptime.shared.PvPTimeMod;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LevelAccessor;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.InterModComms;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.InterModProcessEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.CommandEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityInvulnerabilityCheckEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.function.Function;

/**
 * @author Guilherme Chaguri
 */
@Mod("pvptime")
public class PvPTimeNeoForge extends PvPTimeMod {

    public PvPTimeNeoForge(IEventBus eventBus) {
        super(new PvPTimeNeoForgeConfig());
        NeoForge.EVENT_BUS.register(this);
        eventBus.addListener(this::imc);
    }

    @SubscribeEvent
    public void onStart(ServerStartingEvent event) {
        handleServerStarting(event.getServer());
    }

    @SubscribeEvent
    public void onPostStart(ServerStartedEvent event) {
        handleServerStarted(event.getServer());
    }

    @SubscribeEvent
    public void onStop(ServerStoppingEvent event) {
        handleServerStopping(event.getServer());
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        handleRegisterCommands(event.getDispatcher(), event.getBuildContext(), event.getCommandSelection());
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        handleServerTick(event.getServer());
    }

    public void imc(InterModProcessEvent event) {
        for(InterModComms.IMCMessage msg : event.getIMCStream().toList()) {
            if (!msg.modId().equals("pvptime")) continue;

            String key = msg.method().toLowerCase();
            if(!key.equals("pvptime") && !key.equals("api")) continue;

            Function<IPvPTimeAPI, Void> func = (Function<IPvPTimeAPI, Void>)msg.messageSupplier().get();

            func.apply(engine);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onCommand(CommandEvent event) {
        handleCommandPerformed();
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onAttackEntity(AttackEntityEvent event) {
        boolean isAttackAllowed = handleAllowEntityAttack(event.getEntity(), event.getTarget());

        if (!isAttackAllowed) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onLivingAttack(EntityInvulnerabilityCheckEvent event) {
        if (event.isInvulnerable()) return;

        Entity attacker = event.getSource().getEntity();
        if (attacker == null) return;

        // Ignore if the attacker is not a player
        if (!(attacker instanceof Player playerAttacker)) return;

        boolean isAttackAllowed = handleAllowEntityAttack(playerAttacker, event.getEntity());

        if (!isAttackAllowed) {
            event.setInvulnerable(true);
        }
    }

    @SubscribeEvent
    public void onWorldLoad(LevelEvent.Load event) {
        LevelAccessor levelAccessor = event.getLevel();

        if (!(levelAccessor instanceof ServerLevel level)) return;

        handleWorldLoad(level);
    }

}
