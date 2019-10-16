/*
 * Copyright (c) 2019, Ganom <https://github.com/Ganom>
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
package net.runelite.client.plugins.autoconstruction;

import com.google.inject.Provides;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import net.runelite.api.DecorativeObject;
import net.runelite.api.GameObject;
import net.runelite.api.ObjectDefinition;
import net.runelite.api.Tile;
import net.runelite.api.TileObject;
import net.runelite.api.queries.TileObjectQuery;
import net.runelite.client.flexo.Flexo;
import net.runelite.api.Client;
import net.runelite.api.MenuEntry;
import net.runelite.api.events.FocusChanged;
import net.runelite.api.events.AnimationChanged;
import net.runelite.api.events.NpcSpawned;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.ConfigChanged;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.input.KeyListener;
import net.runelite.client.input.KeyManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.PluginType;
import net.runelite.client.plugins.stretchedmode.StretchedModeConfig;

@PluginDescriptor(
	name = "Auto Clicker for Construction",
	enabledByDefault = false,
	type = PluginType.EXTERNAL
)

public class AutoConstructionPlugin extends Plugin implements KeyListener
{
	@Inject
	private Client client;
	@Inject
	private EventBus eventBus;
	@Inject
	private AutoConstructionConfig config;
	@Inject
	private KeyManager keyManager;
	@Inject
	private ConfigManager configManager;
	private boolean toggledOn;
	private boolean butlerPresent;
	private Flexo flexo;
	private Point2D point;
	private String lastClicked;

	@Provides
	AutoConstructionConfig getConfig(ConfigManager configManager)
	{
		return configManager.getConfig(AutoConstructionConfig.class);
	}

	@Override
	protected void startUp()
	{
		keyManager.registerKeyListener(this);
		eventBus.subscribe(FocusChanged.class, this, this::onFocusChanged);
		eventBus.subscribe(AnimationChanged.class, this, this::onAnimationChanged);
		eventBus.subscribe(ConfigChanged.class, this, this::onConfigChanged);
		eventBus.subscribe(GameTick.class, this, this::onGameTick);
		eventBus.subscribe(MenuEntryAdded.class, this, this::onMenuEntryAdded);
		eventBus.subscribe(MenuOptionClicked.class, this, this::onMenuOptionClicked);
		eventBus.subscribe(NpcSpawned.class, this, this::onNpcSpawned);
		eventBus.subscribe(NpcDespawned.class, this, this::onNpcDespawned);
		toggledOn = false;
		flexo = null;
		try
		{
			flexo = new Flexo();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	@Override
	protected void shutDown()
	{
		flexo = null;
		toggledOn = false;
		eventBus.unregister(this);
		keyManager.unregisterKeyListener(this);
	}

	@Override
	public void keyTyped(KeyEvent e)
	{

	}

	@Override
	public void keyPressed(KeyEvent e)
	{
		final int keycode = config.hotkey().getKeyCode();
		if (e.getKeyCode() == keycode && !toggledOn)
		{
			toggledOn = true;
		}
		else if (e.getKeyCode() == keycode && toggledOn)
		{
			toggledOn = false;
		}
	}

	@Override
	public void keyReleased(KeyEvent e)
	{

	}

	private void onFocusChanged(FocusChanged focusChanged)
	{
		toggledOn = false;
	}

	private void onAnimationChanged(AnimationChanged animationChanged)
	{

	}

	private void onConfigChanged(ConfigChanged configChanged)
	{

	}

	private void onGameTick(GameTick gameTick)
	{
		if (toggledOn)
		{
			MenuEntry[] entries = client.getMenuEntries();
			for (MenuEntry entry : entries)
			{
				if (entry.getOption().equals("Talk-to"))
				{
					return;
				}
			}
			for (MenuEntry entry : entries)
			{
				if (entry.getOption().equals("Build"))
				{
					if (!entry.getOption().equals(lastClicked))
					{
						//client.setLeftClickMenuEntry(entry);
						singleClick();
						return;
					}
				}
			}

		}
	}

	private void onMenuEntryAdded(MenuEntryAdded entry)
	{

	}

	private void onMenuOptionClicked(MenuOptionClicked entry)
	{
		lastClicked = entry.getOption();
	}

	private void onNpcSpawned(NpcSpawned npc)
	{
		if (toggledOn && npc.getActor().getName().equals("Demon butler"))
		{
			doubleClick();
			flexo.holdKey(KeyEvent.VK_1, randomDelay(20, 40));
			butlerPresent = true;
		}
	}

	private void onNpcDespawned(NpcDespawned npc)
	{
		if (toggledOn && npc.getActor().getName().equals("Demon butler"))
		{
			singleClick();
			butlerPresent = false;
		}
	}

	private void singleClick()
	{
		delayFirstClick();
	}

	private void doubleClick()
	{
		delayFirstClick();
		delaySecondClick();
	}

	private void delayFirstClick()
	{
		final ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();
		service.schedule(this::simLeftClick, randomDelay(config.delayMin(), config.delayMax()), TimeUnit.MILLISECONDS);
		service.shutdown();
	}

	private void delaySecondClick()
	{
		final ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();
		service.schedule(this::simLeftClick, randomDelay(config.delayMin() * 2, config.delayMax() * 2), TimeUnit.MILLISECONDS);
		service.shutdown();
	}

	private void simLeftClick()
	{
		leftClick();
	}

	public void leftClick()
	{
		double scalingFactor = configManager.getConfig(StretchedModeConfig.class).scalingFactor();
		if (client.isStretchedEnabled())
		{
			double scale = 1 + (scalingFactor / 100);

			MouseEvent mousePressed =
				new MouseEvent(client.getCanvas(), 501, System.currentTimeMillis(), 0, (int) (client.getMouseCanvasPosition().getX() * scale), (int) (client.getMouseCanvasPosition().getY() * scale), 1, false, 1);
			client.getCanvas().dispatchEvent(mousePressed);
			MouseEvent mouseReleased =
				new MouseEvent(client.getCanvas(), 502, System.currentTimeMillis(), 0, (int) (client.getMouseCanvasPosition().getX() * scale), (int) (client.getMouseCanvasPosition().getY() * scale), 1, false, 1);
			client.getCanvas().dispatchEvent(mouseReleased);
			MouseEvent mouseClicked =
				new MouseEvent(client.getCanvas(), 500, System.currentTimeMillis(), 0, (int) (client.getMouseCanvasPosition().getX() * scale), (int) (client.getMouseCanvasPosition().getY() * scale), 1, false, 1);
			client.getCanvas().dispatchEvent(mouseClicked);
		}
		if (!client.isStretchedEnabled())
		{
			MouseEvent mousePressed =
				new MouseEvent(client.getCanvas(), 501, System.currentTimeMillis(), 0, client.getMouseCanvasPosition().getX(), client.getMouseCanvasPosition().getY(), 1, false, 1);
			client.getCanvas().dispatchEvent(mousePressed);
			MouseEvent mouseReleased =
				new MouseEvent(client.getCanvas(), 502, System.currentTimeMillis(), 0, client.getMouseCanvasPosition().getX(), client.getMouseCanvasPosition().getY(), 1, false, 1);
			client.getCanvas().dispatchEvent(mouseReleased);
			MouseEvent mouseClicked =
				new MouseEvent(client.getCanvas(), 500, System.currentTimeMillis(), 0, client.getMouseCanvasPosition().getX(), client.getMouseCanvasPosition().getY(), 1, false, 1);
			client.getCanvas().dispatchEvent(mouseClicked);
		}
	}

	private int randomDelay(int min, int max)
	{
		Random rand = new Random();
		int n = rand.nextInt(max) + 1;
		if (n < min)
		{
			n += min;
		}
		return n;
	}

	private TileObject findTileObject(Tile tile, int id)
	{
		if (tile == null)
		{
			return null;
		}

		final GameObject[] tileGameObjects = tile.getGameObjects();
		final DecorativeObject tileDecorativeObject = tile.getDecorativeObject();

		if (tileDecorativeObject != null && tileDecorativeObject.getId() == id)
		{
			return tileDecorativeObject;
		}

		for (GameObject object : tileGameObjects)
		{
			if (object == null)
			{
				continue;
			}

			if (object.getId() == id)
			{
				return object;
			}

			// Check impostors
			final ObjectDefinition comp = client.getObjectDefinition(object.getId());

			if (comp.getImpostorIds() != null)
			{
				for (int impostorId : comp.getImpostorIds())
				{
					if (impostorId == id)
					{
						return object;
					}
				}
			}
		}

		return null;
	}
}

