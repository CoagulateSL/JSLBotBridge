package net.coagulate.SL.JSLBotBridge;

import net.coagulate.JSLBot.JSLBot;
import net.coagulate.JSLBot.LLCATruster;
import net.coagulate.SL.Config;
import net.coagulate.SL.SLModule;

import javax.annotation.Nonnull;

public class JSLBotBridge extends SLModule {

    JSLBot bot=null;

    @Nonnull
    @Override
    public String getName() {
        return "JSLBot";
    }
    public String getDescription() { return "JSLBot bridge module for core SL services"; }

    public void shutdown() {
        if (bot != null) {
            bot.shutdown("SL System is shutting down");
        }
    }
    public void initialise() {
        LLCATruster.doNotUse(); // as in we use our own truster later on
        bot = new JSLBot(getBotConfig());
        bot.registershutdownhook = false;
        bot.ALWAYS_RECONNECT = true;
        bot.start();
    }

    @Override
    public void maintenance() {}

    public void startup() {
        waitBot();
    }
    private static BotConfig botconfig=null;
    @Nonnull
    public static BotConfig getBotConfig() {
        if (botconfig==null) {
            botconfig=new BotConfig();
        }
        botconfig.put("firstname",Config.getBotFirstName());
        botconfig.put("lastname",Config.getBotLastName());
        botconfig.put("password",Config.getBotPassword());
        botconfig.put("CnC.authoriser","OwnerOnly");
        botconfig.put("handlers",
                "CnC,Sink,Health,Regions,Teleportation,Agent,Objects,Groups,net.coagulate.SL.Bots.BotInterface,net.coagulate.SL.Services.RegionMonitor"+".SimStats"
        );
        botconfig.put("loginlocation","home");
        botconfig.put("CnC.authorisation.owneruuid",Config.getBotOwnerUUID());
        botconfig.put("CnC.authorisation.ownerusername",Config.getBotOwnerUsername());
        botconfig.put("CnC.publiccommandprefix","*");
        botconfig.put("CnC.privatecommandprefix","*");
        botconfig.put("CnC.homesickfor",Config.getBotHomeRegion());
        return botconfig;
    }

    private void waitBot() {
        try {
            bot.waitConnection(30000);
        } catch (@Nonnull final IllegalStateException e) {
        }
        if (!bot.connected()) {
            bot.shutdown("Failed to connect");
        }
    }

}
