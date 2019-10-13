/*
 * Copyright (c) 2018, DennisDeV <https://github.com/DevDennis>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package net.runelite.client.plugins.autoprayflick;

import java.awt.event.KeyEvent;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Keybind;

@ConfigGroup("autoprayflick")
public interface AutoPrayFlickConfig extends Config
{
	@ConfigItem(
		keyName = "onlyInNmz",
		name = "Only flick in NMZ",
		description = "If you leave nmz, it will stop working.",
		position = 13
	)
	default boolean onlyInNmz()
	{
		return false;
	}

	@ConfigItem(
		keyName = "useMouse",
		name = "Use Mouse Button",
		description = "Uses mouse button instead of /",
		position = 1
	)
	default boolean useMouse()
	{
		return false;
	}

	@ConfigItem(
		keyName = "clearChat",
		name = "Clear Chat if using a keybind",
		description = "Clears Chat",
		position = 2
	)
	default boolean clearChat()
	{
		return false;
	}

	@ConfigItem(
		keyName = "hotkey2",
		name = "Flick hotkey",
		description = "When you press this key pray flicking will start",
		position = 3
	)
	default Keybind hotkey2()
	{
		Keybind k = new Keybind(KeyEvent.VK_BACK_SLASH, 0);
		return k;
	}

	@ConfigItem(
		keyName = "mouseButton",
		name = "Mouse Button",
		description = "Which mouse button should it work on, i.e 1,2,3,4,5",
		position = 4
	)
	default int mouseButton()
	{
		return 4;
	}

	@ConfigItem(
		keyName = "holdMode",
		name = "Hold Mode",
		description = "Hold down key/mouse instead of toggle",
		position = 5
	)
	default boolean holdMode()
	{
		return false;
	}

	@ConfigItem(
		keyName = "clicks",
		name = "Double Click Mode",
		description = "Enabled = Double Click, Disabled = Single Click",
		position = 6
	)
	default boolean clicks()
	{
		return true;
	}

	@ConfigItem(
		keyName = "display",
		name = "Draw Indicator",
		description = "Draws circle around prayer",
		position = 7
	)
	default boolean display()
	{
		return true;
	}

	@ConfigItem(
			keyName = "randLowFirst",
			name = "First Click Delay Low",
			description = "First click delay low.",
			position = 8
	)
	default int randLowFirst()
	{
		return 1;
	}

	@ConfigItem(
			keyName = "randHighFirst",
			name = "First Click Delay High",
			description = "First click delay high.",
			position = 9
	)
	default int randHighFirst()
	{
		return 50;
	}

	@ConfigItem(
			keyName = "randLowSecond",
			name = "Second Click Delay Low",
			description = "Second click delay low.",
			position = 10
	)
	default int randLowSecond()
	{
		return 50;
	}

	@ConfigItem(
			keyName = "randHighSecond",
			name = "Second Click Delay High",
			description = "Second click delay high.",
			position = 11
	)
	default int randHighSecond()
	{
		return 90;
	}

	@ConfigItem(
			keyName = "moveOnTick",
			name = "Set mouse on each tick",
			description = "If enabled, sets the mouse over the prayer orb on each tick.",
			position = 12
	)
	default boolean moveOnTick()
	{
		return false;
	}
}