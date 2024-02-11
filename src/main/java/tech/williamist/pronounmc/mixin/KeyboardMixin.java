package tech.williamist.pronounmc.mixin;

import net.minecraft.client.Keyboard;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import tech.williamist.pronounmc.PronounCache;

@Mixin(Keyboard.class)
public abstract class KeyboardMixin {

    @Shadow private long debugCrashStartTime;

    @Shadow protected abstract void debugLog(Text text);

    @Inject(method = "processF3", at = @At("HEAD"), cancellable = true)
    public void injectCustomF3Key(int key, CallbackInfoReturnable<Boolean> cir) {
        if (debugCrashStartTime > 0 && debugCrashStartTime < Util.getMeasuringTimeMs() - 100)
            return;

        if (key == 75) {
            cir.cancel();
            cir.setReturnValue(true);
            PronounCache.clear();
            debugLog(Text.literal("Cleared pronoun cache."));
        }
    }

}
