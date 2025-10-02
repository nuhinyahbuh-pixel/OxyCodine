package com.example.entityesp;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.client.registry.ClientRegistry;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiSlider;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;

import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;

import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import java.io.File;

@Mod(modid = "entityesp", name = "EntityESP", version = "3.0", clientSideOnly = true)
public class EntityESPMod {

    private static final Minecraft mc = Minecraft.getMinecraft();
    private KeyBinding toggleKey, guiKey;
    private boolean enabled = false;

    private static Configuration config;

    // Settings
    public static boolean highlightPlayers = true;
    public static boolean highlightHostiles = true;
    public static boolean highlightPassives = true;

    // Colors
    public static int[] playerColor = {255, 0, 0};
    public static int[] hostileColor = {0, 255, 0};
    public static int[] passiveColor = {0, 0, 255};

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        toggleKey = new KeyBinding("Toggle EntityESP", Keyboard.KEY_V, "key.categories.misc");
        guiKey = new KeyBinding("ESP Settings", Keyboard.KEY_B, "key.categories.misc");
        ClientRegistry.registerKeyBinding(toggleKey);
        ClientRegistry.registerKeyBinding(guiKey);

        MinecraftForge.EVENT_BUS.register(this);
        cpw.mods.fml.common.FMLCommonHandler.instance().bus().register(this);

        File cfgFile = new File(Minecraft.getMinecraft().mcDataDir, "config/entityesp.cfg");
        config = new Configuration(cfgFile);
        loadConfig();
    }

    private static void loadConfig() {
        highlightPlayers = config.get("toggles", "highlightPlayers", true).getBoolean();
        highlightHostiles = config.get("toggles", "highlightHostiles", true).getBoolean();
        highlightPassives = config.get("toggles", "highlightPassives", true).getBoolean();

        playerColor[0] = config.get("colors", "playerR", 255).getInt();
        playerColor[1] = config.get("colors", "playerG", 0).getInt();
        playerColor[2] = config.get("colors", "playerB", 0).getInt();

        hostileColor[0] = config.get("colors", "hostileR", 0).getInt();
        hostileColor[1] = config.get("colors", "hostileG", 255).getInt();
        hostileColor[2] = config.get("colors", "hostileB", 0).getInt();

        passiveColor[0] = config.get("colors", "passiveR", 0).getInt();
        passiveColor[1] = config.get("colors", "passiveG", 0).getInt();
        passiveColor[2] = config.get("colors", "passiveB", 255).getInt();

        if (config.hasChanged()) config.save();
    }

    public static void saveConfig() {
        config.get("toggles", "highlightPlayers", true).set(highlightPlayers);
        config.get("toggles", "highlightHostiles", true).set(highlightHostiles);
        config.get("toggles", "highlightPassives", true).set(highlightPassives);

        config.get("colors", "playerR", 255).set(playerColor[0]);
        config.get("colors", "playerG", 0).set(playerColor[1]);
        config.get("colors", "playerB", 0).set(playerColor[2]);

        config.get("colors", "hostileR", 0).set(hostileColor[0]);
        config.get("colors", "hostileG", 255).set(hostileColor[1]);
        config.get("colors", "hostileB", 0).set(hostileColor[2]);

        config.get("colors", "passiveR", 0).set(passiveColor[0]);
        config.get("colors", "passiveG", 0).set(passiveColor[1]);
        config.get("colors", "passiveB", 255).set(passiveColor[2]);

        config.save();
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (toggleKey.isPressed()) {
            enabled = !enabled;
            mc.thePlayer.addChatMessage(
                new net.minecraft.util.ChatComponentText("[EntityESP] " + (enabled ? "Enabled" : "Disabled"))
            );
        }
        if (guiKey.isPressed()) {
            mc.displayGuiScreen(new GuiESPSettings());
        }
    }

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        if (!enabled) return;
        if (mc.theWorld == null || mc.thePlayer == null) return;

        double renderPosX = RenderManager.renderPosX;
        double renderPosY = RenderManager.renderPosY;
        double renderPosZ = RenderManager.renderPosZ;

        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glPushMatrix();
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glLineWidth(2.0F);

        for (Object o : mc.theWorld.loadedEntityList) {
            if (!(o instanceof EntityLivingBase)) continue;
            EntityLivingBase entity = (EntityLivingBase) o;
            if (entity == mc.thePlayer) continue;

            boolean shouldDraw = false;
            int[] color = {0, 255, 0};

            if (entity instanceof EntityPlayer && highlightPlayers) {
                shouldDraw = true;
                color = playerColor;
            } else if (entity.isCreatureType(net.minecraft.entity.EnumCreatureType.MONSTER, false) && highlightHostiles) {
                shouldDraw = true;
                color = hostileColor;
            } else if (entity.isCreatureType(net.minecraft.entity.EnumCreatureType.CREATURE, false) && highlightPassives) {
                shouldDraw = true;
                color = passiveColor;
            }

            if (shouldDraw) {
                AxisAlignedBB bb = entity.getEntityBoundingBox()
                        .expand(0.1, 0.1, 0.1)
                        .offset(-renderPosX, -renderPosY, -renderPosZ);
                float r = color[0] / 255f;
                float g = color[1] / 255f;
                float b = color[2] / 255f;
                RenderGlobal.drawSelectionBoundingBox(bb, r, g, b, 0.6f);
            }
        }

        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glPopMatrix();
        GL11.glPopAttrib();
    }

    public static class GuiESPSettings extends GuiScreen {
        @Override
        public void initGui() {
            this.buttonList.clear();
            this.buttonList.add(new GuiButton(0, this.width / 2 - 100, 20, 200, 20,
                "Players: " + (highlightPlayers ? "ON" : "OFF")));
            this.buttonList.add(new GuiButton(1, this.width / 2 - 100, 45, 200, 20,
                "Hostiles: " + (highlightHostiles ? "ON" : "OFF")));
            this.buttonList.add(new GuiButton(2, this.width / 2 - 100, 70, 200, 20,
                "Passives: " + (highlightPassives ? "ON" : "OFF")));

            this.buttonList.add(new GuiSlider(3, this.width / 2 - 100, 110, 200, 20, "Player R: ", "", 0, 255, playerColor[0], false, true));
            this.buttonList.add(new GuiSlider(4, this.width / 2 - 100, 135, 200, 20, "Player G: ", "", 0, 255, playerColor[1], false, true));
            this.buttonList.add(new GuiSlider(5, this.width / 2 - 100, 160, 200, 20, "Player B: ", "", 0, 255, playerColor[2], false, true));

            this.buttonList.add(new GuiSlider(6, this.width / 2 - 100, 200, 200, 20, "Hostile R: ", "", 0, 255, hostileColor[0], false, true));
            this.buttonList.add(new GuiSlider(7, this.width / 2 - 100, 225, 200, 20, "Hostile G: ", "", 0, 255, hostileColor[1], false, true));
            this.buttonList.add(new GuiSlider(8, this.width / 2 - 100, 250, 200, 20, "Hostile B: ", "", 0, 255, hostileColor[2], false, true));

            this.buttonList.add(new GuiSlider(9, this.width / 2 - 100, 290, 200, 20, "Passive R: ", "", 0, 255, passiveColor[0], false, true));
            this.buttonList.add(new GuiSlider(10, this.width / 2 - 100, 315, 200, 20, "Passive G: ", "", 0, 255, passiveColor[1], false, true));
            this.buttonList.add(new GuiSlider(11, this.width / 2 - 100, 340, 200, 20, "Passive B: ", "", 0, 255, passiveColor[2], false, true));

            this.buttonList.add(new GuiButton(12, this.width / 2 - 100, this.height - 40, 200, 20, "Save & Close"));
        }

        @Override
        protected void actionPerformed(GuiButton button) {
            switch (button.id) {
                case 0: highlightPlayers = !highlightPlayers; button.displayString = "Players: " + (highlightPlayers ? "ON" : "OFF"); break;
                case 1: highlightHostiles = !highlightHostiles; button.displayString = "Hostiles: " + (highlightHostiles ? "ON" : "OFF"); break;
                case 2: highlightPassives = !highlightPassives; button.displayString = "Passives: " + (highlightPassives ? "ON" : "OFF"); break;
                case 12: saveConfig(); mc.displayGuiScreen(null); break;
            }
        }

        @Override
        public void drawScreen(int mouseX, int mouseY, float partialTicks) {
            super.drawScreen(mouseX, mouseY, partialTicks);
            for (Object obj : this.buttonList) {
                if (obj instanceof GuiSlider) {
                    GuiSlider slider = (GuiSlider) obj;
                    int value = (int) slider.getValue();
                    switch (slider.id) {
                        case 3: playerColor[0] = value; break;
                        case 4: playerColor[1] = value; break;
                        case 5: playerColor[2] = value; break;
                        case 6: hostileColor[0] = value; break;
                        case 7: hostileColor[1] = value; break;
                        case 8: hostileColor[2] = value; break;
                        case 9: passiveColor[0] = value; break;
                        case 10: passiveColor[1] = value; break;
                        case 11: passiveColor[2] = value; break;
                    }
                }
            }
            drawRect(this.width / 2 - 100, 185, this.width / 2 + 100, 195,
                (0xFF << 24) | (playerColor[0] << 16) | (playerColor[1] << 8) | playerColor[2]);
            drawRect(this.width / 2 - 100, 275, this.width / 2 + 100, 285,
                (0xFF << 24) | (hostileColor[0] << 16) | (hostileColor[1] << 8) | hostileColor[2]);
            drawRect(this.width / 2 - 100, 365, this.width / 2 + 100, 375,
                (0xFF << 24) | (passiveColor[0] << 16) | (passiveColor[1] << 8) | passiveColor[2]);
        }

        @Override
        public boolean doesGuiPauseGame() { return false; }
    }
}
