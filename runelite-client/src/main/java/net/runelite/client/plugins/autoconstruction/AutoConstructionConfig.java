package net.runelite.client.plugins.autoconstruction;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Keybind;

@ConfigGroup("autoConstructionConfig")
public interface AutoConstructionConfig extends Config
{
    @ConfigItem(
            keyName = "hotkey",
            name = "Click hotkey",
            description = "",
            position = 0
    )
    default Keybind hotkey()
    {
        return Keybind.NOT_SET;
    }

    @ConfigItem(
            keyName = "delayMin",
            name = "Min Delay",
            description = "",
            position = 1
    )
    default int delayMin()
    {
        return 50;
    }

    @ConfigItem(
            keyName = "delayMax",
            name = "Max Delay",
            description = "",
            position = 2
    )
    default int delayMax()
    {
        return 80;
    }
}

