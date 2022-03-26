package net.coagulate.SL.JSLBotBridge;

import net.coagulate.Core.BuildInfo.JSLBotBuildInfo;
import net.coagulate.Core.Database.DBConnection;
import net.coagulate.SL.HTML.ServiceTile;
import net.coagulate.SL.SLModule;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Date;
import java.util.Map;

public class JSLBot extends SLModule {
    public final String commitId() { return JSLBotBuildInfo.COMMITID; }
    public final Date getBuildDate() { return JSLBotBuildInfo.BUILDDATE; }

    net.coagulate.JSLBot.JSLBot bot=null;

    @Nullable
    @Override
    public Map<ServiceTile, Integer> getServices() {
        return null;
    }

    @Nonnull
    @Override
    public String getName() {
        return "JSLBot";
    }
    @Nonnull
    public String getDescription() { return "JSLBot imported sources"; }

    public void shutdown() {}
    public void initialise() {}
    @Override
    public void maintenance() {}
    @Override
    public void maintenanceInternal() {}

    public void startup() {}

    @Override
    public Object weakInvoke(String command, Object... arguments) { return null; }

    @Override
    protected int schemaUpgrade(DBConnection db, String schemaName, int currentVersion) {
        return currentVersion;
    }
}
