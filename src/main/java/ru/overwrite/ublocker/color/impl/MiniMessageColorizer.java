package ru.overwrite.ublocker.color.impl;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.jetbrains.annotations.Nullable;
import ru.overwrite.ublocker.color.Colorizer;

public class MiniMessageColorizer implements Colorizer {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer LEGACY_COMPONENT_SERIALIZER = LegacyComponentSerializer.legacySection();

    @Override
    public String colorize(@Nullable String message) {
        if (message == null || message.isEmpty()) {
            return message;
        }
        Component component = MINI_MESSAGE.deserialize(message);
        return LEGACY_COMPONENT_SERIALIZER.serialize(component);
    }
}
