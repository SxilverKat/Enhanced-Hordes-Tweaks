package com.enhancedhordes.tweaks.util;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

public final class VersionCompat {

    private VersionCompat() {}

    public static Level level(Entity entity) {
        //? if >=1.20.1 {
        return entity.level();
        //?} else {
        /*return entity.level;*/
        //?}
    }

    public static net.minecraft.network.chat.MutableComponent literal(String text) {
        //? if >=1.19.2 {
        return net.minecraft.network.chat.Component.literal(text);
        //?} else {
        /*return new net.minecraft.network.chat.TextComponent(text);*/
        //?}
    }
}
