package net.johnvictorfs.simple_utilities.mixin;

// Fabric-only, Yarn-mapped mixin target (InGameHud) - see GameInfoHud's class
// javadoc. Render-method parameter type is version-split: DrawContext
// (>=1.20) vs MatrixStack (1.18.2/1.19.4, pre-DrawContext).
//? if fabric {
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

    @Inject(method = "<init>(Lnet/minecraft/client/MinecraftClient;)V", at = @At(value = "RETURN"))
    private void onInit(MinecraftClient client, CallbackInfo ci) {
        this.hudInfo = new GameInfoHud(client);
    }

    //? if >=1.20 {
    @Inject(method = "render", at = @At("HEAD"))
    private void onDraw(DrawContext context, float tickDelta, CallbackInfo ci) {
        if (!this.client.inGameHud.getDebugHud().shouldShowDebugHud()) {
            this.hudInfo.draw(context);
        }
    }
    //?} else {
    /*@Inject(method = "render", at = @At("HEAD"))
    private void onDraw(MatrixStack matrices, float tickDelta, CallbackInfo ci) {
        if (!this.client.inGameHud.getDebugHud().shouldShowDebugHud()) {
            this.hudInfo.draw(matrices);
        }
    }
    *///?}
}
//?}
