package nortantis.test;

import nortantis.MapCreator;
import nortantis.MapSettings;
import nortantis.River;
import nortantis.SubMapCreator;
import nortantis.geom.Rectangle;
import nortantis.swing.SubMapDialog;
import nortantis.graph.voronoi.Corner;
import nortantis.platform.Image;
import nortantis.platform.ImageHelper;
import nortantis.platform.PlatformFactory;
import nortantis.platform.awt.AwtFactory;
import nortantis.WorldGraph;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class SubMapCreatorTest
{
	@BeforeAll
	public static void setUpBeforeClass()
	{
		PlatformFactory.setInstance(new AwtFactory());
		nortantis.swing.translation.Translation.initialize();
	}

	/**
	 * Verifies that when a sub-map contains two rivers that form a confluence in the source map, those rivers are still connected via a shared corner in the sub-map's edge edits. This is a
	 * regression test for a bug in SubMapCreator where the last edge of a tributary was incorrectly removed by simplifyToPath, preventing the confluence from being transferred.
	 */
	@Test
	public void subMapRiversFormConfluence() throws Exception
	{
		String originalSettingsPath = Paths.get("unit test files", "map settings", "riverConfluence.nort").toString();
		MapSettings originalSettings = new MapSettings(originalSettingsPath);
		originalSettings.resolution = 0.5;

		WorldGraph originalGraph = MapCreator.createGraphForUnitTests(originalSettings);

		// Sub-map selection bounds in RI (resolution-invariant) coordinates.
		Rectangle selectionBoundsRI = new Rectangle(1324, 999, 1307, 1307);

		int worldSize = SubMapDialog.computeDefaultWorldSize(originalSettings, selectionBoundsRI);

		long seed = 1962328436L;
		MapSettings subMapSettings = SubMapCreator.createSubMapSettings(originalSettings, originalGraph, selectionBoundsRI, worldSize,
				originalSettings.resolution, seed, true);

		// Recreate the same WorldGraph that createSubMapSettings built internally.
		WorldGraph newGraph = MapCreator.createGraphForUnitTests(subMapSettings);

		List<River> rivers = newGraph.findRivers();

		// TODO Remove this when Claude is done with it
		StringBuilder debugInfo = new StringBuilder();
		debugInfo.append("Rivers found: ").append(rivers.size()).append("\n");
		for (int i = 0; i < rivers.size(); i++)
		{
			River r = rivers.get(i);
			List<Corner> orderedCorners = r.getOrderedCorners();
			debugInfo.append("  River ").append(i).append(": ").append(r.size()).append(" edges, corners: ");
			debugInfo.append(orderedCorners.stream().map(c -> String.valueOf(c.index)).collect(java.util.stream.Collectors.joining(",")));
			debugInfo.append("\n");
		}

		assertEquals(2, rivers.size(), "Sub-map should contain exactly 2 rivers (expected a main river and a tributary)\n" + debugInfo);

		// Build corner sets for each river and check that some pair shares a corner,
		// indicating the rivers are joined at a confluence.
		boolean confluenceFound = false;
		outer:
		for (int i = 0; i < rivers.size(); i++)
		{
			Set<Corner> cornersI = rivers.get(i).getCorners();
			for (int j = i + 1; j < rivers.size(); j++)
			{
				Set<Corner> cornersJ = rivers.get(j).getCorners();
				Set<Corner> intersection = new HashSet<>(cornersI);
				intersection.retainAll(cornersJ);
				if (!intersection.isEmpty())
				{
					confluenceFound = true;
					break outer;
				}
			}
		}

		if (!confluenceFound)
		{
			// Render and save the sub-map for visual inspection.
			java.io.File failedMapsDir = Paths.get("unit test files", "failed maps").toFile();
			failedMapsDir.mkdirs();
			String failedMapPath = Paths.get("unit test files", "failed maps", "subMapRiversFormConfluence.png").toString();
			Image map = new MapCreator().createMap(subMapSettings, null, null);
			ImageHelper.getInstance().write(map, failedMapPath);
			fail("The rivers in the sub-map should share a common corner at their confluence.\nFailed map written to: " + failedMapPath + "\n" + debugInfo);
		}
	}
}
