package me.guichaguri.pvptime;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;

public class CommandPvPTime extends CommandBase {
    @Override
    public String getCommandName() {
        return "pvptime";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/pvptime [reload]";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) {

        if((args.length > 0) && (args[0].equalsIgnoreCase("reload"))) {
            PvPTime.INSTANCE.loadConfig();

            TextComponentString txt = new TextComponentString("Config Reloaded!");
            txt.setStyle(txt.getStyle().setColor(TextFormatting.GREEN));
            sender.addChatMessage(txt);
            
            return;
        }

        TextComponentString title = new TextComponentString("---------- PvPTime Info ----------");
        title.getStyle().setColor(TextFormatting.GREEN);

        sender.addChatMessage(title);
        for(int id : DimensionManager.getIDs()) {
            World w = DimensionManager.getWorld(id);
            String n = w.provider.getDimensionType().getName() + " (" + id + ")";
            Boolean pvptime = PvPTimeRegistry.isPvPTime(id);
            String on = pvptime == null ? TextFormatting.RED + "Disabled" : (pvptime ? "PvP On" : "PvP Off");
            TextComponentString txt = new TextComponentString("");
            txt.getStyle().setColor(TextFormatting.YELLOW);
            TextComponentString name = new TextComponentString(n);
            name.getStyle().setColor(TextFormatting.GOLD);
            txt.appendSibling(name);
            txt.appendText(": " + on);
            sender.addChatMessage(txt);
        }

        TextComponentString footer = new TextComponentString("--------------------------------");
        footer.getStyle().setColor(TextFormatting.GREEN);
        sender.addChatMessage(footer);
    }
}
