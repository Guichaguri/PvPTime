package com.guichaguri.pvptime.sponge;

import com.guichaguri.pvptime.api.IPvPTimeAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandCause;
import org.spongepowered.api.command.CommandExecutor;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.exception.CommandException;
import org.spongepowered.api.command.parameter.CommandContext;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.world.server.ServerWorld;

/**
 * @author Guilherme Chaguri
 */
public class PvPTimeCommand implements CommandExecutor {
    private final PvPTimeSponge plugin;

    public PvPTimeCommand(PvPTimeSponge plugin) {
        this.plugin = plugin;
    }

    @Override
    public CommandResult execute(CommandContext context) throws CommandException {
        CommandCause src = context.cause();
        if(!src.hasPermission("pvptime.reload")) {
            info(src, true);
        } else {
            help(src);
        }
        return CommandResult.success();
    }

    public CommandResult info(CommandContext context) {
        info(context.cause(), false);
        return CommandResult.success();
    }

    public CommandResult reload(CommandContext context) {
        plugin.reloadConfig();
        plugin.loadConfig();
        context.cause().sendMessage(Component.text("The configuration file was reloaded", NamedTextColor.GREEN));
        return CommandResult.success();
    }

    private void info(CommandCause src, boolean onlyCurrent) {
        src.sendMessage(Component.text("------------ PvPTime ------------", NamedTextColor.GREEN));

        if(onlyCurrent && src.root() instanceof ServerPlayer) {
            infoWorld(src, ((ServerPlayer)src.root()).world(), "Current World");
        } else {
            for(ServerWorld w : Sponge.server().worldManager().worlds()) {
                infoWorld(src, w, w.key().asString());
            }
        }

        src.sendMessage(Component.text("--------------------------------", NamedTextColor.GREEN));
    }

    private void infoWorld(CommandCause src, ServerWorld world, String name) {
        IPvPTimeAPI<ResourceKey> engine = plugin.getAPI();
        Boolean isPvPTime = engine.isPvPTime(world.key());

        Component pvp;
        if(isPvPTime == null) {
            pvp = Component.text("Disabled", NamedTextColor.RED);
        } else {
            pvp = Component.text(isPvPTime ? "PvP On" : "PvP Off", NamedTextColor.YELLOW);
        }

        src.sendMessage(Component.text(name + " ", NamedTextColor.GOLD).append(pvp));
    }

    private void help(CommandCause src) {
        src.sendMessage(Component.text("------------ PvPTime ------------", NamedTextColor.GREEN));

        Component infoDescription = Component.text("Shows information about the worlds", NamedTextColor.YELLOW);
        src.sendMessage(Component.text("/pvptime info ", NamedTextColor.GOLD).append(infoDescription));

        Component reloadDescription = Component.text("Reloads the configuration file", NamedTextColor.YELLOW);
        src.sendMessage(Component.text("/pvptime reload ", NamedTextColor.GOLD).append(reloadDescription));

        src.sendMessage(Component.text("--------------------------------", NamedTextColor.GREEN));
    }
}
