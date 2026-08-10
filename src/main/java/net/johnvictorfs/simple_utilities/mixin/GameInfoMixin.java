package net.johnvictorfs.simple_utilities.mixin;

// Fabric-only, Yarn-mapped mixin target (InGameHud) - see GameInfoHud's class
// javadoc. Render-method parameter type is version-split: DrawContext
// (>=1.20) vs MatrixStack (1.18.2/1.19.4, pre-DrawContext). Excluded on
// Fabric 26.1+ too, since Yarn mappings (and InGameHud/DrawContext
// themselves) don't exist there - see PLAN.md "26.2".
//? if fabric && <26.1 {
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.johnvictorfs.simple_utilities.hud.GameInfoHud;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.InGameHud;
//? if >=1.20 {
import net.minecraft.client.gui.DrawContext;
//?} else {
/*import net.minecraft.client.util.math.MatrixStack;
*///?}
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(value = InGameHud.class)
public abstract class GameInfoMixin {
    private GameInfoHud hudInfo;

    @Shadow
    @Final
    private MinecraftClient client;

    // InGameHud's constructor is ALSO independently version-split, and NOT on
    // the same boundary as the render-param split above: it takes a second
    // ItemRenderer parameter on 1.19.4 and 1.20.1 ONLY (confirmed via javap on
    // all four real merged Yarn jars) - 1.18.2 and 1.21.4 both use the
    // single-arg constructor, so this is a "hump" in the middle of the
    // version range, not a simple >=/< split.
    //? if >=1.19 {
    //? if >=1.21 {
    @Inject(method = "<init>(Lnet/minecraft/client/MinecraftClient;)V", at = @At(value = "RETURN"))
    private void onInit(MinecraftClient client, CallbackInfo ci) {
        this.hudInfo = new GameInfoHud(client);
    }
    //?} else {
    /*@Inject(method = "<init>(Lnet/minecraft/client/MinecraftClient;Lnet/minecraft/client/render/item/ItemRenderer;)V", at = @At(value = "RETURN"))
    private void onInit(MinecraftClient client, net.minecraft.client.render.item.ItemRenderer itemRenderer, CallbackInfo ci) {
        this.hudInfo = new GameInfoHud(client);
    }
    *///?}
    //?} else {
    /*@Inject(method = "<init>(Lnet/minecraft/client/MinecraftClient;)V", at = @At(value = "RETURN"))
    private void onInit(MinecraftClient client, CallbackInfo ci) {
        this.hudInfo = new GameInfoHud(client);
    }
    *///?}

    // Debug-HUD-visible check is ALSO version-split, independently of the
    // render-param split above: InGameHud.getDebugHud().shouldShowDebugHud()
    // only exists from 1.21.4 onward (confirmed via javap on the real merged
    // Yarn jars). On 1.18.2/1.19.4/1.20.1, the equivalent flag is the plain
    // GameOptions.debugEnabled field (also confirmed via javap - no
    // per-DebugHud visibility method exists there at all).
    //? if >=1.20 {
    @Inject(method = "render", at = @At("HEAD"))
    private void onDraw(DrawContext context, float tickDelta, CallbackInfo ci) {
        //? if >=1.21 {
        if (!this.client.inGameHud.getDebugHud().shouldShowDebugHud()) {
        //?} else {
        /*if (!this.client.options.debugEnabled) {
        *///?}
            this.hudInfo.draw(context);
        }
    }
    //?} else {
    /*@Inject(method = "render", at = @At("HEAD"))
    private void onDraw(MatrixStack matrices, float tickDelta, CallbackInfo ci) {
        if (!this.client.options.debugEnabled) {
            this.hudInfo.draw(matrices);
        }
    }
    *///?}
}
//?}
