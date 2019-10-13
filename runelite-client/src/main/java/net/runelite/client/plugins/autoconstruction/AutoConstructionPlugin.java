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
import java.util.Random;
import java.util.concurrent.*;
import javax.inject.Inject;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.VarClientStr;
import net.runelite.api.events.*;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.input.KeyManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.PluginType;
import net.runelite.client.plugins.stretchedmode.StretchedModeConfig;
import net.runelite.client.util.HotkeyListener;

@PluginDescriptor(
        name = "Auto Clicker for Construction",
        enabledByDefault = false,
        type = PluginType.EXTERNAL
)
public class AutoConstructionPlugin extends Plugin
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
    private final BlockingQueue<Runnable> queue = new ArrayBlockingQueue<>(1);
    private final ThreadPoolExecutor executorService = new ThreadPoolExecutor(1, 1,
            10, TimeUnit.SECONDS, queue, new ThreadPoolExecutor.DiscardPolicy());
    private boolean run;
    @Getter(AccessLevel.PACKAGE)
    @Setter(AccessLevel.PACKAGE)
    private boolean toggledOn;

    @Provides
    AutoConstructionConfig getConfig(ConfigManager configManager)
    {
        return configManager.getConfig(AutoConstructionConfig.class);
    }

    @Override
    protected void startUp()
    {
        eventBus.subscribe(FocusChanged.class, this, this::onFocusChanged);
        eventBus.subscribe(AnimationChanged.class, this, this::onAnimationChanged);
        eventBus.subscribe(ConfigChanged.class, this, this::onConfigChanged);
        eventBus.subscribe(GameTick.class, this, this::onGameTick);
        eventBus.subscribe(MenuEntryAdded.class, this, this::onMenuEntryAdded);
        eventBus.subscribe(MenuOptionClicked.class, this, this::onMenuOptionClicked);
        eventBus.subscribe(NpcSpawned.class, this, this::onNpcSpawned);
        eventBus.subscribe(NpcDespawned.class, this, this::onNpcDespawned);
        keyManager.registerKeyListener(hotkeyListener);
    }

    @Override
    protected void shutDown()
    {
        keyManager.unregisterKeyListener(hotkeyListener);
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

    }

    private void onMenuEntryAdded(MenuEntryAdded entry)
    {
        if (toggledOn)
        {
            if (entry.getOption().equals("Build")) {
                singleClick();
            } else if (entry.getOption().equals("Remove")) {
                singleClick();
            } else if (entry.getOption().equals("Talk-to")) {
                doubleClick();
            }
        }
    }

    private void onMenuOptionClicked(MenuOptionClicked entry)
    {

    }

    private void onNpcSpawned(NpcSpawned npc)
    {

    }

    private void onNpcDespawned(NpcDespawned npc)
    {

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
        String chat = client.getVar(VarClientStr.CHATBOX_TYPED_TEXT);
        if (chat.endsWith(String.valueOf(e.getKeyChar())))
        {
            chat = chat.substring(0, chat.length() - 1);
            client.setVar(VarClientStr.CHATBOX_TYPED_TEXT, chat);
        }
    }

    private HotkeyListener hotkeyListener = new HotkeyListener(() -> config.hotkey())
    {
        @Override
        public void hotkeyPressed()
        {
            run = !run;
            executorService.submit(() ->
            {
                while (run)
                {
                    if (client.getGameState() != GameState.LOGGED_IN)
                    {
                        run = false;
                        break;
                    }

                    simLeftClick();

                    try
                    {
                        Thread.sleep(randomDelay(config.delayMin(), config.delayMax()));
                    }
                    catch (InterruptedException e)
                    {
                        e.printStackTrace();
                    }
                }
            });
        }
    };

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
        service.schedule(this::simLeftClick, randomDelay(config.delayMin() * 3, config.delayMax() * 3), TimeUnit.MILLISECONDS);
        service.shutdown();
    }

    private void simLeftClick()
    {
        leftClick(client, configManager);
    }

    public static void leftClick(Client client, ConfigManager configManager)
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
}

