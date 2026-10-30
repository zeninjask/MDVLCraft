package com.mdvlcraft.binder.mixin.epicfight;

import com.google.gson.JsonObject;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import yesman.epicfight.api.asset.JsonAssetLoader;
import yesman.epicfight.api.utils.ParseUtil;

/**
 * Epic Fight wrote every animation and mesh JSON it parsed back out to a string, only to hash it: about a third of the
 * time the client spends loading the pack's thousands of animations at start-up. The hash is now made the first time
 * getFileHash asks for it, from the same string, so its value is unchanged.
 */
@Mixin(value = JsonAssetLoader.class, remap = false)
public abstract class JsonAssetHashMixin {
    @Shadow
    private JsonObject rootJson;
    @Shadow
    private String filehash;

    @Redirect(method = "<init>(Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/resources/ResourceLocation;)V",
        at = @At(value = "INVOKE", target = "Lcom/google/gson/JsonObject;toString()Ljava/lang/String;"))
    private String mdvlcraft$skipText(JsonObject json) {
        return "";
    }

    @Redirect(method = "<init>(Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/resources/ResourceLocation;)V",
        at = @At(value = "INVOKE", target = "Lyesman/epicfight/api/utils/ParseUtil;getBytesSHA256Hash([B)Ljava/lang/String;"))
    private String mdvlcraft$skipHash(byte[] bytes) {
        return null;
    }

    @Inject(method = "getFileHash", at = @At("HEAD"))
    private void mdvlcraft$hashOnFirstUse(CallbackInfoReturnable<String> cir) {
        if (filehash == null && rootJson != null) {
            filehash = ParseUtil.getBytesSHA256Hash(rootJson.toString().getBytes());
        }
    }
}
