package me.guichaguri.pvptime;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
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
    public void processCommand(ICommandSender sender, String[] args) {

        if((args.length > 0) && (args[0].equalsIgnoreCase("reload"))) {
            PvPTime.INSTANCE.loadConfig();
            
            ChatComponentText txt = new ChatComponentText("Config Reloaded!");
            txt.setChatStyle(txt.getChatStyle().setColor(EnumChatFormatting.GREEN));
            sender.addChatMessage(txt);
            
            return;
        }

        ChatComponentText title = new ChatComponentText("---------- PvPTime Info ----------");
        title.getChatStyle().setColor(EnumChatFormatting.GREEN);

        sender.addChatMessage(title);
        for(int id : DimensionManager.getIDs()) {
            World w = DimensionManager.getWorld(id);
            String n = w.provider.getDimensionName() + " (" + id + ")";
            Boolean pvptime = PvPTimeRegistry.isPvPTime(id);
            String on = pvptime == null ? "§cDisabled" : (pvptime ? "PvP On" : "PvP Off");
            ChatComponentText txt = new ChatComponentText("");
            txt.getChatStyle().setColor(EnumChatFormatting.YELLOW);
            ChatComponentText name = new ChatComponentText(n);
            name.getChatStyle().setColor(EnumChatFormatting.GOLD);
            txt.appendSibling(name);
            txt.appendText(": " + on);
            sender.addChatMessage(txt);
        }

        ChatComponentText footer = new ChatComponentText("--------------------------------");
        footer.getChatStyle().setColor(EnumChatFormatting.GREEN);
        sender.addChatMessage(footer);
    }
}
