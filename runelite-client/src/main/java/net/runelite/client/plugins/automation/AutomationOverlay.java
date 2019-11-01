package net.runelite.client.plugins.automation;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Shape;
import static java.lang.Math.floor;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;
import net.runelite.client.ui.overlay.OverlayUtil;

class AutomationOverlay extends Overlay
{
	private static final Color TRANSPARENT = new Color(0, 0, 0, 0);

	private final Client client;
	private final AutomationPlugin plugin;
	private final AutomationConfig config;

	@Inject
	private AutomationOverlay(final Client client, final AutomationPlugin plugin, final AutomationConfig config)
	{
		this.client = client;
		this.plugin = plugin;
		this.config = config;
		setPosition(OverlayPosition.DYNAMIC);
		setPriority(OverlayPriority.LOW);
		setLayer(OverlayLayer.ABOVE_SCENE);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		for (GameObject object : plugin.markedObjects)
		{
			if (object.getPlane() != client.getPlane())
			{
				continue;
			}

			Color color = config.objectMarkerColor();
			int opacity = (int) floor(config.objectMarkerAlpha() * 2.55);
			Color objectColor = new Color(color.getRed(), color.getGreen(), color.getBlue(), opacity);

			Shape clickbox = object.getClickbox();
			if (clickbox != null)
			{
				OverlayUtil.renderHoverableArea(graphics, object.getClickbox(), client.getMouseCanvasPosition(), TRANSPARENT, objectColor, objectColor.darker());
			}
		}

		return null;
	}
}
