package com.larisa.ltbbs.client;

import com.larisa.ltbbs.LTBBS;
import com.larisa.ltbbs.LittleTilesUtil;
import io.netty.buffer.Unpooled;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.dashboard.UIDashboard;
import mchorse.bbs_mod.ui.dashboard.panels.UIDashboardPanel;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.input.list.UIStringList;
import mchorse.bbs_mod.ui.framework.elements.utils.UILabel;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;

import java.util.ArrayList;
import java.util.List;

public class LittleTilesDashboardPanel extends UIDashboardPanel {
    private final StructureCatalog catalog = new StructureCatalog();
    private final UIStringList entries;
    private final UILabel status;

    public LittleTilesDashboardPanel(UIDashboard dashboard) {
        super(dashboard);

        UILabel title = new UILabel(IKey.constant("LittleTiles structure catalog"));
        title.xy(20, 20).wh(360, 20);

        this.status = new UILabel(IKey.constant("Hold a LittleTiles structure, then create a catalog entry."));
        this.status.xy(20, 45).wh(600, 20);

        UIButton create = new UIButton(IKey.constant("Create from held item"), button -> this.createFromHeldItem());
        create.xy(20, 75).wh(180, 20);

        UIButton give = new UIButton(IKey.constant("Give selected structure"), button -> this.giveSelected());
        give.xy(210, 75).wh(180, 20);

        UIButton delete = new UIButton(IKey.constant("Delete selected"), button -> this.deleteSelected());
        delete.xy(400, 75).wh(140, 20);

        this.entries = new UIStringList(selected -> this.updateStatus());
        this.entries.xy(20, 110).wh(420, 260);
        this.entries.background();
        this.entries.emptyState(IKey.constant("No LittleTiles structures in this catalog."));

        this.add(title, this.status, create, give, delete, this.entries);
        this.catalog.load();
        this.refreshEntries(-1);
    }

    @Override
    public boolean needsBackground() {
        return true;
    }

    private void createFromHeldItem() {
        MinecraftClient client = MinecraftClient.getInstance();
        ItemStack held = client.player == null ? ItemStack.EMPTY : client.player.getMainHandStack();

        if (!LittleTilesUtil.isLittleTilesBuild(held)) {
            this.status.label = IKey.constant("Hold a LittleTiles structure in your main hand first.");
            return;
        }

        StructureCatalog.Entry entry = this.catalog.add(held.copy());
        if (entry == null) {
            this.status.label = IKey.constant("The structure could not be saved.");
            return;
        }

        this.refreshEntries(this.catalog.entries().size() - 1);
        this.status.label = IKey.constant("Created " + entry.name + ". It is ready for BBS Block Forms and re-issuing.");
    }

    private void giveSelected() {
        int index = this.entries.getIndex();
        ItemStack stack = this.catalog.getStack(index);

        if (stack.isEmpty()) {
            this.status.label = IKey.constant("Select a structure from the catalog first.");
            return;
        }

        PacketByteBuf buffer = new PacketByteBuf(Unpooled.buffer());
        buffer.writeItemStack(stack);
        ClientPlayNetworking.send(LTBBS.GIVE_STRUCTURE, buffer);
        this.status.label = IKey.constant("Requested a new copy of the selected structure.");
    }

    private void deleteSelected() {
        int index = this.entries.getIndex();
        if (index < 0) {
            this.status.label = IKey.constant("Select a structure from the catalog first.");
            return;
        }

        this.catalog.remove(index);
        this.refreshEntries(Math.min(index, this.catalog.entries().size() - 1));
        this.status.label = IKey.constant("Removed the selected structure from the catalog.");
    }

    private void refreshEntries(int selectedIndex) {
        List<String> names = new ArrayList<>();
        for (StructureCatalog.Entry entry : this.catalog.entries()) {
            names.add(entry.name);
        }

        this.entries.setList(names);
        if (selectedIndex >= 0) {
            this.entries.setIndex(selectedIndex);
        }
    }

    private void updateStatus() {
        if (this.entries.getIndex() >= 0) {
            this.status.label = IKey.constant("Selected catalog structure. You can create a BBS Block Form from its LittleTiles item.");
        }
    }
}
