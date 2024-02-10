package tech.williamist.pronounmc.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.util.Colors;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tech.williamist.pronounmc.PronounCache;

import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Mixin(PlayerListHud.class)
public abstract class PlayerListHudMixin {

    @Shadow @Final private MinecraftClient client;

    @Shadow protected abstract List<PlayerListEntry> collectPlayerEntries();

    @Inject(method = "renderLatencyIcon", at = @At("TAIL"))
    public void afterRenderPlayer(DrawContext context, int width, int x, int y, PlayerListEntry entry, CallbackInfo ci) {
        String txt = PronounCache.getPronounsFor(entry.getProfile().getId());
        if (txt == null) return;

        // positition text correctly
        int textX = x + (width - 12 - client.textRenderer.getWidth(txt));

        context.drawText(client.textRenderer, txt, textX, y, Colors.GRAY, true);
    }

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Ljava/lang/Math;min(II)I"))
    public int redirectPlayerListEntryWidth(int a, int b) {
        List<PlayerListEntry> entries = collectPlayerEntries();
        UUID[] uuids = entries
                .stream()
                .map(ent -> ent.getProfile().getId())
                .toArray(UUID[]::new);

        HashMap<UUID, String> pronouns = PronounCache.getPronounsFor(uuids);

        int longestPronoun = pronouns.values().stream().filter(Objects::nonNull).mapToInt(client.textRenderer::getWidth).max().orElse(0);

        return Math.min(a + 5 + longestPronoun, b);
    }

}
