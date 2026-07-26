package com.guichaguri.pvptime.forge;

import com.guichaguri.pvptime.api.IPvPTimeAPI;
import com.guichaguri.pvptime.shared.PvPTimeMod;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LevelAccessor;
import net.minecraftforge.event.CommandEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.listener.Priority;
import net.minecraftforge.fml.InterModComms;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.InterModProcessEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import java.util.function.Function;

/**
 * @author Guilherme Chaguri
 */
@Mod("pvptime")
public final class PvPTimeForge extends PvPTimeMod {

    public PvPTimeForge(FMLJavaModLoadingContext context) {
        super(new PvPTimeForgeConfig());

        FMLCommonSetupEvent.getBus(context.getModBusGroup()).addListener(this::onSetup);
        InterModProcessEvent.getBus(context.getModBusGroup()).addListener(this::imc);
    }

    private void onSetup(FMLCommonSetupEvent event) {
        ServerStartingEvent.BUS.addListener(this::onStart);
        ServerStartedEvent.BUS.addListener(this::onPostStart);
        ServerStoppingEvent.BUS.addListener(this::onStop);
        RegisterCommandsEvent.BUS.addListener(this::onRegisterCommands);
        TickEvent.ServerTickEvent.Post.BUS.addListener(this::onServerTick);
        CommandEvent.BUS.addListener(Priority.LOWEST, this::onCommand);
        AttackEntityEvent.BUS.addListener(Priority.LOWEST, this::onAttackEntity);
        LivingAttackEvent.BUS.addListener(Priority.LOWEST, this::onLivingAttack);
        LevelEvent.Load.BUS.addListener(this::onWorldLoad);
    }

    private void onStart(ServerStartingEvent event) {
        handleServerStarting(event.getServer());
    }

    private void onPostStart(ServerStartedEvent event) {
        handleServerStarted(event.getServer());
    }

    private void onStop(ServerStoppingEvent event) {
        handleServerStopping(event.getServer());
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        handleRegisterCommands(event.getDispatcher(), event.getBuildContext(), event.getCommandSelection());
    }

    private void onServerTick(TickEvent.ServerTickEvent.Post event) {
        handleServerTick(event.server());
    }

    private void imc(InterModProcessEvent event) {
        for(InterModComms.IMCMessage msg : event.getIMCStream().toList()) {
            if (!msg.modId().equals("pvptime")) continue;

            String key = msg.method().toLowerCase();
            if(!key.equals("pvptime") && !key.equals("api")) continue;

            Function<IPvPTimeAPI, Void> func = (Function<IPvPTimeAPI, Void>)msg.messageSupplier().get();

            func.apply(engine);
        }
    }

    private void onCommand(CommandEvent event) {
        handleCommandPerformed();
    }

    private boolean onAttackEntity(AttackEntityEvent event) {
        boolean isAttackAllowed = handleAllowEntityAttack(event.getEntity(), event.getTarget());

        if (!isAttackAllowed) {
            return true;
        }

        return false;
    }

    private boolean onLivingAttack(LivingAttackEvent event) {
        Entity attacker = event.getSource().getEntity();
        if (attacker == null) return false;

        // Ignore if the attacker is not a player
        if (!(attacker instanceof Player playerAttacker)) return false;

        boolean isAttackAllowed = handleAllowEntityAttack(playerAttacker, event.getEntity());

        if (!isAttackAllowed) {
            return true;
        }

        return false;
    }

    private void onWorldLoad(LevelEvent.Load event) {
        LevelAccessor levelAccessor = event.getLevel();

        if (!(levelAccessor instanceof ServerLevel level)) return;

        handleWorldLoad(level);
    }

}
