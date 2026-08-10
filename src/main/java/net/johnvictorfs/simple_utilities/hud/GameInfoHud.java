package net.johnvictorfs.simple_utilities.hud;

import com.google.common.collect.Lists;
import net.johnvictorfs.simple_utilities.config.SimpleUtilitiesConfig;
import net.johnvictorfs.simple_utilities.util.TextCompat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Draws the HUD overlay: coordinates/direction, fps, biome, game time,
 * equipment + durability, sprint indicator. Direct behavioral port of
 * upstream's {@code GameInfoHud} (johnvictorfs/simple-utilities-mod 2.6.2),
 * with Cloth Config swapped for the vendored {@link SimpleUtilitiesConfig}
 * and every Yarn-mapped API replaced by its Mojmap equivalent (confirmed via
 * {@code javap} against the real merged jars at both ends of the matrix -
 * 1.18.2 and 26.2 - not assumption). Draws through the loader/version-neutral
 * {@link SimpleCanvas} interface (see {@link HudHooks}/{@link PlatformCanvas}
 * for how each loader/version wires a canvas to its own native render
 * context) instead of a Fabric-only {@code DrawContext}/{@code MatrixStack} -
 * this is what makes the feature real (not a config-only stub) on every
 * loader in the matrix (Fabric, Forge, NeoForge), not just Fabric&lt;26.1.
 *
 * <p>Equipment info is icon+text on 1.20+ (via {@link SimpleCanvas#drawItem})
 * and text-only pre-1.20 (item name drawn where the icon would go instead -
 * {@code PlatformCanvas}'s pre-1.20 {@code drawItem} is a no-op), matching
 * upstream's original pre-{@code DrawContext} behavior exactly.
 */
public class GameInfoHud {
    private static final EquipmentSlot[] EQUIPMENT_SLOTS = {
            EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND,
            EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD
    };

    private final Minecraft client;
    private final Font font;
    private final SimpleUtilitiesConfig config;
    private LocalPlayer player;

    public GameInfoHud(Minecraft client) {
        this.client = client;
        this.font = client.font;
        // SimpleUtilities.java's entrypoint (every loader) already calls
        // SimpleUtilitiesConfig.getInstance(configDir) before HudHooks ever
        // fires - getInstance(...) ignores its argument once the singleton
        // exists, so this just fetches the already-loaded config.
        this.config = SimpleUtilitiesConfig.getInstance(null);
    }

    public void draw(SimpleCanvas canvas) {
        if (!config.toggleSimpleUtilitiesHUD) return;
        this.player = this.client.player;
        if (this.player == null) return;

        List<ItemStack> equippedItems = collectEquippedItems();

        if (config.toggleEquipmentStatus) {
            drawEquipmentInfo(canvas, equippedItems);
        }

        int lineHeight = this.font.lineHeight + 2;
        int top = 0;
        int left = 4;

        for (String line : getGameInfo()) {
            canvas.drawText(this.font, line, left, top + 4, config.textColor, true);
            top += lineHeight;
        }

        if (config.toggleSprintStatus && (this.client.options.keySprint.isDown() || this.player.isSprinting())) {
            drawSprintingInfo(canvas);
        }
    }

    private void drawSprintingInfo(SimpleCanvas canvas) {
        final String sprintingText = TextCompat.translatableString("text.hud.simple_utilities.sprinting");

        int maxLineHeight = Math.max(10, this.font.width(sprintingText));
        maxLineHeight = (int) (Math.ceil(maxLineHeight / 5.0D + 0.5D) * 5);
        int scaleHeight = this.client.getWindow().getGuiScaledHeight();
        int sprintingTop = scaleHeight - maxLineHeight;

        canvas.drawText(this.font, sprintingText, 2, sprintingTop + 20, config.textColor, true);
    }

    private void drawEquipmentInfo(SimpleCanvas canvas, List<ItemStack> equippedItems) {
        int maxLineHeight = maxEquipmentLineHeight(equippedItems);
        int itemTop = this.client.getWindow().getGuiScaledHeight() - maxLineHeight;
        int lineHeight = this.font.lineHeight + 6;

        for (ItemStack equippedItem : Lists.reverse(equippedItems)) {
            if (equippedItem.isEmpty()) continue;

            canvas.drawItem(equippedItem, 2, itemTop - 68);
            // Text-only equipment info pre-1.20 - no item-slot icon (see class
            // javadoc), so draw the item's name in text where the icon would
            // otherwise go. PlatformCanvas#drawItem above is a no-op here.
            //? if <1.20 {
            /*canvas.drawText(this.font, itemName(equippedItem), 2, itemTop - 68, config.textColor, true);
            *///?}
            drawEquipmentLine(canvas, equippedItem, itemTop, equippedItems);

            itemTop += lineHeight;
        }
    }

    private void drawEquipmentLine(SimpleCanvas canvas, ItemStack equippedItem, int itemTop, List<ItemStack> equippedItems) {
        if (equippedItem.getMaxDamage() != 0) {
            int currentDurability = equippedItem.getMaxDamage() - equippedItem.getDamageValue();
            String itemDurability = currentDurability + "/" + equippedItem.getMaxDamage();

            int color = GameInfoMath.durabilityColor(currentDurability, equippedItem.getMaxDamage(), config.textColor);

            canvas.drawText(this.font, itemDurability, 22, itemTop - 64, color, true);
        } else {
            int inventoryCount = countItemInInventory(equippedItem.getItem(), equippedItems);
            int count = equippedItem.getCount();

            if (inventoryCount > 1) {
                String itemCount = GameInfoMath.formatItemCount(count, inventoryCount);
                canvas.drawText(this.font, itemCount, 22, itemTop - 64, config.textColor, true);
            }
        }
    }

    private List<ItemStack> collectEquippedItems() {
        List<ItemStack> equippedItems = new ArrayList<>();
        for (EquipmentSlot slot : EQUIPMENT_SLOTS) {
            equippedItems.add(this.player.getItemBySlot(slot));
        }
        return equippedItems;
    }

    /**
     * Sums how many of {@code item} the player is carrying, across both the
     * non-equipment inventory slots and the currently-equipped items. Replaces
     * upstream's {@code PlayerInventory.count(Item)} (Yarn), which has no
     * direct Mojmap equivalent - {@code Inventory} exposes the backing item
     * list instead ({@code items} field through 1.21.4, renamed to the
     * accessor {@code getNonEquipmentItems()} at 26.1+ - confirmed via
     * {@code javap} against both jars), so the summing is done by hand here.
     */
    private int countItemInInventory(Item item, List<ItemStack> equippedItems) {
        int count = 0;
        for (ItemStack stack : nonEquipmentItems()) {
            if (stack.getItem() == item) {
                count += stack.getCount();
            }
        }
        for (ItemStack stack : equippedItems) {
            if (stack.getItem() == item) {
                count += stack.getCount();
            }
        }
        return count;
    }

    //? if >=26.1 {
    /*private List<ItemStack> nonEquipmentItems() {
        return this.player.getInventory().getNonEquipmentItems();
    }
    *///?} else {
    private List<ItemStack> nonEquipmentItems() {
        return this.player.getInventory().items;
    }
    //?}

    private static String itemName(ItemStack stack) {
        return TextCompat.translatableString(stack.getItem().getDescriptionId());
    }

    private int maxEquipmentLineHeight(List<ItemStack> equippedItems) {
        int maxLineHeight = Math.max(10, this.font.width(""));
        for (ItemStack item : equippedItems) {
            maxLineHeight = Math.max(maxLineHeight, this.font.width(itemName(item)));
        }
        return (int) (Math.ceil(maxLineHeight / 5.0D + 0.5D) * 5);
    }

    // 1.18.2 has no Minecraft.getFps() accessor (added 1.19.4 on, confirmed
    // via javap) - only the private static `fps` field. Reflection is the
    // only way to read it without a Mixin.
    //? if <1.19 {
    /*private static int getFpsReflectively() {
        try {
            java.lang.reflect.Field fpsField = Minecraft.class.getDeclaredField("fps");
            fpsField.setAccessible(true);
            return fpsField.getInt(null);
        } catch (ReflectiveOperationException e) {
            return 0;
        }
    }
    *///?}

    private List<String> getGameInfo() {
        List<String> gameInfo = new ArrayList<>();
        Level level = this.client.level;

        if (config.toggleCoordinatesStatus || config.toggleDirectionStatus) {
            String coordDirectionStatus = "";
            Direction facing = this.player.getDirection();
            String translatedDirection = TextCompat.translatableString("text.direction.simple_utilities." + facing.getSerializedName());
            String direction = translatedDirection + " " + GameInfoMath.getOffset(facing.getStepX(), facing.getStepZ());

            if (config.toggleCoordinatesStatus) {
                coordDirectionStatus += GameInfoMath.formatCoordinates((int) this.player.getX(), (int) this.player.getY(), (int) this.player.getZ());
                if (config.toggleDirectionStatus) {
                    coordDirectionStatus += " (" + direction + ")";
                }
            } else {
                coordDirectionStatus += direction;
            }

            gameInfo.add(coordDirectionStatus);
        }

        if (config.toggleNetherCoordinateConversion && level != null) {
            ResourceKey<Level> dimension = level.dimension();
            if (dimension.equals(Level.OVERWORLD)) {
                gameInfo.add("Nether: " + GameInfoMath.formatNetherFromOverworld(this.player.getX(), this.player.getZ()));
            } else if (dimension.equals(Level.NETHER)) {
                gameInfo.add("Overworld: " + GameInfoMath.formatOverworldFromNether(this.player.getX(), this.player.getZ()));
            }
        }

        if (config.toggleFpsStatus) {
            //? if >=1.19 {
            gameInfo.add(String.format("%d fps", this.client.getFps()));
            //?} else {
            /*gameInfo.add(String.format("%d fps", getFpsReflectively()));
            *///?}
        }

        if (config.togglePlayerSpeedStatus) {
            Vec3 pos = this.player.position();
            double travelledX = pos.x - this.player.xo;
            double travelledZ = pos.z - this.player.zo;
            float currentSpeed = Mth.sqrt((float) (travelledX * travelledX + travelledZ * travelledZ));
            gameInfo.add(GameInfoMath.formatSpeed(currentSpeed));
        }

        if (level != null) {
            if (config.toggleLightLevelStatus) {
                int lightLevel = level.getMaxLocalRawBrightness(this.player.blockPosition());
                gameInfo.add("Light Level: " + lightLevel);
            }

            if (config.toggleBiomeStatus) {
                Holder<Biome> biome = level.getBiome(this.player.blockPosition());
                Optional<ResourceKey<Biome>> biomeKey = biome.unwrapKey();
                if (biomeKey.isPresent()) {
                    String biomeName = TextCompat.translatableString(
                            "biome." + TextCompat.keyNamespace(biomeKey.get()) + "." + TextCompat.keyPath(biomeKey.get()));
                    gameInfo.add(TextCompat.translatableString("text.hud.simple_utilities.biome", GameInfoMath.capitalize(biomeName)));
                }
            }

            if (config.toggleGameTimeStatus) {
                //? if >=26.1 {
                /*gameInfo.add(GameInfoMath.parseTime(level.getOverworldClockTime()));
                *///?} else {
                gameInfo.add(GameInfoMath.parseTime(level.getDayTime()));
                //?}
            }
        }

        if (config.togglePlayerName) {
            gameInfo.add(this.player.getName().getString());
        }

        if (config.toggleServerName) {
            ServerData server = this.client.getCurrentServer();
            gameInfo.add(server != null ? server.name : "Singleplayer");
        }

        if (config.toggleServerAddress) {
            ServerData server = this.client.getCurrentServer();
            gameInfo.add(server != null ? server.ip : "N/A");
        }

        return gameInfo;
    }
}
