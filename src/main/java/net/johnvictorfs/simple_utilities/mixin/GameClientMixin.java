package net.johnvictorfs.simple_utilities.mixin;

// Fabric-only, Yarn-mapped accessor - see GameInfoHud's class javadoc for why
// this whole file doesn't exist on Forge/NeoForge builds this pass (and, for
// the same Yarn-mappings-availability reason, not on Fabric 26.1+ either).
//? if fabric && <26.1 {
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MinecraftClient.class)
public interface GameClientMixin {
    @Accessor("currentFps")
    int getCurrentFps();
}
//?}
