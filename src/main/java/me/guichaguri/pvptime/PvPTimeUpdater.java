package me.guichaguri.pvptime;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.CommandEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.HashMap;
import java.util.List;

public class PvPTimeUpdater {
    private static long ticksLeft = 100;

    private static PvPTimeTickUpdater tick = null;
    private static PvPTimeEventListener event = null;
    public static void register() {
        unregister();
        tick = new PvPTimeTickUpdater();
        event = new PvPTimeEventListener();
        FMLCommonHandler.instance().bus().register(tick);
        MinecraftForge.EVENT_BUS.register(event);
        update();
        HashMap<Integer, Boolean> hs = PvPTimeRegistry.getPvPTimeMap();
        for(int id : DimensionManager.getIDs()) {
            hs.put(id, PvPTimeRegistry.isRawPvPTime(id));
        }
    }
    public static void unregister() {
        if(tick != null) FMLCommonHandler.instance().bus().unregister(tick);
        if(event != null) MinecraftForge.EVENT_BUS.unregister(event);
        tick = null;
        event = null;
    }

    public static class PvPTimeTickUpdater {
        @SubscribeEvent
        public void ServerTickEvent(TickEvent.ServerTickEvent event) {
            ticksLeft--;
            if (ticksLeft < 0) {
                update();
            }
        }
    }

    public static class PvPTimeEventListener {
        @SubscribeEvent
        public void CommandEvent(CommandEvent event) {
            ticksLeft = 5;
        }

        @SubscribeEvent
        public void AttackEntityEvent(AttackEntityEvent event) {
            if (!(event.target instanceof EntityPlayer)) return;
            World w = event.entityPlayer.getEntityWorld();
            if (!PvPTimeRegistry.isPvPTime(w.provider.getDimensionId()))
                event.setCanceled(true);
        }
    }

    private static void update() {
        long i = 10000;
        HashMap<Integer, WorldOptions> o = PvPTimeRegistry.getWorldOptionMap();
        HashMap<Integer, Boolean> pt = PvPTimeRegistry.getPvPTimeMap();
        for(int id : DimensionManager.getIDs()) {
            if(!o.containsKey(id)) continue;
            WorldOptions options = o.get(id);
            if(!options.isEnabled()) continue;
            World w = DimensionManager.getWorld(id);
            long time = PvPTimeRegistry.getDayTime(w);
            long start = options.getPvPTimeStart();
            long end = options.getPvPTimeEnd();

            Boolean was = pt.containsKey(id) ? pt.get(id) : null;
            Boolean is = PvPTimeRegistry.isRawPvPTime(id);
            if(was != null) {
                if(is != was) {
                    announce(w, is ? options.getStartMessage() : options.getEndMessage());
                    pt.put(id, is);
                }
            } else {
                pt.put(id, is);
            }

            /* Calc ticks left to check time again */

            long timeLeft = 10000;

            if(is) {
                if (time > end) {
                    timeLeft = w.getTotalWorldTime() - time + end;
                } else {
                    timeLeft = end - time;
                }
            } else {
                if(time > start) {
                    timeLeft = w.getTotalWorldTime() - time + start;
                } else {
                    timeLeft = start - time;
                }
            }

            if(timeLeft < i) i = timeLeft;
        }
        ticksLeft = i;
    }

    private static void announce(World w, String msg) {
        if((msg == null) || (msg.isEmpty())) return;
        for(EntityPlayer p : (List<EntityPlayer>)w.playerEntities) {
            p.addChatMessage(new ChatComponentText(msg.replaceAll("&([0-9a-fk-or])", "\u00a7$1")));
        }
    }
}
