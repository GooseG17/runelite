package net.runelite.client.plugins.actionrecorder;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import com.google.inject.Provides;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import net.runelite.api.AnimationID;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.NPC;
import net.runelite.api.ObjectDefinition;
import net.runelite.api.ObjectID;
import net.runelite.api.Point;
import net.runelite.api.Tile;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.events.AnimationChanged;
import net.runelite.api.events.FocusChanged;
import net.runelite.api.events.GameObjectDespawned;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.InteractingChanged;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.NpcSpawned;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.api.widgets.WidgetItem;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.events.NpcLootReceived;
import net.runelite.client.flexo.Flexo;
import net.runelite.client.input.KeyListener;
import net.runelite.client.input.KeyManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.PluginType;
import net.runelite.client.plugins.stretchedmode.StretchedModeConfig;
import net.runelite.client.ui.overlay.OverlayManager;

@PluginDescriptor(
	name = "Action Recorder",
	enabledByDefault = false,
	type = PluginType.EXTERNAL
)

public class ActionRecorder extends Plugin implements KeyListener
{
	@Inject
	private Client client;

	@Inject
	private EventBus eventBus;

	@Inject
	private ActionRecorderConfig config;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private ActionRecorderOverlay overlay;

	@Inject
	private KeyManager keyManager;

	@Inject
	private ConfigManager configManager;

	private final BlockingQueue<Runnable> queue = new ArrayBlockingQueue<>(1);
	private final ThreadPoolExecutor executorService = new ThreadPoolExecutor(
		1, 1, 30, TimeUnit.SECONDS, queue, new ThreadPoolExecutor.DiscardPolicy()
	);

	private boolean recording = false;
	private boolean playing = false;
	private Flexo flexo;
	private List<Integer> markedObjectIDs = new ArrayList<>();
	private List<Tile> markedObjectTiles = new ArrayList<>();
	List<GameObject> markedObjects = new ArrayList<>();
	List<NPC> markedNPCs = new ArrayList<>();

	private static final Set<Integer> MINING_ANIMATIONS = ImmutableSet.of(
		AnimationID.MINING_BRONZE_PICKAXE,
		AnimationID.MINING_IRON_PICKAXE,
		AnimationID.MINING_STEEL_PICKAXE,
		AnimationID.MINING_BLACK_PICKAXE,
		AnimationID.MINING_MITHRIL_PICKAXE,
		AnimationID.MINING_ADAMANT_PICKAXE,
		AnimationID.MINING_RUNE_PICKAXE,
		AnimationID.MINING_DRAGON_PICKAXE,
		AnimationID.MINING_DRAGON_PICKAXE_OR,
		AnimationID.MINING_DRAGON_PICKAXE_UPGRADED,
		AnimationID.MINING_INFERNAL_PICKAXE,
		AnimationID.MINING_3A_PICKAXE,
		AnimationID.MINING_MOTHERLODE_RUNE,
		AnimationID.MINING_MOTHERLODE_DRAGON,
		AnimationID.MINING_MOTHERLODE_DRAGON_OR,
		AnimationID.MINING_MOTHERLODE_DRAGON_UPGRADED
	);

	private static final Set<Integer> EMPTY_ROCKS = ImmutableSet.of(
		ObjectID.ROCKS_11390,
		ObjectID.ROCKS_11391
	);

	@Provides
	ActionRecorderConfig getConfig(ConfigManager configManager)
	{
		return configManager.getConfig(ActionRecorderConfig.class);
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
		eventBus.subscribe(NpcLootReceived.class, this, this::onNpcLootReceived);
		eventBus.subscribe(GameObjectSpawned.class, this, this::onGameObjectSpawned);
		eventBus.subscribe(GameObjectDespawned.class, this, this::onGameObjectDespawned);
		eventBus.subscribe(InteractingChanged.class, this, this::onInteractingChanged);
		eventBus.subscribe(ItemContainerChanged.class, this, this::onItemContainerChanged);
		overlayManager.add(overlay);
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
	}

	@Override
	protected void shutDown()
	{
		flexo = null;
		recording = false;
		playing = false;
		eventBus.unregister(this);
		keyManager.unregisterKeyListener(this);
		overlayManager.remove(overlay);
	}

	@Override
	public void keyTyped(KeyEvent e)
	{

	}

	@Override
	public void keyPressed(KeyEvent e)
	{
		final int recordKey = config.recordKey().getKeyCode();
		final int playbackKey = config.playbackKey().getKeyCode();
		if (e.getKeyCode() == recordKey && !recording)
		{
			recording = true;
		}
		else if (e.getKeyCode() == recordKey && recording)
		{
			recording = false;
		}
		else if (e.getKeyCode() == playbackKey && playing)
		{
			playing = true;
		}
		else if (e.getKeyCode() == playbackKey && !playing)
		{
			playing = false;
		}
	}

	@Override
	public void keyReleased(KeyEvent e)
	{

	}

	private void onConfigChanged(ConfigChanged event)
	{

	}

	private void onFocusChanged(FocusChanged focusChanged)
	{

	}

	private void onMenuEntryAdded(MenuEntryAdded event)
	{

	}

	private void onMenuOptionClicked(MenuOptionClicked event)
	{
		final Tile tile = client.getScene().getTiles()[client.getPlane()][event.getParam0()][event.getParam1()];
	}

	private void onInteractingChanged(InteractingChanged event)
	{

	}

	private void onGameTick(GameTick event)
	{

	}

	private void onAnimationChanged(AnimationChanged event)
	{
		/*if (toggledOn && type == Types.GATHERING && event.getActor() == client.getLocalPlayer())
		{
			isMining = MINING_ANIMATIONS.contains(client.getLocalPlayer().getAnimation());
		}*/
	}

	private void onGameObjectSpawned(GameObjectSpawned event)
	{

	}

	private void onGameObjectDespawned(GameObjectDespawned event)
	{

	}

	private void onNpcSpawned(NpcSpawned npc)
	{

	}

	private void onNpcDespawned(NpcDespawned npc)
	{

	}

	private void onNpcLootReceived(NpcLootReceived event)
	{

	}

	private void onItemContainerChanged(ItemContainerChanged event)
	{

	}

	private int inventoryCount()
	{
		return client.getWidget(WidgetInfo.INVENTORY).getWidgetItems().size();
	}

	private GameObject findGameObject(Tile tile, int id)
	{
		if (tile == null)
		{
			return null;
		}

		final GameObject[] tileGameObjects = tile.getGameObjects();

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

	private GameObject getNearestObject()
	{
		List<Double> tmp = new ArrayList<>();
		final GameObject[] temp = {null};
		final LocalPoint player = client.getLocalPlayer().getLocalLocation();
		final Point playerPoint = new Point(player.getSceneX(), player.getSceneY());


		for (Tile tile : markedObjectTiles)
		{
			GameObject[] objects = tile.getGameObjects();
			for (GameObject object : objects)
			{
				if (markedObjectIDs.contains(object.getId()))
				{
					final Rectangle b = object.getConvexHull().getBounds();
					final double distance = distance(playerPoint.getX(), playerPoint.getY(), (int) b.getCenterX(), (int) b.getCenterY());
					tmp.add(distance);
				}
			}
		}

		double lowest = Collections.min(tmp);

		for (Tile tile : markedObjectTiles)
		{
			GameObject[] objects = tile.getGameObjects();
			for (GameObject object : objects)
			{
				if (markedObjectIDs.contains(object.getId()))
				{
					final Rectangle b = object.getConvexHull().getBounds();
					if (distance(playerPoint.getX(), playerPoint.getY(), (int) b.getCenterX(), (int) b.getCenterY()) == lowest)
					{
						temp[0] = object;
					}
				}
			}
		}
		return temp[0];
	}

	private void clickObject()
	{
		//GameObject object = getNearestObject();
		GameObject object = null;
		for (Tile tile : markedObjectTiles)
		{
			if (markedObjectIDs.contains(tile.getGameObjects()[0].getId()))
			{
				object = tile.getGameObjects()[0];
			}
		}
		if (object == null)
		{
			System.out.println("clickObject target is null");
			return;
		}
		Point point = getClickPoint(object.getConvexHull().getBounds());
		leftClick(point);
	}

	private void click()
	{
		flexo.mousePressAndRelease(1);
	}

	public void leftClick()
	{
		final ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();
		service.schedule(this::click, randomDelay(50, 100), TimeUnit.MILLISECONDS);
		service.shutdown();
	}

	public void leftClick(int x, int y)
	{
		moveMouse(x, y);
		leftClick();
	}

	public void leftClick(Point point)
	{
		moveMouse(point);
		leftClick();
	}

	public void leftClick(java.awt.Point jPoint)
	{
		moveMouse(jPoint);
		leftClick();
	}

	private void moveMouse(int x, int y)
	{
		flexo.mouseMove(x, y);
	}

	private void moveMouse(Point point)
	{
		java.awt.Point jPoint = new java.awt.Point(point.getX(), point.getY());
		flexo.mouseMove(jPoint);
	}

	private void moveMouse(java.awt.Point jPoint)
	{
		flexo.mouseMove(jPoint);
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

	private double distance(int x1, int y1, int x2, int y2)
	{
		int dx = x2 - x1;
		int dy = y2 - y1;
		return (Math.sqrt(dx * dx + dy * dy));
	}

	public List<Widget> getEquippedItems(int[] itemIds)
	{
		Widget equipmentWidget = client.getWidget(WidgetInfo.EQUIPMENT);

		ArrayList<Integer> equippedIds = new ArrayList<>();

		for (int i : itemIds)
		{
			equippedIds.add(i);
		}

		List<Widget> equipped = new ArrayList<>();

		if (equipmentWidget.getStaticChildren() != null)
		{
			for (Widget widgets : equipmentWidget.getStaticChildren())
			{
				for (Widget items : widgets.getDynamicChildren())
				{
					if (equippedIds.contains(items.getItemId()))
					{
						equipped.add(items);
					}
				}
			}
		}

		return equipped;
	}

	public List<WidgetItem> getItems(int[] itemIds)
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
}

