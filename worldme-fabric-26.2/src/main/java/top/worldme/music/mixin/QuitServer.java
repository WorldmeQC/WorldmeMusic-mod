package top.worldme.music.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.worldme.music.event.ClientEvent;

@Mixin(Minecraft.class)
public class QuitServer {
    @Inject(method = "disconnect(Lnet/minecraft/client/gui/screens/Screen;ZZ)V", at = @At("HEAD"))
    public void onDisconnected(Screen screen, boolean bl, boolean bl2, CallbackInfo info) {
        ClientEvent.onDisconnect();
    }
}