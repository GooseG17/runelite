package net.runelite.client.plugins.actionrecorder;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Keybind;

@ConfigGroup("actionrecorder")
public interface ActionRecorderConfig extends Config
{
	@ConfigItem(
		keyName = "recordKey",
		name = "Key to toggle recording",
		description = "",
		position = 0
	)
	default Keybind recordKey()
	{
		return Keybind.NOT_SET;
	}

	@ConfigItem(
		keyName = "playbackKey",
		name = "Key to toggle playback",
		description = "",
		position = 1
	)
	default Keybind playbackKey()
	{
		return Keybind.NOT_SET;
	}
}

