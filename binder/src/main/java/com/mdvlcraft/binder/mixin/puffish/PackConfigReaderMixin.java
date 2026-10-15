package com.mdvlcraft.binder.mixin.puffish;

import com.mdvlcraft.binder.skills.VocationParts;
import java.nio.file.Path;
import java.util.Optional;
import net.puffish.skillsmod.api.json.JsonElement;
import net.puffish.skillsmod.api.json.JsonPath;
import net.puffish.skillsmod.api.util.Problem;
import net.puffish.skillsmod.api.util.Result;
import net.puffish.skillsmod.config.reader.PackConfigReader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Vocation tabs (categories/voc_<a>_<b>/) have no files of their own: each file is assembled from the generated parts
 * when Pufferfish reads it (skills/VocationParts). A part that fails to read fails the tab like a missing file would.
 */
@Mixin(value = PackConfigReader.class, remap = false)
public abstract class PackConfigReaderMixin {
    @Inject(method = "read", at = @At("HEAD"), cancellable = true)
    private void mdvlcraft$vocation(Path path, CallbackInfoReturnable<Result<JsonElement, Problem>> cir) {
        Optional<String[]> match = VocationParts.match(path);
        if (match.isEmpty()) {
            return;
        }

        String tab = match.get()[0];
        String file = match.get()[1];
        PackConfigReader self = (PackConfigReader)(Object)this;
        Optional<com.google.gson.JsonElement> composed = VocationParts.compose(tab, file,
            part -> self.read(part).getSuccess().orElseThrow(() -> new IllegalStateException("Missing vocation part " + part)).getJson());
        cir.setReturnValue(composed
            .map(json -> JsonElement.parseString(json.toString(), JsonPath.create("mdvlcraft:" + path)))
            .orElseGet(() -> Result.failure(Problem.message("Vocation tab " + tab + " has no " + file))));
    }

    @Inject(method = "exists", at = @At("HEAD"), cancellable = true)
    private void mdvlcraft$vocationExists(Path path, CallbackInfoReturnable<Boolean> cir) {
        VocationParts.match(path).ifPresent(match -> cir.setReturnValue(!match[1].equals("exchange.json")));
    }
}
