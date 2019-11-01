/*
 * Copyright (c) 2018, DennisDeV <https://github.com/DevDennis>
 * Copyright (c) 2019, Ganom <https://github.com/ganom>
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

import com.google.inject.Provides;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Point;
import net.runelite.api.Prayer;
import net.runelite.api.VarClientStr;
import net.runelite.api.events.GameTick;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.input.KeyListener;
import net.runelite.client.input.KeyManager;
import net.runelite.client.input.MouseListener;
import net.runelite.client.input.MouseManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.PluginType;
import net.runelite.client.plugins.stretchedmode.StretchedModeConfig;
import net.runelite.client.ui.overlay.OverlayManager;

@PluginDescriptor(
	name = "Auto Pray Flick",
	enabledByDefault = false,
	type = PluginType.EXTERNAL
)
@Slf4j
public class AutoPrayFlickPlugin extends Plugin implements KeyListener, MouseListener
{
	private static final int[] NMZ_MAP_REGION = {9033};

	boolean toggleFlick = false;
	@Inject
	private OverlayManager overlayManager;
	@Inject
	private AutoPrayFlickOverlay autoPrayFlickOverlay;
	@Inject
	private Client client;
	@Inject
	private AutoPrayFlickConfig config;
	@Inject
	private KeyManager keyManager;
	@Inject
	private MouseManager mouseManager;
	@Inject
	private ConfigManager configManager;
	@Inject
	private EventBus eventBus;
	private boolean held = false;
	private boolean firstFlick = false;

	private final BlockingQueue<Runnable> queue = new ArrayBlockingQueue<>(1);
	private final ThreadPoolExecutor executorService = new ThreadPoolExecutor(
		1, 1, 25, TimeUnit.SECONDS, queue, new ThreadPoolExecutor.DiscardPolicy()
	);

	private static int randomDelay(int min, int max)
	{
		Random rand = new Random();
		int n = rand.nextInt(max) + 1;
		if (n < min)
		{
			n += min;
		}
		return n;
	}

	@Provides
	AutoPrayFlickConfig getConfig(ConfigManager configManager)
	{
		return configManager.getConfig(AutoPrayFlickConfig.class);
	}

	@Override
	protected void startUp()
	{
		addSubscriptions();
		keyManager.registerKeyListener(this);
		mouseManager.registerMouseListener(this);
		overlayManager.add(autoPrayFlickOverlay);
	}

	@Override
	protected void shutDown()
	{
		keyManager.unregisterKeyListener(this);
		overlayManager.remove(autoPrayFlickOverlay);
		eventBus.unregister(this);
	}

	private void addSubscriptions()
	{
		//eventBus.subscribe(FocusChanged.class, this, this::onFocusChanged);
		eventBus.subscribe(GameTick.class, this, this::onGameTick);
	}

	@Override
	public void keyTyped(KeyEvent e)
	{

	}

	@Override
	public void keyPressed(KeyEvent e)
	{
		if (config.useMouse())
		{
			return;
		}
		final int keycode = config.hotkey2().getKeyCode();
		if (e.getKeyCode() == keycode && toggleFlick && !held)
		{
			toggleFlick = false;
			firstFlick = true;
			int p = 0;
			for (Prayer prayer : Prayer.values())
			{
				if (!client.isPrayerActive(prayer))
				{
					p++;
				}
			}
			if (p != 29 && !toggleFlick)
			{
				executorService.submit(() ->
				{
					mouseOverPrayer();
					singleClick();
				});
			}
		}
		else if (e.getKeyCode() == keycode && !toggleFlick && !held)
		{
			mouseOverPrayer();
			toggleFlick = true;
			firstFlick = true;
		}
		if (config.holdMode())
		{
			held = true;
			firstFlick = true;
		}
	}

	@Override
	public void keyReleased(KeyEvent e)
	{
		if (config.useMouse())
		{
			return;
		}
		if (config.holdMode())
		{
			toggleFlick = false;
			held = false;
			firstFlick = false;
		}
		if (config.clearChat() && config.hotkey2().matches(e))
		{
			String chat = client.getVar(VarClientStr.CHATBOX_TYPED_TEXT);
			if (chat.endsWith(String.valueOf(e.getKeyChar())))
			{
				chat = chat.substring(0, chat.length() - 1);
				client.setVar(VarClientStr.CHATBOX_TYPED_TEXT, chat);
			}
		}
	}

	private void onGameTick(GameTick event)
	{
		if (config.onlyInNmz() && !isInNightmareZone())
		{
			return;
		}
		executorService.submit(() ->
		{
			if (toggleFlick && config.clicks())
			{
				int p = 0;
				for (Prayer prayer : Prayer.values())
				{
					if (!client.isPrayerActive(prayer))
					{
						p++;
					}
				}
				if (p == 29 && !firstFlick)
				{
					singleClick();
					return;
				}
				doubleClick();
				if (firstFlick)
				{
					firstFlick = false;
				}
			}
			if (toggleFlick && !config.clicks())
			{
				singleClick();
			}
		});
	}

	//private void onFocusChanged(FocusChanged focusChanged)
	//{
	//	if (!focusChanged.isFocused())
	//	{
	//		toggleFlick = false;
	//		firstFlick = false;
	//	}
	//}

	private void doubleClick()
	{
		delayFirstClick();
		delaySecondClick();
	}

	private void singleClick()
	{
		delayFirstClick();
	}

	private void delayFirstClick()
	{
		final ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();
		service.schedule(this::simLeftClick, randomDelay(config.randLowFirst(), config.randHighFirst()), TimeUnit.MILLISECONDS);
		service.shutdown();
	}

	private void delaySecondClick()
	{
		final ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();
		service.schedule(this::simLeftClick, randomDelay(config.randLowSecond(), config.randHighSecond()), TimeUnit.MILLISECONDS);
		service.shutdown();
	}

	private void simLeftClick()
	{
		if (config.moveOnTick())
		{
			mouseOverPrayer();
		}
		if (isMouseOverPrayer())
		{
			leftClick();
		}
	}

	private boolean isMouseOverPrayer()
	{
		Point tmp = client.getMouseCanvasPosition();
		java.awt.Point mousePos = new java.awt.Point(tmp.getX(), tmp.getY());
		Widget xpOrb = client.getWidget(WidgetInfo.MINIMAP_QUICK_PRAYER_ORB);
		if (!xpOrb.getBounds().contains(mousePos))
		{
			return false;
		}
		else
		{
			return true;
		}
	}

	private void mouseOverPrayer()
	{
		if (!isMouseOverPrayer())
		{
			Widget xpOrb = client.getWidget(WidgetInfo.MINIMAP_QUICK_PRAYER_ORB);
			Point randomPoint = getClickPoint(xpOrb.getBounds());
			moveMouse(randomPoint);
		}
	}

	private void moveMouse(Point point)
	{
		MouseEvent mouseEntered = new MouseEvent(client.getCanvas(), 504, System.currentTimeMillis(), 0, point.getX(), point.getY(), 0, false);
		client.getCanvas().dispatchEvent(mouseEntered);
		MouseEvent mouseExited = new MouseEvent(client.getCanvas(), 505, System.currentTimeMillis(), 0, point.getX(), point.getY(), 0, false);
		client.getCanvas().dispatchEvent(mouseExited);
		MouseEvent mouseMoved = new MouseEvent(client.getCanvas(), 503, System.currentTimeMillis(), 0, point.getX(), point.getY(), 0, false);
		client.getCanvas().dispatchEvent(mouseMoved);
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

	private Point getClickPoint(Rectangle rect)
	{
		double scalingFactor = configManager.getConfig(StretchedModeConfig.class).scalingFactor();
		boolean stretchedMode = client.isStretchedEnabled();
		if (stretchedMode)
		{
			int rand = (Math.random() <= 0.5) ? 1 : 2;
			int x = (int) (rect.getX() + (rand * 3) + rect.getWidth() / 2);
			int y = (int) (rect.getY() + (rand * 3) + rect.getHeight() / 2);
			double scale = 1 + (scalingFactor / 100);
			return new Point((int) (x * scale), (int) (y * scale));
		}
		else
		{
			int rand = (Math.random() <= 0.5) ? 1 : 2;
			int x = (int) (rect.getX() + (rand * 3) + rect.getWidth() / 2);
			int y = (int) (rect.getY() + (rand * 3) + rect.getHeight() / 2);
			return new Point(x, y);
		}
	}

	@Override
	public MouseEvent mouseClicked(MouseEvent e)
	{
		return e;
	}

	@Override
	public MouseEvent mousePressed(MouseEvent e)
	{
		if (!config.useMouse())
		{
			return e;
		}
		if (e.getButton() == config.mouseButton() && toggleFlick && !held)
		{
			toggleFlick = false;
			firstFlick = false;
		}
		else if (e.getButton() == config.mouseButton() && !toggleFlick && !held)
		{
			toggleFlick = true;
			firstFlick = true;
		}
		if (config.holdMode() && e.getButton() == config.mouseButton())
		{
			held = true;
			firstFlick = true;
		}
		return e;
	}

	@Override
	public MouseEvent mouseReleased(MouseEvent e)
	{
		if (config.holdMode() && e.getButton() == config.mouseButton())
		{
			toggleFlick = false;
			firstFlick = false;
			held = false;
		}
		return e;
	}

	@Override
	public MouseEvent mouseEntered(MouseEvent e)
	{
		return e;
	}

	@Override
	public MouseEvent mouseExited(MouseEvent e)
	{
		return e;
	}

	@Override
	public MouseEvent mouseDragged(MouseEvent e)
	{
		return e;
	}

	@Override
	public MouseEvent mouseMoved(MouseEvent e)
	{
		return e;
	}

	private boolean isInNightmareZone()
	{
		return Arrays.equals(client.getMapRegions(), NMZ_MAP_REGION);
	}
}
