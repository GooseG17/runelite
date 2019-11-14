/*
 * Copyright (c) 2018, Nickolaj <https://github.com/fire-proof>
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
package net.runelite.client.plugins.nightmarezone;

import com.google.inject.Provides;
import java.awt.Color;
import java.awt.Rectangle;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.AccessLevel;
import lombok.Getter;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.ItemID;
import net.runelite.api.Point;
import net.runelite.api.Skill;
import net.runelite.api.Varbits;
import net.runelite.api.events.ChatMessage;
import net.runelite.client.events.ConfigChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.util.Text;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.api.widgets.WidgetItem;
import net.runelite.client.Notifier;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.flexo.Flexo;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.stretchedmode.StretchedModeConfig;
import net.runelite.client.ui.overlay.OverlayManager;

@PluginDescriptor(
	name = "Nightmare Zone",
	description = "Show NMZ points/absorption and/or notify about expiring potions",
	tags = {"combat", "nmz", "minigame", "notifications"}
)
@Singleton
public class NightmareZonePlugin extends Plugin
{
	private static final int[] NMZ_MAP_REGION = {9033};
	private static final Duration HOUR = Duration.ofHours(1);

	@Inject
	private Notifier notifier;

	@Inject
	private Client client;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private ConfigManager configManager;

	@Inject
	private NightmareZoneConfig config;

	@Inject
	private NightmareZoneOverlay overlay;

	@Inject
	private EventBus eventBus;

	@Getter
	private int pointsPerHour;
	
	private Instant nmzSessionStartTime;

	// This starts as true since you need to get
	// above the threshold before sending notifications
	private boolean absorptionNotificationSend = true;

	@Getter(AccessLevel.PACKAGE)
	private boolean moveOverlay;
	@Getter(AccessLevel.PACKAGE)
	private boolean showtotalpoints;
	private boolean powerSurgeNotification;
	private boolean recurrentDamageNotification;
	private boolean zapperNotification;
	private boolean ultimateForceNotification;
	private boolean overloadNotification;
	private boolean absorptionNotification;
	private boolean autonmz;
	private int maxHP;
	private int minStatBoost;
	private Flexo flexo;
	private BlockingQueue<Runnable> queue = new ArrayBlockingQueue<>(1);
	private ThreadPoolExecutor executorService = new ThreadPoolExecutor(1, 1, 1, TimeUnit.SECONDS, queue,
		new ThreadPoolExecutor.DiscardPolicy());
	@Getter(AccessLevel.PACKAGE)
	private int absorptionThreshold;
	@Getter(AccessLevel.PACKAGE)
	private Color absorptionColorAboveThreshold;
	@Getter(AccessLevel.PACKAGE)
	private Color absorptionColorBelowThreshold;

	private static final int[] ABSORB_POTION = {
		ItemID.ABSORPTION_1,
		ItemID.ABSORPTION_2,
		ItemID.ABSORPTION_3,
		ItemID.ABSORPTION_4
	};
	private static final int[] COMBAT_POTION = {
		ItemID.SUPER_COMBAT_POTION1,
		ItemID.SUPER_COMBAT_POTION1_23549,
		ItemID.SUPER_COMBAT_POTION2,
		ItemID.SUPER_COMBAT_POTION2_23547,
		ItemID.SUPER_COMBAT_POTION3,
		ItemID.SUPER_COMBAT_POTION3_23545,
		ItemID.SUPER_COMBAT_POTION4,
		ItemID.SUPER_COMBAT_POTION4_23543
	};
	private static final int[] ROCK_CAKE = {
		ItemID.DWARVEN_ROCK_CAKE,
		ItemID.DWARVEN_ROCK_CAKE_7510
	};

	@Override
	protected void startUp() throws Exception
	{
		Flexo.client = client;
		executorService.submit(() ->
		{
			flexo = null;
			try
			{
				flexo = new Flexo();
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		});
		updateConfig();
		addSubscriptions();

		overlayManager.add(overlay);
		overlay.removeAbsorptionCounter();
	}

	@Override
	protected void shutDown() throws Exception
	{
		eventBus.unregister(this);

		overlayManager.remove(overlay);
		overlay.removeAbsorptionCounter();

		Widget nmzWidget = client.getWidget(WidgetInfo.NIGHTMARE_ZONE);

		if (nmzWidget != null)
		{
			nmzWidget.setHidden(false);
		}

		resetPointsPerHour();
	}

	private void addSubscriptions()
	{
		eventBus.subscribe(ConfigChanged.class, this, this::onConfigChanged);
		eventBus.subscribe(GameTick.class, this, this::onGameTick);
		eventBus.subscribe(ChatMessage.class, this, this::onChatMessage);
		eventBus.subscribe(NpcDespawned.class, this, this::onNpcDespawned);
	}

	private void onConfigChanged(ConfigChanged event)
	{
		if (!event.getGroup().equals("nightmareZone"))
		{
			return;
		}

		updateConfig();
		overlay.updateConfig();
	}

	@Provides
	NightmareZoneConfig getConfig(ConfigManager configManager)
	{
		return configManager.getConfig(NightmareZoneConfig.class);
	}

	private void onGameTick(GameTick event)
	{
		if (isNotInNightmareZone())
		{
			if (!absorptionNotificationSend)
			{
				absorptionNotificationSend = true;
			}

			if (nmzSessionStartTime != null)
			{
				resetPointsPerHour();
			}

			return;
		}
		else if (autonmz)
		{

		}

		if (this.absorptionNotification)
		{
			checkAbsorption();
		}

		if (config.moveOverlay())
		{
			pointsPerHour = calculatePointsPerHour();
		}
	}

	private void onChatMessage(ChatMessage event)
	{
		if (event.getType() != ChatMessageType.GAMEMESSAGE
			|| isNotInNightmareZone())
		{
			return;
		}

		String msg = Text.removeTags(event.getMessage()); //remove color
		if (msg.contains("The effects of overload have worn off, and you feel normal again."))
		{
			if (this.overloadNotification)
			{
				notifier.notify("Your overload has worn off");
			}
		}
		else if (msg.contains("A power-up has spawned:"))
		{
			if (msg.contains("Power surge"))
			{
				if (this.powerSurgeNotification)
				{
					notifier.notify(msg);
				}
			}
			else if (msg.contains("Recurrent damage"))
			{
				if (this.recurrentDamageNotification)
				{
					notifier.notify(msg);
				}
			}
			else if (msg.contains("Zapper"))
			{
				if (this.zapperNotification)
				{
					notifier.notify(msg);
				}
			}
			else if (msg.contains("Ultimate force"))
			{
				if (this.ultimateForceNotification)
				{
					notifier.notify(msg);
				}
			}
		}
	}

	private void onNpcDespawned(NpcDespawned npc)
	{
		if (autonmz && !isNotInNightmareZone())
		{
			if (!isCombatPotionActive() || client.getLocalPlayer().getHealth() > maxHP || !isAbsorptionActive())
			{
				int order = randomDelay(1, 3);
				if (order == 1)
				{
					useRockCake();
					drinkAbsorb();
					drinkCombatPot();
					setValues();
				}
				else if (order == 2)
				{
					useRockCake();
					drinkCombatPot();
					drinkAbsorb();
					setValues();
				}
				else if (order == 3)
				{
					drinkCombatPot();
					useRockCake();
					drinkAbsorb();
					setValues();
				}
			}
		}
	}

	private void checkAbsorption()
	{
		int absorptionPoints = client.getVar(Varbits.NMZ_ABSORPTION);

		if (!absorptionNotificationSend)
		{
			if (absorptionPoints < this.absorptionThreshold)
			{
				notifier.notify("Absorption points below: " + this.absorptionThreshold);
				absorptionNotificationSend = true;
			}
		}
		else
		{
			if (absorptionPoints > this.absorptionThreshold)
			{
				absorptionNotificationSend = false;
			}
		}
	}

	private boolean isAbsorptionActive()
	{
		return client.getVar(Varbits.NMZ_ABSORPTION) > 50;
	}

	private boolean isCombatPotionActive()
	{
		return (client.getBoostedSkillLevel(Skill.STRENGTH) - client.getRealSkillLevel(Skill.STRENGTH)) > minStatBoost;
	}

	private void useRockCake()
	{
		mouseItem(ROCK_CAKE);
		while (client.getLocalPlayer().getHealth() > 1)
		{
			executorService.submit(() ->
			{
				flexo.delay(randomDelay(610, 680));
				click();
			});
		}
	}

	private void drinkAbsorb()
	{
		while (client.getVar(Varbits.NMZ_ABSORPTION) < 951)
		{
			mouseItem(ABSORB_POTION);
			executorService.submit(() ->
			{
				for (int i = 0; i < randomDelay(4, 7); i++)
				{
					flexo.delay(randomDelay(610, 680));
					click();
				}
			});
		}
	}

	private void drinkCombatPot()
	{
		if (client.getBoostedSkillLevel(Skill.STRENGTH) - client.getRealSkillLevel(Skill.STRENGTH) < 6)
		{
			clickItem(COMBAT_POTION);
		}
	}

	private void mouseItem(int[] itemIds)
	{
		Rectangle rect = getWidgetItems(itemIds).get(0).getCanvasBounds();
		Point p = getClickPoint(rect);
		Point pos = client.getMouseCanvasPosition();
		if (!rect.contains(pos.getX(), pos.getY()))
		{
			flexo.mouseMove(p.getX(), p.getY());
		}
	}

	private void click()
	{
		flexo.mousePressAndRelease(1);
	}

	private void clickItem(int[] itemIds)
	{
		mouseItem(itemIds);
		click();
	}

	private void setValues()
	{
		maxHP = ThreadLocalRandom.current().nextInt(3, 9 + 1);
		minStatBoost = ThreadLocalRandom.current().nextInt(2, 5 + 1);
	}

	private List<WidgetItem> getWidgetItems(int[] itemIds)
	{
		Widget inventoryWidget = client.getWidget(WidgetInfo.INVENTORY);

		ArrayList<Integer> itemIDs = new ArrayList<>();

		for (int i : itemIds)
		{
			itemIDs.add(i);
		}

		List<WidgetItem> listToReturn = new ArrayList<>();

		for (WidgetItem item : inventoryWidget.getWidgetItems())
		{
			if (itemIDs.contains(item.getId()))
			{
				listToReturn.add(item);
			}
		}

		return listToReturn;
	}

	private Point getClickPoint(Rectangle rect)
	{
		int x, y;
		if (randomDelay(0, 10) > 1)
		{
			x = randomDelay(rect.x, rect.x + (rect.width / 2));
			y = randomDelay(rect.y, rect.y + (rect.height / 2));
		}
		else
		{
			x = randomDelay(rect.x, rect.x + (int) (rect.width * .9));
			y = randomDelay(rect.y, rect.y + (int) (rect.height * .9));
		}
		if (client.isStretchedEnabled())
		{
			double scalingFactor = configManager.getConfig(StretchedModeConfig.class).scalingFactor();
			double scale = 1 + (scalingFactor / 100);
			return new Point((int) (x * scale), (int) (y * scale));
		}
		else
		{
			return new Point(x, y);
		}
	}

	private int randomDelay(int min, int max)
	{
		return ThreadLocalRandom.current().nextInt(min, max + 1);
	}

	boolean isNotInNightmareZone()
	{
		return !Arrays.equals(client.getMapRegions(), NMZ_MAP_REGION);
	}

	private int calculatePointsPerHour()
	{
		Instant now = Instant.now();
		final int currentPoints = client.getVar(Varbits.NMZ_POINTS);

		if (nmzSessionStartTime == null)
		{
			nmzSessionStartTime = now;
		}

		Duration timeSinceStart = Duration.between(nmzSessionStartTime, now);

		if (!timeSinceStart.isZero())
		{
			return (int) ((double) currentPoints * (double) HOUR.toMillis() / (double) timeSinceStart.toMillis());
		}

		return 0;
	}

	private void resetPointsPerHour()
	{
		nmzSessionStartTime = null;
		pointsPerHour = 0;
	}

	private void updateConfig()
	{
		this.moveOverlay = config.moveOverlay();
		this.showtotalpoints = config.showtotalpoints();
		this.powerSurgeNotification = config.powerSurgeNotification();
		this.recurrentDamageNotification = config.recurrentDamageNotification();
		this.zapperNotification = config.zapperNotification();
		this.ultimateForceNotification = config.ultimateForceNotification();
		this.overloadNotification = config.overloadNotification();
		this.absorptionNotification = config.absorptionNotification();
		this.absorptionThreshold = config.absorptionThreshold();
		this.absorptionColorAboveThreshold = config.absorptionColorAboveThreshold();
		this.absorptionColorBelowThreshold = config.absorptionColorBelowThreshold();
		this.autonmz = config.autoNMZ();
	}
}
