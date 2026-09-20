package top.worldme.music.mixin;

import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.sounds.SoundSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import top.worldme.music.WorldmeMusic;
import top.worldme.music.WorldmeMusicPlayer;

@Mixin(SoundEngine.class)
public class SoundEvent {
    @Inject(method = "play*", at = @At("HEAD"), cancellable = true)
    public void play(SoundInstance soundInstance, CallbackInfoReturnable<SoundEngine.PlayResult> info) {
        if (WorldmeMusic.getPlayer().getState() != WorldmeMusicPlayer.STATE_PLAYING || soundInstance == null) {
            return;
        }
        SoundSource data = soundInstance.getSource();
        if (data == SoundSource.RECORDS || data == SoundSource.MUSIC) {
            info.setReturnValue(SoundEngine.PlayResult.NOT_STARTED);
        }
    }
}