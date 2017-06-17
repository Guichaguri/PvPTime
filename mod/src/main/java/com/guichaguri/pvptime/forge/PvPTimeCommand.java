package com.guichaguri.pvptime.forge;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;

/**
 * @author Guilherme Chaguri
 */
public class PvPTimeCommand extends CommandBase {

    private final PvPTimeForge mod;

    public PvPTimeCommand(PvPTimeForge mod) {
        this.mod = mod;
    }

    @Override
    public String getName() {
        return "pvptime";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/pvptime [info/reload]";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if(args.length > 0 && args[0].equalsIgnoreCase("info")) {
            info(sender, false);
        } else if(args.length > 0 && args[0].equals("reload")) {
            reload(sender);
        } else if(!sender.canUseCommand(2, "pvptime")) {
            info(sender, true);
        } else {
            help(sender);
        }
    }

    private void info(ICommandSender sender, boolean onlyCurrent) {
        sender.sendMessage(create("------------ PvPTime ------------", TextFormatting.GREEN));

        if(onlyCurrent) {
            infoWorld(sender, sender.getEntityWorld(), "Current World");
        } else {
            for(World w : DimensionManager.getWorlds()) {
                infoWorld(sender, w, w.provider.getDimensionType().getName() + " (" + w.provider.getDimension() + ")");
            }
        }

        sender.sendMessage(create("--------------------------------", TextFormatting.GREEN));
    }

    private void infoWorld(ICommandSender sender, World world, String name) {
        EngineForge engine = mod.getEngine();
        Boolean isPvPTime = engine.isPvPTime(world.provider.getDimension());

        ITextComponent pvp;
        if(isPvPTime == null) {
            pvp = create("Disabled", TextFormatting.RED);
        } else {
            pvp = create(isPvPTime ? "PvP On" : "PvP Off", TextFormatting.YELLOW);
        }

        sender.sendMessage(create(name + " ", TextFormatting.GOLD).appendSibling(pvp));
    }

    private void reload(ICommandSender sender) {
        if(!sender.canUseCommand(2, "pvptime")) {
            sender.sendMessage(create("You don't have permission to run this command", TextFormatting.RED));
            return;
        }
        mod.reloadConfig();
        mod.loadConfig();
        sender.sendMessage(create("The configuration file was reloaded", TextFormatting.GREEN));
    }

    private void help(ICommandSender sender) {
        sender.sendMessage(create("------------ PvPTime ------------", TextFormatting.GREEN));

        ITextComponent infoDescription = create("Shows information about the worlds", TextFormatting.YELLOW);
        sender.sendMessage(create("/pvptime info ", TextFormatting.GOLD).appendSibling(infoDescription));

        ITextComponent reloadDescription = create("Reloads the configuration file", TextFormatting.YELLOW);
        sender.sendMessage(create("/pvptime reload ", TextFormatting.GOLD).appendSibling(reloadDescription));

        sender.sendMessage(create("--------------------------------", TextFormatting.GREEN));
    }

    private ITextComponent create(String txt, TextFormatting color) {
        ITextComponent c = new TextComponentString(txt);
        c.getStyle().setColor(color);
        return c;
    }
}
