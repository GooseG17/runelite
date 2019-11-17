/*
 * Copyright (c) 2019, ganom <https://github.com/Ganom>
 * Copyright (c) 2019, TomC <https://github.com/tomcylke>
 * All rights reserved.
 * Licensed under GPL3, see LICENSE for the full scope.
 */
package net.runelite.client.plugins.oneclick;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Provides;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import net.runelite.api.AnimationID;
import net.runelite.api.Client;
import net.runelite.api.DynamicObject;
import net.runelite.api.Entity;
import net.runelite.api.GameObject;
import net.runelite.api.GameState;
import net.runelite.api.ItemID;
import net.runelite.api.MenuEntry;
import net.runelite.api.MenuOpcode;
import static net.runelite.api.ObjectID.DWARF_MULTICANNON;
import net.runelite.api.Player;
import net.runelite.api.Skill;
import net.runelite.api.SpriteID;
import net.runelite.api.Varbits;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.events.MenuOpened;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.api.widgets.WidgetItem;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.PluginType;
import org.apache.commons.lang3.tuple.Pair;

@PluginDescriptor(
	name = "One Click",
	description = "OP One Click methods.",
	enabledByDefault = false,
	type = PluginType.EXTERNAL
)
public class OneClickPlugin extends Plugin
{
	private static final Set<Integer> BOLTS = ImmutableSet.of(
		ItemID.BRONZE_BOLTS_UNF, ItemID.IRON_BOLTS_UNF, ItemID.STEEL_BOLTS_UNF,
		ItemID.MITHRIL_BOLTS_UNF, ItemID.ADAMANT_BOLTSUNF, ItemID.RUNITE_BOLTS_UNF,
		ItemID.DRAGON_BOLTS_UNF, ItemID.UNFINISHED_BROAD_BOLTS
	);
	private static final Set<Integer> ARROW_TIPS = ImmutableSet.of(
		ItemID.BRONZE_ARROWTIPS, ItemID.IRON_ARROWTIPS, ItemID.STEEL_ARROWTIPS,
		ItemID.MITHRIL_ARROWTIPS, ItemID.ADAMANT_ARROWTIPS, ItemID.RUNE_ARROWTIPS,
		ItemID.DRAGON_ARROWTIPS, ItemID.BROAD_ARROWHEADS
	);
	private static final Set<Integer> DART_TIPS = ImmutableSet.of(
		ItemID.BRONZE_DART_TIP, ItemID.IRON_DART_TIP, ItemID.STEEL_DART_TIP,
		ItemID.MITHRIL_DART_TIP, ItemID.ADAMANT_DART_TIP, ItemID.RUNE_DART_TIP,
		ItemID.DRAGON_DART_TIP
	);
	private static final Set<Integer> LOG_ID = ImmutableSet.of(
		ItemID.LOGS, ItemID.OAK_LOGS, ItemID.WILLOW_LOGS, ItemID.TEAK_LOGS,
		ItemID.MAPLE_LOGS, ItemID.MAHOGANY_LOGS, ItemID.YEW_LOGS, ItemID.MAGIC_LOGS,
		ItemID.REDWOOD_LOGS
	);
	private static final Set<Integer> HOPS_SEED = ImmutableSet.of(
		ItemID.BARLEY_SEED, ItemID.HAMMERSTONE_SEED, ItemID.ASGARNIAN_SEED,
		ItemID.JUTE_SEED, ItemID.YANILLIAN_SEED, ItemID.KRANDORIAN_SEED, ItemID.WILDBLOOD_SEED
	);
	private static final Set<Integer> TITHE_SEEDS = ImmutableSet.of(
		ItemID.GOLOVANOVA_SEED, ItemID.BOLOGANO_SEED, ItemID.LOGAVANO_SEED
	);
	private static final Set<Integer> HERBS = ImmutableSet.of(
		ItemID.GUAM_LEAF, ItemID.MARRENTILL, ItemID.TARROMIN, ItemID.HARRALANDER
	);
	private static final Set<Integer> BONE_SET = ImmutableSet.of(
		ItemID.BONES, ItemID.WOLF_BONE, ItemID.BURNT_BONES, ItemID.MONKEY_BONES, ItemID.BAT_BONES,
		ItemID.JOGRE_BONE, ItemID.BIG_BONES, ItemID.ZOGRE_BONE, ItemID.SHAIKAHAN_BONES, ItemID.BABYDRAGON_BONES,
		ItemID.WYRM_BONES, ItemID.DRAGON_BONES, ItemID.DRAKE_BONES, ItemID.FAYRG_BONES, ItemID.LAVA_DRAGON_BONES,
		ItemID.RAURG_BONES, ItemID.HYDRA_BONES, ItemID.DAGANNOTH_BONES, ItemID.OURG_BONES, ItemID.SUPERIOR_DRAGON_BONES,
		ItemID.WYVERN_BONES
	);
	private static final Set<Integer> LOG_FLETCH = ImmutableSet.of(ItemID.LOGS, ItemID.ACHEY_TREE_LOGS, ItemID.OAK_LOGS, ItemID.WILLOW_LOGS,
		ItemID.TEAK_LOGS, ItemID.MAPLE_LOGS, ItemID.YEW_LOGS, ItemID.MAGIC_LOGS, ItemID.REDWOOD_LOGS, ItemID.BRUMA_ROOT
	);
	private static final Set<Integer> POTION_VIAL = ImmutableSet.of(ItemID.VIAL_OF_WATER, ItemID.COCONUT_MILK, ItemID.VIAL_OF_BLOOD
	);
	private static final Set<Integer> POTION_HERBS = ImmutableSet.of(ItemID.GUAM_LEAF, ItemID.MARRENTILL, ItemID.TARROMIN, ItemID.HARRALANDER,
		ItemID.RANARR_WEED, ItemID.TOADFLAX, ItemID.IRIT_LEAF, ItemID.AVANTOE, ItemID.KWUARM, ItemID.SNAPDRAGON, ItemID.CADANTINE,
		ItemID.LANTADYME, ItemID.DWARF_WEED, ItemID.TORSTOL, ItemID.CACTUS_SPINE, ItemID.CAVE_NIGHTSHADE
	);
	private static final Set<Integer> POTION_INGREDIENT = ImmutableSet.of(ItemID.EYE_OF_NEWT, ItemID.LIMPWURT_ROOT,
		ItemID.WHITE_BERRIES, ItemID.POTATO_CACTUS, ItemID.WINE_OF_ZAMORAK, ItemID.GOAT_HORN_DUST, ItemID.DRAGON_SCALE_DUST,
		ItemID.JANGERBERRIES, ItemID.CRUSHED_NEST, ItemID.RED_SPIDERS_EGGS, ItemID.CHOCOLATE_DUST, ItemID.SNAPE_GRASS,
		ItemID.MORT_MYRE_FUNGUS, ItemID.UNICORN_HORN_DUST, ItemID.YEW_ROOTS, ItemID.MAGIC_ROOTS, ItemID.TOADS_LEGS,
		ItemID.KEBBIT_TEETH_DUST, ItemID.POISON_IVY_BERRIES, ItemID.VOLCANIC_ASH, ItemID.ASHES, ItemID.TORSTOL,
		ItemID.LAVA_SCALE_SHARD, ItemID.CRUSHED_SUPERIOR_DRAGON_BONES, ItemID.CRYSTAL_DUST, ItemID.AMYLASE_CRYSTAL,
		ItemID.ZULRAHS_SCALES
	);
	private static final Set<Integer> POTION_UNF_NAMES = ImmutableSet.of(ItemID.GUAM_POTION_UNF, ItemID.MARRENTILL_POTION_UNF,
		ItemID.TARROMIN_POTION_UNF, ItemID.HARRALANDER_POTION_UNF, ItemID.RANARR_POTION_UNF, ItemID.TOADFLAX_POTION_UNF,
		ItemID.IRIT_POTION_UNF, ItemID.AVANTOE_POTION_UNF, ItemID.KWUARM_POTION_UNF, ItemID.SNAPDRAGON_POTION_UNF,
		ItemID.CADANTINE_POTION_UNF, ItemID.CADANTINE_BLOOD_POTION_UNF, ItemID.LANTADYME_POTION_UNF, ItemID.ANTIDOTE_UNF,
		ItemID.DWARF_WEED_POTION_UNF, ItemID.WEAPON_POISON_UNF, ItemID.TORSTOL_POTION_UNF, ItemID.ANTIDOTE_UNF_5951,
		ItemID.WEAPON_POISON_UNF_5939, ItemID.REJUVENATION_POTION_UNF
	);
	private static final Set<Integer> UPGRADABLE_POTIONS = ImmutableSet.of(ItemID.SUPER_ATTACK4, ItemID.SUPER_DEFENCE4,
		ItemID.ANTIFIRE_POTION4, ItemID.SUPER_ANTIFIRE_POTION4, ItemID.RANGING_POTION4, ItemID.MAGIC_POTION4,
		ItemID.SUPER_COMBAT_POTION4, ItemID.SUPER_ENERGY4, ItemID.ANTIDOTE4_5952, ItemID.ANTIVENOM4
	);
	private static final Set<String> BIRD_HOUSES_NAMES = ImmutableSet.of(
		"<col=ffff>Bird house (empty)", "<col=ffff>Oak birdhouse (empty)", "<col=ffff>Willow birdhouse (empty)",
		"<col=ffff>Teak birdhouse (empty)", "<col=ffff>Maple birdhouse (empty)", "<col=ffff>Mahogany birdhouse (empty)",
		"<col=ffff>Yew birdhouse (empty)", "<col=ffff>Magic birdhouse (empty)", "<col=ffff>Redwood birdhouse (empty)"
	);
	private static final String MAGIC_IMBUE_EXPIRED_MESSAGE = "Your Magic Imbue charge has ended.";
	private static final String MAGIC_IMBUE_MESSAGE = "You are charged to combine runes!";

	@Inject
	private Client client;
	@Inject
	private OneClickConfig config;
	@Inject
	private EventBus eventBus;

	private final Map<Integer, String> targetMap = new HashMap<>();

	private AlchItem alchItem;
	private GameObject cannon;
	private Types type = Types.NONE;
	private boolean cannonFiring;
	private boolean enableImbue;
	private boolean imbue;
	private boolean tick;
	private int prevCannonAnimation = 514;

	@Provides
	OneClickConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(OneClickConfig.class);
	}

	@Override
	protected void startUp()
	{
		addSubscriptions();
		type = config.getType();
		enableImbue = config.isUsingImbue();
	}

	@Override
	protected void shutDown()
	{
		eventBus.unregister(this);
	}

	private void addSubscriptions()
	{
		eventBus.subscribe(ChatMessage.class, this, this::onChatMessage);
		eventBus.subscribe(ConfigChanged.class, this, this::onConfigChanged);
		eventBus.subscribe(GameObjectSpawned.class, this, this::onGameObjectSpawned);
		eventBus.subscribe(GameStateChanged.class, this, this::onGameStateChanged);
		eventBus.subscribe(GameTick.class, this, this::onGameTick);
		eventBus.subscribe(MenuEntryAdded.class, this, this::onMenuEntryAdded);
		eventBus.subscribe(MenuOpened.class, this, this::onMenuOpened);
		eventBus.subscribe(MenuOptionClicked.class, this, this::onMenuOptionClicked);
	}

	private void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOGGED_IN && imbue)
		{
			imbue = false;
		}
	}

	private void onConfigChanged(ConfigChanged event)
	{
		if (event.getGroup().equals("oneclick"))
		{
			type = config.getType();
			enableImbue = config.isUsingImbue();
		}
	}

	private void onChatMessage(ChatMessage event)
	{
		switch (event.getMessage())
		{
			case "You pick up the cannon. It's really heavy.":
				cannonFiring = false;
				cannon = null;
				break;
			case MAGIC_IMBUE_MESSAGE:
				imbue = true;
				break;
			case MAGIC_IMBUE_EXPIRED_MESSAGE:
				imbue = false;
				break;
		}
	}

	private void onGameObjectSpawned(GameObjectSpawned event)
	{
		final GameObject gameObject = event.getGameObject();
		final Player localPlayer = client.getLocalPlayer();
		if (gameObject.getId() == DWARF_MULTICANNON && cannon == null && localPlayer != null &&
			localPlayer.getWorldLocation().distanceTo(gameObject.getWorldLocation()) <= 2 &&
			localPlayer.getAnimation() == AnimationID.BURYING_BONES)
		{
			cannon = gameObject;
		}
	}

	private void onGameTick(GameTick event)
	{
		if (cannon != null)
		{
			final Entity entity = cannon.getEntity();
			if (entity instanceof DynamicObject)
			{
				final int anim = ((DynamicObject) entity).getAnimationID();
				if (anim == 514 && prevCannonAnimation == 514)
				{
					cannonFiring = false;
				}
				else if (anim != prevCannonAnimation)
				{
					cannonFiring = true;
				}
				prevCannonAnimation = ((DynamicObject) entity).getAnimationID();
			}
		}
		tick = false;
	}

	private void onMenuOpened(MenuOpened event)
	{
		final MenuEntry firstEntry = event.getFirstEntry();

		if (firstEntry == null)
		{
			return;
		}

		final int widgetId = firstEntry.getParam1();

		if (widgetId == WidgetInfo.INVENTORY.getId() && type == Types.HIGH_ALCH)
		{
			final Widget spell = client.getWidget(WidgetInfo.SPELL_HIGH_LEVEL_ALCHEMY);

			if (spell == null)
			{
				return;
			}

			if (spell.getSpriteId() != SpriteID.SPELL_HIGH_LEVEL_ALCHEMY ||
				spell.getSpriteId() == SpriteID.SPELL_HIGH_LEVEL_ALCHEMY_DISABLED ||
				client.getBoostedSkillLevel(Skill.MAGIC) < 55 ||
				client.getVar(Varbits.SPELLBOOK) != 0)
			{
				alchItem = null;
				return;
			}

			final int itemId = firstEntry.getIdentifier();

			if (itemId == -1)
			{
				return;
			}

			final MenuEntry[] menuList = new MenuEntry[event.getMenuEntries().length + 1];

			for (int i = event.getMenuEntries().length - 1; i >= 0; i--)
			{
				if (i == 0)
				{
					menuList[i] = event.getMenuEntries()[i];
				}
				else
				{
					menuList[i + 1] = event.getMenuEntries()[i];
				}
			}

			final MenuEntry setHighAlchItem = new MenuEntry();
			final boolean set = alchItem != null && alchItem.getId() == firstEntry.getIdentifier();
			setHighAlchItem.setOption(set ? "Unset" : "Set");
			setHighAlchItem.setTarget("<col=00ff00>High Alchemy Item <col=ffffff> -> " + firstEntry.getTarget());
			setHighAlchItem.setIdentifier(set ? -1 : firstEntry.getIdentifier());
			setHighAlchItem.setOpcode(MenuOpcode.RUNELITE.getId());
			setHighAlchItem.setParam1(widgetId);
			setHighAlchItem.setForceLeftClick(false);
			menuList[1] = setHighAlchItem;
			event.setMenuEntries(menuList);
			event.setModified();
		}
	}


	private void onMenuEntryAdded(MenuEntryAdded event)
	{
		final int id = event.getIdentifier();
		final int opcode = event.getOpcode();
		targetMap.put(id, event.getTarget());

		if (opcode == MenuOpcode.ITEM_USE.getId() && ItemID.SALTPETRE == id)
		{
			if (findItem(ItemID.COMPOST).getLeft() == -1)
			{
				return;
			}
			event.setOption("Make fertilizer");
			event.setTarget("<col=ff9040>Compost<col=ffffff> -> " + targetMap.get(id));
			event.setModified();
		}
		else if (opcode == MenuOpcode.ITEM_USE.getId() && ItemID.POT == id)
		{
			if (findItem(ItemID.VOLCANIC_SULPHUR).getLeft() == -1 || findItem(ItemID.SALTPETRE).getLeft() == -1 || findItem(ItemID.JUNIPER_CHARCOAL).getLeft() == -1)
			{
				return;
			}
			event.setOption("Mix dynamite");
			event.setTarget("<col=ff9040>Volcanic sulphur<col=ffffff> -> " + targetMap.get(id));
			event.setModified();
		}
		else if (opcode == MenuOpcode.ITEM_USE.getId() && ItemID.DYNAMITE_POT == id)
		{
			if (findItem(ItemID.BALL_OF_WOOL).getLeft() == -1)
			{
				return;
			}
			event.setOption("Add fuse");
			event.setTarget(targetMap.get(id));
			event.setModified();
		}
		else if (opcode == MenuOpcode.NPC_FIRST_OPTION.getId() &&
			event.getOption().toLowerCase().contains("talk") && event.getTarget().toLowerCase().contains("wounded soldier"))
		{
			if (findItem(ItemID.SHAYZIEN_MEDPACK).getLeft() == -1)
			{
				return;
			}
			event.setOption("Heal");
			event.setTarget(event.getTarget());
			event.setModified();
		}
		else if (opcode == MenuOpcode.ITEM_USE.getId() && (DART_TIPS.contains(id) || BOLTS.contains(id)))
		{
			if (findItem(ItemID.FEATHER).getLeft() == -1)
			{
				return;
			}
			event.setOption("Add feather");
			event.setTarget(targetMap.get(id));
			event.setModified();
		}
		else if (opcode == MenuOpcode.ITEM_USE.getId() && LOG_ID.contains(id))
		{
			if (findItem(ItemID.TINDERBOX).getLeft() == -1)
			{
				return;
			}
			event.setOption("Light");
			event.setTarget(targetMap.get(id));
			event.setModified();
		}
		else if (opcode == MenuOpcode.ITEM_USE.getId() && id == ItemID.CHISEL)
		{
			if (findItem(ItemID.DARK_ESSENCE_BLOCK).getLeft() == -1)
			{
				return;
			}
			event.setOption("Chisel");
			event.setTarget(targetMap.get(id));
			event.setModified();
		}
		else if (opcode == MenuOpcode.ITEM_USE.getId() && id == ItemID.ARROW_SHAFT)
		{
			if (findItem(ItemID.FEATHER).getLeft() == -1)
			{
				return;
			}
			event.setOption("Attach feather");
			event.setTarget(targetMap.get(id));
			event.setModified();
		}
		else if (opcode == MenuOpcode.ITEM_USE.getId() && id == ItemID.HEADLESS_ARROW)
		{
			if (findItem(ARROW_TIPS).getLeft() == -1)
			{
				return;
			}
			event.setOption("Finish");
			event.setTarget(targetMap.get(id));
			event.setModified();
		}
		else if (opcode == MenuOpcode.EXAMINE_OBJECT.getId() && event.getTarget().contains("Sink"))
		{
			if (findItem(ItemID.BOWL).getLeft() == -1)
			{
				return;
			}
			event.setOption("Fill bowl");
			event.setTarget(targetMap.get(id));
			event.setModified();
		}
		else if (opcode == MenuOpcode.ITEM_USE.getId() && id == ItemID.BOWL_OF_WATER)
		{
			if (findItem(ItemID.SERVERY_FLOUR).getLeft() == -1)
			{
				return;
			}
			event.setOption("Make dough");
			event.setTarget(targetMap.get(id));
			event.setModified();
		}
		else if (opcode == MenuOpcode.ITEM_USE.getId() && id == ItemID.SERVERY_PIZZA_BASE)
		{
			if (findItem(ItemID.SERVERY_TOMATO).getLeft() == -1)
			{
				return;
			}
			event.setOption("Add sauce");
			event.setTarget(targetMap.get(id));
			event.setModified();
		}
		else if (opcode == MenuOpcode.ITEM_USE.getId() && id == ItemID.SERVERY_INCOMPLETE_PIZZA)
		{
			if (findItem(ItemID.SERVERY_CHEESE).getLeft() == -1)
			{
				return;
			}
			event.setOption("Add cheese");
			event.setTarget(targetMap.get(id));
			event.setModified();
		}
		else if (opcode == MenuOpcode.ITEM_USE.getId() && id == ItemID.SERVERY_PINEAPPLE)
		{
			if (findItem(ItemID.KNIFE).getLeft() == -1)
			{
				return;
			}
			event.setOption("Slice");
			event.setTarget(targetMap.get(id));
			event.setModified();
		}
		else if (opcode == MenuOpcode.ITEM_USE.getId() && id == ItemID.SERVERY_PLAIN_PIZZA)
		{
			if (findItem(ItemID.SERVERY_PINEAPPLE_CHUNKS).getLeft() == -1)
			{
				return;
			}
			event.setOption("Add pineapple");
			event.setTarget(targetMap.get(id));
			event.setModified();
		}
		else if (opcode == MenuOpcode.EXAMINE_OBJECT.getId() && event.getTarget().equals("Tithe patch"))
		{
			if (findItem(TITHE_SEEDS).getLeft() == -1)
			{
				return;
			}
			event.setOption("Plant seed");
			event.setTarget(targetMap.get(id));
			event.setModified();
		}
		else if (opcode == MenuOpcode.ITEM_USE.getId() && LOG_FLETCH.contains(id))
		{
			if (findItem(ItemID.KNIFE).getLeft() == -1)
			{
				return;
			}
			event.setOption("Fletch");
			event.setTarget(targetMap.get(id));
			event.setModified();
		}
		else if (opcode == MenuOpcode.ITEM_USE.getId() && POTION_HERBS.contains(id))
		{
			if (findItem(POTION_VIAL).getLeft() == -1)
			{
				return;
			}
			event.setOption("Add herb");
			event.setTarget(targetMap.get(id));
			event.setModified();
		}
		else if (opcode == MenuOpcode.ITEM_USE.getId() && POTION_UNF_NAMES.contains(id))
		{
			if (findItem(POTION_INGREDIENT).getLeft() == -1)
			{
				return;
			}
			event.setOption("Make potion");
			event.setTarget(targetMap.get(id));
			event.setModified();
		}
		else if (opcode == MenuOpcode.ITEM_USE.getId() && POTION_INGREDIENT.contains(id))
		{
			if (findItem(UPGRADABLE_POTIONS).getLeft() == -1)
			{
				return;
			}
			event.setOption("Upgrade potion");
			event.setTarget(targetMap.get(id));
			event.setModified();
		}

		switch (type)
		{
			case BIRDHOUSES:
				if (opcode == MenuOpcode.GAME_OBJECT_SECOND_OPTION.getId() && BIRD_HOUSES_NAMES.contains(event.getTarget()))
				{
					if (findItem(HOPS_SEED).getLeft() == -1)
					{
						return;
					}
					event.setOption("Use");
					event.setTarget("<col=ff9040>Hops seed<col=ffffff> -> " + targetMap.get(id));
					event.setOpcode(MenuOpcode.ITEM_USE_ON_GAME_OBJECT.getId());
					event.setModified();
				}
				break;
			case HERB_TAR:
				if (opcode == MenuOpcode.ITEM_USE.getId() && HERBS.contains(id))
				{
					if (findItem(ItemID.SWAMP_TAR).getLeft() == -1 || findItem(ItemID.PESTLE_AND_MORTAR).getLeft() == -1)
					{
						return;
					}
					event.setTarget("<col=ff9040>Swamp tar<col=ffffff> -> " + targetMap.get(id));
					event.setModified();
				}
				break;
			case LAVA_RUNES:
				if (opcode == MenuOpcode.GAME_OBJECT_FIRST_OPTION.getId() && event.getOption().equals("Craft-rune") && event.getTarget().equals("<col=ffff>Altar"))
				{
					if (findItem(ItemID.EARTH_RUNE).getLeft() == -1)
					{
						return;
					}

					if (!imbue && enableImbue)
					{
						event.setOption("Use");
						event.setTarget("<col=ff9040>Magic Imbue<col=ffffff> -> <col=ffff>Yourself");
						event.setModified();
						return;
					}
					event.setOption("Use");
					event.setTarget("<col=ff9040>Earth rune<col=ffffff> -> <col=ffff>Altar");
					event.setModified();
				}
				break;
			case HIGH_ALCH:
				if (opcode == MenuOpcode.WIDGET_TYPE_2.getId() && alchItem != null && event.getOption().equals("Cast") && event.getTarget().equals("<col=00ff00>High Level Alchemy</col>"))
				{
					event.setOption("Cast");
					event.setTarget("<col=00ff00>High Level Alchemy</col><col=ffffff> -> " + alchItem.getName());
					event.setModified();
				}
				break;
			case DWARF_CANNON:
				if (cannonFiring && event.getIdentifier() == DWARF_MULTICANNON && opcode == MenuOpcode.GAME_OBJECT_FIRST_OPTION.getId())
				{
					if (findItem(ItemID.CANNONBALL).getLeft() == -1)
					{
						return;
					}
					event.setOption("Use");
					event.setTarget("<col=ff9040>Cannonball<col=ffffff> -> <col=ffff>Dwarf multicannon");
					event.setModified();
				}
				break;
			case BONES:
				if (opcode == MenuOpcode.GAME_OBJECT_FIRST_OPTION.getId() && event.getOption().toLowerCase().contains("pray") && event.getTarget().toLowerCase().contains("altar"))
				{
					if (findItem(BONE_SET).getLeft() == -1)
					{
						return;
					}
					event.setOption("Use");
					event.setTarget("<col=ff9040>Bones<col=ffffff> -> " + event.getTarget());
					event.setModified();
				}
				break;
			case KARAMBWANS:
				if (opcode == MenuOpcode.GAME_OBJECT_FIRST_OPTION.getId() && event.getOption().equals("Cook"))
				{
					if (findItem(ItemID.RAW_KARAMBWAN).getLeft() == -1)
					{
						return;
					}
					event.setOption("Use");
					event.setTarget("<col=ff9040>Raw karambwan<col=ffffff> -> " + event.getTarget());
					event.setModified();
				}
				break;
		}
	}

	private void onMenuOptionClicked(MenuOptionClicked event)
	{
		final String target = event.getTarget();
		final int opcode = event.getOpcode();

		if (tick)
		{
			event.consume();
			return;
		}

		if (event.getTarget() == null)
		{
			return;
		}

		if (opcode == MenuOpcode.NPC_FIRST_OPTION.getId() && event.getOption().equals("Heal"))
		{
			if (updateSelectedItem(ItemID.SHAYZIEN_MEDPACK))
			{
				event.setOpcode(MenuOpcode.ITEM_USE_ON_NPC.getId());
			}
		}
		else if (opcode == MenuOpcode.ITEM_USE.getId() && event.getOption().equals("Make fertilizer"))
		{
			if (updateSelectedItem(ItemID.COMPOST))
			{
				event.setOpcode(MenuOpcode.ITEM_USE_ON_WIDGET_ITEM.getId());
			}
		}
		else if (opcode == MenuOpcode.ITEM_USE.getId() && event.getOption().equals("Mix dynamite"))
		{
			if (updateSelectedItem(ItemID.VOLCANIC_SULPHUR))
			{
				event.setOpcode(MenuOpcode.ITEM_USE_ON_WIDGET_ITEM.getId());
			}
		}
		else if (opcode == MenuOpcode.ITEM_USE.getId() && event.getOption().equals("Add fuse"))
		{
			if (updateSelectedItem(ItemID.BALL_OF_WOOL))
			{
				event.setOpcode(MenuOpcode.ITEM_USE_ON_WIDGET_ITEM.getId());
			}
		}
		else if (opcode == MenuOpcode.ITEM_USE.getId() && event.getOption().equals("Light"))
		{
			if (updateSelectedItem(ItemID.TINDERBOX))
			{
				event.setOpcode(MenuOpcode.ITEM_USE_ON_WIDGET_ITEM.getId());
			}
		}
		else if (opcode == MenuOpcode.ITEM_USE.getId() && event.getOption().equals("Add feather"))
		{
			if (updateSelectedItem(ItemID.FEATHER))
			{
				event.setOpcode(MenuOpcode.ITEM_USE_ON_WIDGET_ITEM.getId());
			}
		}
		else if (opcode == MenuOpcode.ITEM_USE.getId() && event.getOption().equals("Chisel"))
		{
			if (updateSelectedItem(ItemID.DARK_ESSENCE_BLOCK))
			{
				event.setOpcode(MenuOpcode.ITEM_USE_ON_WIDGET_ITEM.getId());
			}
		}
		else if (opcode == MenuOpcode.ITEM_USE.getId() && event.getOption().equals("Attach feather"))
		{
			if (updateSelectedItem(ItemID.FEATHER))
			{
				event.setOpcode(MenuOpcode.ITEM_USE_ON_WIDGET_ITEM.getId());
			}
		}
		else if (opcode == MenuOpcode.ITEM_USE.getId() && event.getOption().equals("Finish"))
		{
			if (updateSelectedItem(ARROW_TIPS))
			{
				event.setOpcode(MenuOpcode.ITEM_USE_ON_WIDGET_ITEM.getId());
			}
		}
		else if (opcode == MenuOpcode.EXAMINE_OBJECT.getId() && event.getOption().equals("Fill bowl"))
		{
			if (updateSelectedItem(ItemID.BOWL))
			{
				event.setOpcode(MenuOpcode.ITEM_USE_ON_GAME_OBJECT.getId());
			}
		}
		else if (opcode == MenuOpcode.ITEM_USE.getId() && event.getOption().equals("Make dough"))
		{
			if (updateSelectedItem(ItemID.SERVERY_FLOUR))
			{
				event.setOpcode(MenuOpcode.ITEM_USE_ON_WIDGET_ITEM.getId());
			}
		}
		else if (opcode == MenuOpcode.ITEM_USE.getId() && event.getOption().equals("Add sauce"))
		{
			if (updateSelectedItem(ItemID.SERVERY_TOMATO))
			{
				event.setOpcode(MenuOpcode.ITEM_USE_ON_WIDGET_ITEM.getId());
			}
		}
		else if (opcode == MenuOpcode.ITEM_USE.getId() && event.getOption().equals("Add cheese"))
		{
			if (updateSelectedItem(ItemID.SERVERY_CHEESE))
			{
				event.setOpcode(MenuOpcode.ITEM_USE_ON_WIDGET_ITEM.getId());
			}
		}
		else if (opcode == MenuOpcode.ITEM_USE.getId() && event.getOption().equals("Slice"))
		{
			if (updateSelectedItem(ItemID.KNIFE))
			{
				event.setOpcode(MenuOpcode.ITEM_USE_ON_WIDGET_ITEM.getId());
			}
		}

		else if (opcode == MenuOpcode.EXAMINE_OBJECT.getId() && event.getOption().equals("Plant seed"))
		{
			if (updateSelectedItem(TITHE_SEEDS))
			{
				event.setOpcode(MenuOpcode.ITEM_USE_ON_GAME_OBJECT.getId());
			}
		}
		else if (opcode == MenuOpcode.ITEM_USE.getId() && event.getOption().equals("Fletch"))
		{
			if (updateSelectedItem(ItemID.KNIFE))
			{
				event.setOpcode(MenuOpcode.ITEM_USE_ON_WIDGET_ITEM.getId());
			}
		}
		else if (opcode == MenuOpcode.ITEM_USE.getId() && event.getOption().equals("Add herb"))
		{
			if (updateSelectedItem(POTION_VIAL))
			{
				event.setOpcode(MenuOpcode.ITEM_USE_ON_WIDGET_ITEM.getId());
			}
		}
		else if (opcode == MenuOpcode.ITEM_USE.getId() && event.getOption().equals("Make potion"))
		{
			if (updateSelectedItem(POTION_INGREDIENT))
			{
				event.setOpcode(MenuOpcode.ITEM_USE_ON_WIDGET_ITEM.getId());
			}
		}
		else if (opcode == MenuOpcode.ITEM_USE.getId() && event.getOption().equals("Upgrade potion"))
		{
			if (updateSelectedItem(UPGRADABLE_POTIONS))
			{
				event.setOpcode(MenuOpcode.ITEM_USE_ON_WIDGET_ITEM.getId());
			}
		}

		switch (type)
		{
			case BIRDHOUSES:
				if (opcode == MenuOpcode.ITEM_USE_ON_GAME_OBJECT.getId() && target.contains("<col=ff9040>Hops seed<col=ffffff> -> "))
				{
					updateSelectedItem(HOPS_SEED);
				}
				break;
			case HERB_TAR:
				if (opcode == MenuOpcode.ITEM_USE.getId() && target.contains("<col=ff9040>Swamp tar<col=ffffff> -> "))
				{
					if (updateSelectedItem(ItemID.SWAMP_TAR))
					{
						event.setOpcode(MenuOpcode.ITEM_USE_ON_WIDGET_ITEM.getId());
					}
				}
				break;
			case LAVA_RUNES:
				if (opcode == MenuOpcode.GAME_OBJECT_FIRST_OPTION.getId() &&
					target.equals("<col=ff9040>Earth rune<col=ffffff> -> <col=ffff>Altar"))
				{
					if (updateSelectedItem(ItemID.EARTH_RUNE))
					{
						event.setOpcode(MenuOpcode.ITEM_USE_ON_GAME_OBJECT.getId());
					}
				}
				else if (opcode == MenuOpcode.GAME_OBJECT_FIRST_OPTION.getId() &&
					target.equals("<col=ff9040>Magic Imbue<col=ffffff> -> <col=ffff>Yourself"))
				{
					event.setIdentifier(1);
					event.setOpcode(MenuOpcode.WIDGET_DEFAULT.getId());
					event.setParam0(-1);
					event.setParam1(WidgetInfo.SPELL_MAGIC_IMBUE.getId());
				}
				break;
			case HIGH_ALCH:
				if (opcode == MenuOpcode.WIDGET_TYPE_2.getId() && event.getOption().equals("Cast") && target.contains("<col=00ff00>High Level Alchemy</col><col=ffffff> -> "))
				{
					final Pair<Integer, Integer> pair = findItem(alchItem.getId());
					if (pair.getLeft() != -1)
					{
						event.setOpcode(MenuOpcode.ITEM_USE_ON_WIDGET.getId());
						event.setIdentifier(pair.getLeft());
						event.setParam0(pair.getRight());
						event.setParam1(WidgetInfo.INVENTORY.getId());
						client.setSelectedSpellName("<col=00ff00>High Level Alchemy</col><col=ffffff>");
						client.setSelectedSpellWidget(WidgetInfo.SPELL_HIGH_LEVEL_ALCHEMY.getId());
					}
				}
				else if (opcode == MenuOpcode.RUNELITE.getId() && event.getIdentifier() == -1)
				{
					alchItem = null;
				}
				else if (type == Types.HIGH_ALCH && opcode == MenuOpcode.RUNELITE.getId())
				{
					final String itemName = event.getTarget().split("<col=00ff00>High Alchemy Item <col=ffffff> -> ")[1];
					alchItem = new AlchItem(itemName, event.getIdentifier());
				}
				break;
			case DWARF_CANNON:
				if (cannonFiring && event.getIdentifier() == DWARF_MULTICANNON && opcode == MenuOpcode.GAME_OBJECT_FIRST_OPTION.getId())
				{
					if (updateSelectedItem(ItemID.CANNON_BALL))
					{
						event.setOpcode(MenuOpcode.ITEM_USE_ON_GAME_OBJECT.getId());
					}
				}
				break;
			case BONES:
				if (opcode == MenuOpcode.GAME_OBJECT_FIRST_OPTION.getId() &&
					event.getTarget().contains("<col=ff9040>Bones<col=ffffff> -> ") && target.toLowerCase().contains("altar"))
				{
					if (updateSelectedItem(BONE_SET))
					{
						event.setOpcode(MenuOpcode.ITEM_USE_ON_GAME_OBJECT.getId());
					}
				}
				break;
			case KARAMBWANS:
				if (opcode == MenuOpcode.GAME_OBJECT_FIRST_OPTION.getId() && event.getTarget().contains("<col=ff9040>Raw karambwan<col=ffffff> -> "))
				{
					if (updateSelectedItem(ItemID.RAW_KARAMBWAN))
					{
						event.setOpcode(MenuOpcode.ITEM_USE_ON_GAME_OBJECT.getId());
						tick = true;
					}
				}
				break;
		}
	}

	private boolean updateSelectedItem(int id)
	{
		final Pair<Integer, Integer> pair = findItem(id);
		if (pair.getLeft() != -1)
		{
			client.setSelectedItemWidget(WidgetInfo.INVENTORY.getId());
			client.setSelectedItemSlot(pair.getRight());
			client.setSelectedItemID(pair.getLeft());
			return true;
		}
		return false;
	}

	private boolean updateSelectedItem(Collection<Integer> ids)
	{
		final Pair<Integer, Integer> pair = findItem(ids);
		if (pair.getLeft() != -1)
		{
			client.setSelectedItemWidget(WidgetInfo.INVENTORY.getId());
			client.setSelectedItemSlot(pair.getRight());
			client.setSelectedItemID(pair.getLeft());
			return true;
		}
		return false;
	}

	private Pair<Integer, Integer> findItem(int id)
	{
		final Widget inventoryWidget = client.getWidget(WidgetInfo.INVENTORY);
		final List<WidgetItem> itemList = (List<WidgetItem>) inventoryWidget.getWidgetItems();

		for (int i = itemList.size() - 1; i >= 0; i--)
		{
			final WidgetItem item = itemList.get(i);
			if (item.getId() == id)
			{
				return Pair.of(item.getId(), item.getIndex());
			}
		}

		return Pair.of(-1, -1);
	}

	private Pair<Integer, Integer> findItem(Collection<Integer> ids)
	{
		final Widget inventoryWidget = client.getWidget(WidgetInfo.INVENTORY);
		final List<WidgetItem> itemList = (List<WidgetItem>) inventoryWidget.getWidgetItems();

		for (int i = itemList.size() - 1; i >= 0; i--)
		{
			final WidgetItem item = itemList.get(i);
			if (ids.contains(item.getId()))
			{
				return Pair.of(item.getId(), item.getIndex());
			}
		}

		return Pair.of(-1, -1);
	}
}