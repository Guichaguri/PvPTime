package com.guichaguri.pvptime.fabric;

import com.guichaguri.pvptime.shared.PvPTimeMod;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLevelEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import org.jspecify.annotations.Nullable;

public class PvPTimeFabric extends PvPTimeMod implements ModInitializer {

    public PvPTimeFabric() {
        super(new PvPTimeFabricConfig());
    }

    @Override
    public void onInitialize() {
        ServerLifecycleEvents.SERVER_STARTING.register(this::handleServerStarting);
        ServerLifecycleEvents.SERVER_STARTED.register(this::handleServerStarted);
        ServerLifecycleEvents.SERVER_STOPPING.register(this::handleServerStopping);
        CommandRegistrationCallback.EVENT.register(this::handleRegisterCommands);
        ServerTickEvents.END_SERVER_TICK.register(this::handleServerTick);
        AttackEntityCallback.EVENT.register(this::onAttackEntity);
        ServerLivingEntityEvents.ALLOW_DAMAGE.register(this::onEntityHurt);
        ServerLevelEvents.LOAD.register(this::onWorldLoad);
        CommandPerformCallback.EVENT.register(this::handleCommandPerformed);
    }

    private InteractionResult onAttackEntity(Player attacker, Level level, InteractionHand hand, Entity victim, @Nullable EntityHitResult hitResult) {
        boolean isAttackAllowed = handleAllowEntityAttack(attacker, victim);

        return isAttackAllowed ? InteractionResult.PASS : InteractionResult.FAIL;
    }

    private boolean onEntityHurt(LivingEntity victim, DamageSource source, float amount) {
        Entity attacker = source.getEntity();
        if (attacker == null) return true;

        // Ignore if the attacker is not a player
        if (!(attacker instanceof Player playerAttacker)) return true;

        return handleAllowEntityAttack(playerAttacker, victim);
    }

    private void onWorldLoad(MinecraftServer server, ServerLevel level) {
        handleWorldLoad(level);
    }
}
