package dev.phantasm.util;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;

/*
 Shared registry for pua codepoint tracking (NX3,NX4, IO4)
 Extracted from MixinTextRenderer to avoid non-private static methods in Mixins
 */
public final class PuaCodepointRegistry {

    public static final int PUA_START = 0xE000;
    public static final int PUA_END   = 0xF8FF;

    private static final IntOpenHashSet knownPuaCodepoints   = new IntOpenHashSet();
    private static final IntOpenHashSet zeroWidthCodepoints  = new IntOpenHashSet();

    private PuaCodepointRegistry() {}

    public static void registerZeroWidth(int codepoint) {
        if (codepoint >= PUA_START && codepoint <= PUA_END) {
            zeroWidthCodepoints.add(codepoint);
        }
    }

    public static boolean isZeroWidth(int codepoint) {
        return zeroWidthCodepoints.contains(codepoint);
    }

    public static void markKnown(int codepoint) {
        knownPuaCodepoints.add(codepoint);
    }

    public static boolean isKnownPua(int codepoint) {
        return codepoint >= PUA_START && codepoint <= PUA_END
            && knownPuaCodepoints.contains(codepoint);
    }

    /** True if any zero-width PUA glyphs have been registered (used for TX1 font mipmap check) */
    public static boolean hasPuaGlyphs() {
        return !zeroWidthCodepoints.isEmpty();
    }

    /** Clear on resource reload, glyph set is rebuilt from the new pack */
    public static void clear() {
        knownPuaCodepoints.clear();
        zeroWidthCodepoints.clear();
    }
}
