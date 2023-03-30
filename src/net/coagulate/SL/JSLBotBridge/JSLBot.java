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
	net.coagulate.JSLBot.JSLBot bot;
	
	@Nullable
	@Override
	public Map<ServiceTile,Integer> getServices() {
		return null;
	}
	
	@Nonnull
	@Override
	public String getName() {
		return "JSLBot";
	}
	
	@Nonnull
	public String getDescription() {
		return "JSLBot imported sources";
	}
	
	public void shutdown() {
	}
	
	public void startup() {
	}
	
	public boolean initialise() {
		return true;
	}
	
	@Override
	public void maintenance() {
	}
	
	@Override
	public void maintenanceInternal() {
	}
	
	public final String commitId() {
		return JSLBotBuildInfo.COMMITID;
	}
	
	public final Date getBuildDate() {
		return JSLBotBuildInfo.BUILDDATE;
	}
	
	@Override
	protected int schemaUpgrade(final DBConnection db,final String schemaName,final int currentVersion) {
		return currentVersion;
	}
}
