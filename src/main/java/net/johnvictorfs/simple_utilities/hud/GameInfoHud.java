package net.johnvictorfs.simple_utilities.hud;

// This whole file is Fabric-only (and only below 26.1) for now: it draws
// directly onto the vanilla HUD using Yarn-mapped rendering APIs
// (DrawContext/MatrixStack, TextRenderer, ItemRenderer) that Forge/NeoForge
// would need re-derived against Mojang mappings, and that don't exist at all
// on Fabric 26.1+ (Yarn mappings stopped being published there). Forge/
// NeoForge cells, and the 26.2-fabric cell, only load config this pass - see
// PLAN.md and CLAUDE.md "Why mixins/HUD rendering make Forge/NeoForge harder
// than the template", the same split EasierVillagerTrading documents for its
// own mixin-based feature. PLAN.md "26.2" documents the follow-up plan to
// port this feature using FlightHud's HudHooks/PlatformCanvas pattern
// (loader-official HUD hooks instead of a GuiRenderState mixin).
//? if fabric && <26.1 {
import com.google.common.collect.Lists;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import net.johnvictorfs.simple_utilities.config.SimpleUtilitiesConfig;
import net.johnvictorfs.simple_utilities.helpers.Colors;
import net.johnvictorfs.simple_utilities.mixin.GameClientMixin;
import net.johnvictorfs.simple_utilities.util.TextCompat;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
//? if >=1.19 {
import net.minecraft.registry.RegistryKey;
//?} else {
/*import net.minecraft.util.registry.RegistryKey;
*///?}
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.biome.Biome;
//? if >=1.20 {
import net.minecraft.client.gui.DrawContext;
//?} else {
/*import net.minecraft.client.util.math.MatrixStack;
*///?}

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Draws the HUD overlay: coordinates/direction, fps, biome, game time,
 * equipment + durability, sprint indicator. Direct port of upstream's
 * {@code GameInfoHud} (johnvictorfs/simple-utilities-mod 2.6.2), with Cloth
 * Config swapped for the vendored {@link SimpleUtilitiesConfig} and
 * rendering split by MC version - {@code DrawContext} (>=1.20) vs
 * {@code MatrixStack} (1.18.2/1.19.4, pre-DrawContext).
 *
 * <p>1.18.2/1.19.4 equipment info is text-only (item name + durability/count,
 * no item-slot icon) - the old {@code ItemRenderer} icon-drawing signature
 * hasn't been re-verified against those two versions' Yarn mappings; tracked
 * as open work in PLAN.md rather than guessed at.
 */
@Environment(EnvType.CLIENT)
public class GameInfoHud {
    private final MinecraftClient client;
    private final TextRenderer fontRenderer;
    private ClientPlayerEntity player;
    private final SimpleUtilitiesConfig config;

    public GameInfoHud(MinecraftClient client) {
        this.client = client;
        this.fontRenderer = client.textRenderer;
        this.config = SimpleUtilitiesConfig.getInstance(FabricLoader.getInstance().getConfigDir().toFile());
    }

    //? if >=1.20 {
    public void draw(DrawContext context) {
        if (!config.toggleSimpleUtilitiesHUD) return;
        this.player = this.client.player;
        if (this.player == null) return;

        RenderSystem.enableBlend();

        if (config.toggleEquipmentStatus) {
            drawEquipmentInfo(context);
        }

        int lineHeight = this.fontRenderer.fontHeight + 2;
        int top = 0;
        int left = 4;

        for (String line : getGameInfo()) {
            context.drawTextWithShadow(this.fontRenderer, line, left, top + 4, config.textColor);
            top += lineHeight;
        }

        if (config.toggleSprintStatus && (this.client.options.sprintKey.isPressed() || this.player.isSprinting())) {
            drawSprintingInfo(context);
        }
    }

    private void drawSprintingInfo(DrawContext context) {
        final String sprintingText = TextCompat.translatableString("text.hud.simple_utilities.sprinting");

        int maxLineHeight = Math.max(10, this.fontRenderer.getWidth(sprintingText));
        maxLineHeight = (int) (Math.ceil(maxLineHeight / 5.0D + 0.5D) * 5);
        int scaleHeight = this.client.getWindow().getScaledHeight();
        int sprintingTop = scaleHeight - maxLineHeight;

        context.drawTextWithShadow(this.fontRenderer, sprintingText, 2, sprintingTop + 20, config.textColor);
    }

    private void drawEquipmentInfo(DrawContext context) {
        List<ItemStack> equippedItems = collectEquippedItems();
        int maxLineHeight = maxEquipmentLineHeight(equippedItems);
        int itemTop = this.client.getWindow().getScaledHeight() - maxLineHeight;
        int lineHeight = this.fontRenderer.fontHeight + 6;

        for (ItemStack equippedItem : Lists.reverse(equippedItems)) {
            if (equippedItem.getItem().equals(Blocks.AIR.asItem())) continue;

            context.drawItem(equippedItem, 2, itemTop - 68);
            drawEquipmentLine(equippedItem, itemTop, (text, x, y, color) ->
                    context.drawTextWithShadow(this.fontRenderer, text, x, y, color));

            itemTop += lineHeight;
        }
    }
    //?} else {
    /*public void draw(MatrixStack matrices) {
        if (!config.toggleSimpleUtilitiesHUD) return;
        this.player = this.client.player;
        if (this.player == null) return;

        RenderSystem.enableBlend();

        if (config.toggleEquipmentStatus) {
            drawEquipmentInfo(matrices);
        }

        int lineHeight = this.fontRenderer.fontHeight + 2;
        int top = 0;
        int left = 4;

        for (String line : getGameInfo()) {
            this.fontRenderer.drawWithShadow(matrices, line, left, top + 4, config.textColor);
            top += lineHeight;
        }

        if (config.toggleSprintStatus && (this.client.options.sprintKey.isPressed() || this.player.isSprinting())) {
            drawSprintingInfo(matrices);
        }
    }

    private void drawSprintingInfo(MatrixStack matrices) {
        final String sprintingText = TextCompat.translatableString("text.hud.simple_utilities.sprinting");

        int maxLineHeight = Math.max(10, this.fontRenderer.getWidth(sprintingText));
        maxLineHeight = (int) (Math.ceil(maxLineHeight / 5.0D + 0.5D) * 5);
        int scaleHeight = this.client.getWindow().getScaledHeight();
        int sprintingTop = scaleHeight - maxLineHeight;

        this.fontRenderer.drawWithShadow(matrices, sprintingText, 2, sprintingTop + 20, config.textColor);
    }

    // Text-only equipment info on 1.18.2/1.19.4 - no item-slot icon, see class
    // javadoc. Still shows name + durability/count, just not the icon.
    private void drawEquipmentInfo(MatrixStack matrices) {
        List<ItemStack> equippedItems = collectEquippedItems();
        int lineHeight = this.fontRenderer.fontHeight + 6;
        int itemTop = this.client.getWindow().getScaledHeight() - maxEquipmentLineHeight(equippedItems);

        for (ItemStack equippedItem : Lists.reverse(equippedItems)) {
            if (equippedItem.getItem().equals(Blocks.AIR.asItem())) continue;

            String name = I18n.translate(equippedItem.getItem().getTranslationKey());
            this.fontRenderer.drawWithShadow(matrices, name, 2, itemTop - 68, config.textColor);
            drawEquipmentLine(equippedItem, itemTop, (text, x, y, color) ->
                    this.fontRenderer.drawWithShadow(matrices, text, x, y, color));

            itemTop += lineHeight;
        }
    }
    *///?}

    @FunctionalInterface
    private interface LineDrawer {
        void draw(String text, int x, int y, int color);
    }

    private void drawEquipmentLine(ItemStack equippedItem, int itemTop, LineDrawer drawer) {
        if (equippedItem.getMaxDamage() != 0) {
            int currentDurability = equippedItem.getMaxDamage() - equippedItem.getDamage();
            String itemDurability = currentDurability + "/" + equippedItem.getMaxDamage();

            int color = config.textColor;
            if (currentDurability < equippedItem.getMaxDamage()) {
                color = Colors.lightGreen;
            }
            if (currentDurability <= (equippedItem.getMaxDamage() / 1.5)) {
                color = Colors.lightYellow;
            }
            if (currentDurability <= (equippedItem.getMaxDamage() / 2.5)) {
                color = Colors.lightOrange;
            }
            if (currentDurability <= (equippedItem.getMaxDamage()) / 4) {
                color = Colors.lightRed;
            }

            drawer.draw(itemDurability, 22, itemTop - 64, color);
        } else {
            PlayerInventory inventory = this.player.getInventory();
            int inventoryCount = inventory.count(equippedItem.getItem());
            int count = equippedItem.getCount();

            if (inventoryCount > 1) {
                String itemCount = count + " (" + inventoryCount + ")";
                drawer.draw(itemCount, 22, itemTop - 64, config.textColor);
            }
        }
    }

    private List<ItemStack> collectEquippedItems() {
        List<ItemStack> equippedItems = new ArrayList<>();
        PlayerInventory inventory = this.player.getInventory();

        equippedItems.add(inventory.getMainHandStack());
        for (ItemStack secondHandItem : inventory.offHand) {
            equippedItems.add(secondHandItem);
        }
        for (ItemStack armourItem : inventory.armor) {
            equippedItems.add(armourItem);
        }
        return equippedItems;
    }

    private int maxEquipmentLineHeight(List<ItemStack> equippedItems) {
        int maxLineHeight = Math.max(10, this.fontRenderer.getWidth(""));
        for (ItemStack item : equippedItems) {
            maxLineHeight = Math.max(maxLineHeight, this.fontRenderer.getWidth(I18n.translate(item.getItem().getTranslationKey())));
        }
        return (int) (Math.ceil(maxLineHeight / 5.0D + 0.5D) * 5);
    }

    private static String capitalize(String str) {
        if (str == null) return null;
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    private String getOffset(Direction facing) {
        String offset = "";

        if (facing.getOffsetX() > 0) {
            offset += "+X";
        } else if (facing.getOffsetX() < 0) {
            offset += "-X";
        }

        if (facing.getOffsetZ() > 0) {
            offset += " +Z";
        } else if (facing.getOffsetZ() < 0) {
            offset += " -Z";
        }

        return offset.trim();
    }

    private static String zeroPadding(int number) {
        return (number >= 10) ? Integer.toString(number) : String.format("0%s", number);
    }

    private static String secondsToString(int pTime) {
        final int min = pTime / 60;
        final int sec = pTime - (min * 60);
        return String.format("%s:%s", zeroPadding(min), zeroPadding(sec));
    }

    private static String parseTime(long time) {
        long hours = (time / 1000 + 6) % 24;
        long minutes = (time % 1000) * 60 / 1000;
        String ampm = "AM";

        if (hours >= 12) {
            hours -= 12;
            ampm = "PM";
        }

        if (hours == 0) hours = 12;

        String mm = "0" + minutes;
        mm = mm.substring(mm.length() - 2);

        return hours + ":" + mm + " " + ampm;
    }

    private List<String> getGameInfo() {
        List<String> gameInfo = new ArrayList<>();

        if (config.toggleCoordinatesStatus || config.toggleDirectionStatus) {
            String coordDirectionStatus = "";
            Direction facing = this.player.getHorizontalFacing();
            String translatedDirection = TextCompat.translatableString("text.direction.simple_utilities." + facing.asString());
            String direction = translatedDirection + " " + getOffset(facing);

            if (config.toggleCoordinatesStatus) {
                coordDirectionStatus += String.format("%d, %d, %d", (int) this.player.getX(), (int) this.player.getY(), (int) this.player.getZ());
                if (config.toggleDirectionStatus) {
                    coordDirectionStatus += " (" + direction + ")";
                }
            } else {
                coordDirectionStatus += direction;
            }

            gameInfo.add(coordDirectionStatus);
        }

        if (config.toggleNetherCoordinateConversion) {
            String coordsFormat = "X: %.0f, Z: %.0f";
            String dimension = this.player.getWorld().getRegistryKey().getValue().toString();
            if (dimension.equals("minecraft:overworld")) {
                gameInfo.add("Nether: " + String.format(coordsFormat, this.player.getX() / 8, this.player.getZ() / 8));
            } else if (dimension.equals("minecraft:the_nether")) {
                gameInfo.add("Overworld: " + String.format(coordsFormat, this.player.getX() * 8, this.player.getZ() * 8));
            }
        }

        if (config.toggleFpsStatus) {
            gameInfo.add(String.format("%d fps", ((GameClientMixin) MinecraftClient.getInstance()).getCurrentFps()));
        }

        if (config.togglePlayerSpeedStatus) {
            Vec3d playerPosVec = Objects.requireNonNull(client.player).getPos();
            double travelledX = playerPosVec.x - client.player.prevX;
            double travelledZ = playerPosVec.z - client.player.prevZ;
            double currentSpeed = MathHelper.sqrt((float) (travelledX * travelledX + travelledZ * travelledZ));
            gameInfo.add(String.format("%.2f m/s", currentSpeed / 0.05F));
        }

        if (client.world != null) {
            if (config.toggleLightLevelStatus) {
                int lightLevel = client.world.getLightLevel(Objects.requireNonNull(client.player).getBlockPos());
                gameInfo.add("Light Level: " + lightLevel);
            }

            if (config.toggleBiomeStatus) {
                Optional<RegistryKey<Biome>> biome = this.client.world.getBiome(player.getBlockPos()).getKey();
                if (biome.isPresent()) {
                    String biomeName = TextCompat.translatableString("biome." + biome.get().getValue().getNamespace() + "." + biome.get().getValue().getPath());
                    gameInfo.add(TextCompat.translatableString("text.hud.simple_utilities.biome", capitalize(biomeName)));
                }
            }

            if (config.toggleGameTimeStatus) {
                gameInfo.add(parseTime(client.world.getTimeOfDay()));
            }
        }

        if (config.togglePlayerName) {
            gameInfo.add(player.getName().getString());
        }

        if (config.toggleServerName) {
            String serverName = "Singleplayer";
            try {
                serverName = client.getCurrentServerEntry().name;
            } catch (Exception ignored) {
                // no server entry (singleplayer) - keep default
            }
            gameInfo.add(serverName);
        }

        if (config.toggleServerAddress) {
            String serverIp = "N/A";
            try {
                serverIp = client.getCurrentServerEntry().address;
            } catch (Exception ignored) {
                // no server entry (singleplayer) - keep default
            }
            gameInfo.add(serverIp);
        }

        return gameInfo;
    }
}
//?}
