package net.Slainlight.NoHunger.mixin;

import net.Slainlight.NoHunger.NoHungerConfigHandler;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import  net.minecraft.client.gui.DrawContext;
import static net.Slainlight.NoHunger.Main.raised;

@Mixin(InGameHud.class)
public abstract class MixinInGameHud
{
    @Redirect(method = "renderStatusBars", at = @At(value = "INVOKE",target = "Lnet/minecraft/client/gui/hud/InGameHud;getHeartCount(Lnet/minecraft/entity/LivingEntity;)I"))
    private int onGetMountHealth(InGameHud hud, LivingEntity entity)
    {
        // This tricks the code into thinking that there will be a mount
        // health bar to be rendered instead of the hunger bar.
        return -1;
    }

    int flip(int min, int max, int given)
    {
        int sudoResult = given-min;
        int size = max-min;

        sudoResult = size - sudoResult;

        return sudoResult + min;
    }
    @Shadow
    protected abstract int getHeartCount(LivingEntity entity);
    @Shadow protected abstract LivingEntity getRiddenEntity();
    @Shadow protected abstract PlayerEntity getCameraPlayer();

    @Shadow @Final private static Identifier ARMOR_HALF_TEXTURE;
    @Shadow private int scaledWidth;
    private static final int offset = 202;

    @Unique
    private static final Identifier ARMOR_HALF_BACKWARDS_TEXTURE = new Identifier("nohunger", "hud/armor_half");

    @Redirect(method = "renderStatusBars", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;drawGuiTexture(Lnet/minecraft/util/Identifier;IIII)V"))
    private void onDrawTexture(DrawContext context, Identifier identifier, int x, int y, int width, int height)
    {
        if (identifier == ARMOR_HALF_TEXTURE)
            identifier = ARMOR_HALF_BACKWARDS_TEXTURE;

        int v_offset = getCameraPlayer().isInSneakingPose() ? 0 : 7;
        if (NoHungerConfigHandler.shouldShowExp())
            v_offset = 0;
        if (NoHungerConfigHandler.shouldHideExp())
            v_offset = 7;

        // Moves the armor bar down to where the hunger bar was.
        if (y == 191 - raised)
        {
            if (!(getHeartCount(getRiddenEntity()) > 0))
            {
                // Draw Armor (flipped)
                context.drawGuiTexture(identifier, (-1 * x) + scaledWidth - 10, 201 - raised + v_offset, width, height);
                return;
            }
        }

        if (y == 201 - raised)
        {
            if (getCameraPlayer().getArmor() > 0)
            {
                // Draw air bubbles (flipped)
                context.drawGuiTexture(identifier, (-1 * x) + scaledWidth - 10, 191 - raised + v_offset, width, height);
                return;
            }
        }

        context.drawGuiTexture(identifier, x, y + v_offset, width, height);
    }

    //@Redirect(method = "drawHeart", at = @At(value = "INVOKE",target = "Lnet/minecraft/client/gui/hud/InGameHud;drawTexture(Lnet/minecraft/client/util/math/MatrixStack;IIIIII)V"))
    @Redirect(method = "drawHeart", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;drawGuiTexture(Lnet/minecraft/util/Identifier;IIII)V"))
    private void onDrawHeartTexture(DrawContext context, Identifier identifier, int x, int y, int width, int height)
    {
        if((getCameraPlayer().isInSneakingPose() || NoHungerConfigHandler.shouldShowExp()) && !NoHungerConfigHandler.shouldHideExp())
            context.drawGuiTexture(identifier, x, y, width, height);
        else
            context.drawGuiTexture(identifier, x, y+7, width, height);
    }

    @Inject(method = "renderExperienceBar", at = @At("HEAD"), cancellable = true)
    public void renderExperienceBar(DrawContext context, int x, CallbackInfo ci)
    {
        if(NoHungerConfigHandler.shouldHideExp())
            ci.cancel();

        if (!NoHungerConfigHandler.shouldShowExp())
            if(!getCameraPlayer().isInSneakingPose())
                ci.cancel();
    }
}