package me.guichaguri.pvptime;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;

public class CommandPvPTime extends CommandBase {
    @Override
    public String getCommandName() {
        return "pvptime";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/pvptime";
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        sender.addChatMessage(new ChatComponentText("§a---------- PvPTime Info ----------"));
        for(int id : DimensionManager.getIDs()) {
            World w = DimensionManager.getWorld(id);
            String n = w.provider.getDimensionName() + " (" + id + ")";
            Boolean pvptime = PvPTimeRegistry.isPvPTime(id);
            String on = pvptime == null ? "§cDisabled" : ((pvptime ? "PvP On" : "PvP Off") + " (" + (w.getWorldTime() % 24000) + " - " + w.getTotalWorldTime() + ")");
            sender.addChatMessage(new ChatComponentText("§6" + n + "§e: " + on));
        }
        sender.addChatMessage(new ChatComponentText("§a--------------------------------"));
    }
}
