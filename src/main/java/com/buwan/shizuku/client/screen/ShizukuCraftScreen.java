package com.buwan.shizuku.client.screen;

import com.buwan.shizuku.shizuku;
import com.buwan.shizuku.menu.ShizukuCraftMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;

/**
 * 雫工作台的容器界面：绑定 ShizukuCraftMenu，绘制背景、槽位与标题；
 * containerTick 里刷新界面（与菜单的 slotsChanged 对应）。
 */
public class ShizukuCraftScreen extends AbstractContainerScreen<ShizukuCraftMenu> {
    private static final ResourceLocation BACKGROUND = shizuku.prefix("textures/gui/shizuku_crafting_table_gui.png");

    public ShizukuCraftScreen(ShizukuCraftMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 234;
        this.imageHeight = 278;
    }

    @Override
    protected void init() {
        super.init();
        FallingPetalRenderer.init(this.width, this.height);
    }

    @Override
    public void containerTick() {
        super.containerTick();
        FallingPetalRenderer.tick(this.width, this.height);
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        FallingPetalRenderer.render(graphics, this.width, this.height, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(BACKGROUND, this.leftPos, this.topPos, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 512, 512);
    }

    @Override
    protected void renderLabels(@NotNull GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(this.font, this.title, 8, 6, 4210752, false);
        graphics.drawString(this.font, this.playerInventoryTitle, 39, this.imageHeight - 94, 4210752, false);
    }
}
