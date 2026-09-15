package com.larisa.ltbbs;

import com.larisa.ltbbs.client.LittleTilesDashboardPanel;
import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.api.Subscribe;
import mchorse.bbs_mod.api.client.events.RegisterDashboardPanelsEvent;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import net.fabricmc.api.ClientModInitializer;

public class LTBBSClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        BBSMod.events.register(this);
    }

    @Subscribe
    public void registerDashboardPanel(RegisterDashboardPanelsEvent event) {
        event.dashboard.getPanels().registerPanel(
            new LittleTilesDashboardPanel(event.dashboard),
            IKey.constant("LittleTiles"),
            Icons.BRICKS
        );
    }
}
