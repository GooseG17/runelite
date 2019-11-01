package net.runelite.client.plugins.automation;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum Types
{
	GATHERING("Objects"),
	COMBAT("NPCs");

	private String name;

	@Override
	public String toString()
	{
		return getName();
	}
}
