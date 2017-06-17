package com.guichaguri.pvptime.sponge;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.world.World;

/**
 * @author Guilherme Chaguri
 */
public class PvPTimeCommand implements CommandExecutor {
    private final PvPTimeSponge plugin;

    public PvPTimeCommand(PvPTimeSponge plugin) {
        this.plugin = plugin;
    }

    @Override
    public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
        if(!src.hasPermission("pvptime.reload")) {
            info(src, true);
        } else {
            help(src);
        }
        return CommandResult.success();
    }

    public CommandResult info(CommandSource src, CommandContext args) {
        info(src, false);
        return CommandResult.success();
    }

    public CommandResult reload(CommandSource src, CommandContext args) {
        plugin.reloadConfig();
        plugin.loadConfig();
        src.sendMessage(Text.builder("The configuration file was reloaded").color(TextColors.GREEN).build());
        return CommandResult.success();
    }

    private void info(CommandSource src, boolean onlyCurrent) {
        src.sendMessage(Text.builder("------------ PvPTime ------------").color(TextColors.GREEN).build());

        if(onlyCurrent && src instanceof Player) {
            infoWorld(src, ((Player)src).getWorld(), "Current World");
        } else {
            for(World w : Sponge.getServer().getWorlds()) {
                infoWorld(src, w, w.getName());
            }
        }

        src.sendMessage(Text.builder("--------------------------------").color(TextColors.GREEN).build());
    }

    private void infoWorld(CommandSource src, World world, String name) {
        EngineSponge engine = plugin.getEngine();
        Boolean isPvPTime = engine.isPvPTime(world.getName());

        Text pvp;
        if(isPvPTime == null) {
            pvp = Text.builder("Disabled").color(TextColors.RED).build();
        } else {
            pvp = Text.builder(isPvPTime ? "PvP On" : "PvP Off").color(TextColors.YELLOW).build();
        }

        src.sendMessage(Text.builder(name + " ").color(TextColors.GOLD).append(pvp).build());
    }

    private void help(CommandSource src) {
        src.sendMessage(Text.builder("------------ PvPTime ------------").color(TextColors.GREEN).build());

        Text infoDescription = Text.builder("Shows information about the worlds").color(TextColors.YELLOW).build();
        src.sendMessage(Text.builder("/pvptime info ").color(TextColors.GOLD).append(infoDescription).build());

        Text reloadDescription = Text.builder("Reloads the configuration file").color(TextColors.YELLOW).build();
        src.sendMessage(Text.builder("/pvptime reload ").color(TextColors.GOLD).append(reloadDescription).build());

        src.sendMessage(Text.builder("--------------------------------").color(TextColors.GREEN).build());
    }
}
