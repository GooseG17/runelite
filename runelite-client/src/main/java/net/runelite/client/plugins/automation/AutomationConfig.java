package net.runelite.client.plugins.automation;

import java.awt.Color;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Keybind;
import net.runelite.client.config.Range;

@ConfigGroup("automationConfig")
public interface AutomationConfig extends Config
{
	@ConfigItem(
		keyName = "hotkey",
		name = "Hotkey for toggle",
		description = "",
		position = 0
	)
	default Keybind hotkey()
	{
		return Keybind.NOT_SET;
	}

	@ConfigItem(
		keyName = "Type",
		name = "Type",
		description = "Select activity to perform automatically.",
		position = 1
	)
	default Types getType()
	{
		return Types.GATHERING;
	}

	@ConfigItem(
		keyName = "items",
		name = "Items to Drop",
		description = "Separate with comma",
		position = 0
	)
	default String items()
	{
		return "0";
	}

	@ConfigItem(
		keyName = "markerColor",
		name = "Marker color",
		description = "Configures the outer color of object marker",
		titleSection = "colorTitle",
		position = 2
	)
	default Color objectMarkerColor()
	{
		return Color.RED;
	}

	@Range(
		max = 100
	)
	@ConfigItem(
		keyName = "objectMarkerAlpha",
		name = "Alpha",
		description = "Configures the opacity/alpha of object marker",
		titleSection = "colorTitle",
		position = 3
	)
	default int objectMarkerAlpha()
	{
		return 100;
	}

	@ConfigItem(
		keyName = "delayMin",
		name = "Min Delay",
		description = "",
		position = 4
	)
	default int delayMin()
	{
		return 500;
	}

	@ConfigItem(
		keyName = "delayMax",
		name = "Max Delay",
		description = "",
		position = 5
	)
	default int delayMax()
	{
		return 1000;
	}
}
