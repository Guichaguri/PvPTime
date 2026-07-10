package com.guichaguri.pvptime.neoforge;

import com.guichaguri.pvptime.api.IPvPTimeAPI;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.level.Level;

/**
 * @author Guilherme Chaguri
 */
public class PvPTimeCommand {

    private final PvPTimeNeoForge mod;

    public PvPTimeCommand(PvPTimeNeoForge mod) {
        this.mod = mod;
    }

    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        var infoCommand = Commands.literal("info")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .executes(c -> info(c, false));

        var reloadCommand = Commands.literal("reload")
                .requires(Commands.hasPermission(Commands.LEVEL_ADMINS))
                .executes(this::reload);

        var helpCommand = Commands.literal("help")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .executes(this::help);

        var baseCommand = Commands.literal("pvptime")
                .requires(Commands.hasPermission(Commands.LEVEL_ALL))
                .executes(this::base)
                .then(infoCommand)
                .then(reloadCommand)
                .then(helpCommand);

        dispatcher.register(baseCommand);
    }

    private int base(CommandContext<CommandSourceStack> context) {
        CommandSourceStack sender = context.getSource();

        if (sender.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)) {
            help(context);
        } else {
            info(context, true);
        }

        return 0;
    }

    private int info(CommandContext<CommandSourceStack> context, boolean onlyCurrent) {
        CommandSourceStack sender = context.getSource();

        sender.sendSuccess(() -> create("------------ PvPTime ------------", TextColor.GREEN), false);

        if(onlyCurrent) {
            infoWorld(sender, sender.getLevel(), Component.literal("Current World"));
        } else {
            for(ServerLevel w : sender.getServer().getAllLevels()) {
                infoWorld(sender, w, Component.translatable(w.getDescriptionKey()).append(" (" + w.dimension().identifier() + ") "));
            }
        }

        sender.sendSuccess(() -> create("--------------------------------", TextColor.GREEN), false);

        return 0;
    }

    private void infoWorld(CommandSourceStack sender, ServerLevel world, MutableComponent name) {
        IPvPTimeAPI<ResourceKey<Level>> engine = mod.getAPI();
        Boolean isPvPTime = engine.isPvPTime(world.dimension());

        Component pvp;
        if(isPvPTime == null) {
            pvp = create("Disabled", TextColor.RED);
        } else {
            pvp = create(isPvPTime ? "PvP On" : "PvP Off", TextColor.YELLOW);
        }

        sender.sendSuccess(() -> name.withColor(TextColor.GOLD).append(pvp), false);
    }

    private int reload(CommandContext<CommandSourceStack> context) {
        CommandSourceStack sender = context.getSource();

        mod.reloadConfig();
        mod.loadConfig(sender.getServer());

        sender.sendSuccess(() -> create("The configuration file was reloaded", TextColor.GREEN), false);

        return 1;
    }

    private int help(CommandContext<CommandSourceStack> context) {
        CommandSourceStack sender = context.getSource();

        sender.sendSuccess(() -> create("------------ PvPTime ------------", TextColor.GREEN), false);

        Component infoDescription = create("Shows information about the worlds", TextColor.YELLOW);
        sender.sendSuccess(() -> create("/pvptime info ", TextColor.GOLD).append(infoDescription), false);

        if (sender.permissions().hasPermission(Permissions.COMMANDS_ADMIN)) {
            Component reloadDescription = create("Reloads the configuration file", TextColor.YELLOW);
            sender.sendSuccess(() -> create("/pvptime reload ", TextColor.GOLD).append(reloadDescription), false);
        }

        sender.sendSuccess(() -> create("--------------------------------", TextColor.GREEN), false);

        return 0;
    }

    private MutableComponent create(String txt, TextColor color, Component ...siblings) {
        MutableComponent c = Component.literal(txt).withColor(color);
        for (Component s : siblings) {
            c.append(s);
        }
        return c;
    }
}
