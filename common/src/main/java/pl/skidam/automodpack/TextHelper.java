package pl.skidam.automodpack;

import net.minecraft.text.LiteralText;
import net.minecraft.text.MutableText;
import net.minecraft.text.TranslatableText;

/**
 * Utility class for ease of porting to different Minecraft versions.
 */
public final class TextHelper {

    public static MutableText translatable(String key, Object... args) {
        return new TranslatableText(key, args);
    }

    public static MutableText literal(String string) {
        return new LiteralText(string);
    }
}