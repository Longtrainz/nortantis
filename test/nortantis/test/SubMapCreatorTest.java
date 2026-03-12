package nortantis.test;

import nortantis.MapCreator;
import nortantis.MapSettings;
import nortantis.River;
import nortantis.SubMapCreator;
import nortantis.geom.Rectangle;
import nortantis.swing.SubMapDialog;
import nortantis.graph.voronoi.Corner;
import nortantis.graph.voronoi.Edge;
import nortantis.platform.Image;
import nortantis.platform.ImageHelper;
import nortantis.platform.PlatformFactory;
import nortantis.platform.awt.AwtFactory;
import nortantis.WorldGraph;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;

import static org.junit.jupiter.api.Assertions.*;

public class SubMapCreatorTest
{
	static final String failedMapsFolderName = "failed sub-maps";

	@BeforeAll
	public static void setUpBeforeClass() throws IOException
	{
		PlatformFactory.setInstance(new AwtFactory());
		nortantis.swing.translation.Translation.initialize();
		FileUtils.deleteDirectory(new File(Paths.get("unit test files", failedMapsFolderName).toString()));
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

		assertEquals(2, rivers.size(), "Sub-map should contain exactly 2 rivers (expected a main river and a tributary)");

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
			File failedMapsDir = Paths.get("unit test files", failedMapsFolderName).toFile();
			failedMapsDir.mkdirs();
			String failedMapPath = Paths.get("unit test files", failedMapsFolderName, "subMapRiversFormConfluence.png").toString();
			Image map = new MapCreator().createMap(subMapSettings, null, null);
			ImageHelper.getInstance().write(map, failedMapPath);
			fail("The rivers in the sub-map should share a common corner at their confluence.\nFailed map written to: "
					+ failedMapPath);
		}
	}

	/**
	 * Verifies that rivers in a sub-map contain no finger branches — degree-1 interior corners that are not river endpoints. The original map has no fingers, so the sub-map should not introduce
	 * any.
	 */
	@Test
	public void subMapRiversHaveNoFingers() throws Exception
	{
		String originalSettingsPath = Paths.get("unit test files", "map settings", "riverConfluence.nort").toString();
		MapSettings originalSettings = new MapSettings(originalSettingsPath);
		originalSettings.resolution = 0.5;

		WorldGraph originalGraph = MapCreator.createGraphForUnitTests(originalSettings);

		// Sub-map selection bounds in RI (resolution-invariant) coordinates.
		Rectangle selectionBoundsRI = new Rectangle(0, 0, 1348, 4096);

		int worldSize = SubMapDialog.computeDefaultWorldSize(originalSettings, selectionBoundsRI);

		long seed = 983909832L;
		MapSettings subMapSettings = SubMapCreator.createSubMapSettings(originalSettings, originalGraph, selectionBoundsRI, worldSize,
				originalSettings.resolution, seed, true);

		WorldGraph newGraph = MapCreator.createGraphForUnitTests(subMapSettings);

		List<River> rivers = newGraph.findRivers();

		for (River river : rivers)
		{
			// Build a degree map: count how many edges in this river touch each corner.
			Map<Corner, Integer> cornerDegree = new HashMap<>();
			for (Edge e : river.getEdges())
			{
				if (e.v0 != null)
					cornerDegree.merge(e.v0, 1, Integer::sum);
				if (e.v1 != null)
					cornerDegree.merge(e.v1, 1, Integer::sum);
			}

			// The start and end corners are valid degree-1 endpoints; any other degree-1 corner is a finger.
			List<Corner> orderedCorners = river.getOrderedCorners();
			Set<Corner> endpoints = new HashSet<>();
			if (!orderedCorners.isEmpty())
			{
				endpoints.add(orderedCorners.get(0));
				endpoints.add(orderedCorners.get(orderedCorners.size() - 1));
			}

			for (Map.Entry<Corner, Integer> entry : cornerDegree.entrySet())
			{
				if (entry.getValue() == 1 && !endpoints.contains(entry.getKey()))
				{
					File failedMapsDir = Paths.get("unit test files", failedMapsFolderName).toFile();
					failedMapsDir.mkdirs();
					String failedMapPath = Paths.get("unit test files", failedMapsFolderName, "subMapRiversHaveNoFingers.png").toString();
					Image map = new MapCreator().createMap(subMapSettings, null, null);
					ImageHelper.getInstance().write(map, failedMapPath);
					fail("River has a finger at corner " + entry.getKey().index + ".\nFailed map written to: " + failedMapPath);
				}
			}
		}

	}

	/**
	 * Verifies that rivers in a sub-map contain no loops. A loop exists when the river's edges form a cycle, detected by checking that the edge count exceeds corner count minus one (the invariant
	 * for a simple path).
	 */
	@Test
	public void subMapRiversHaveNoLoops() throws Exception
	{
		String originalSettingsPath = Paths.get("unit test files", "map settings", "riverConfluence.nort").toString();
		MapSettings originalSettings = new MapSettings(originalSettingsPath);
		originalSettings.resolution = 0.5;

		WorldGraph originalGraph = MapCreator.createGraphForUnitTests(originalSettings);

		// Sub-map selection bounds in RI (resolution-invariant) coordinates.
		Rectangle selectionBoundsRI = new Rectangle(0, 0, 1348, 4096);

		int worldSize = SubMapDialog.computeDefaultWorldSize(originalSettings, selectionBoundsRI);

		long seed = 1142346135L;
		MapSettings subMapSettings = SubMapCreator.createSubMapSettings(originalSettings, originalGraph, selectionBoundsRI, worldSize,
				originalSettings.resolution, seed, true);

		WorldGraph newGraph = MapCreator.createGraphForUnitTests(subMapSettings);

		List<River> rivers = newGraph.findRivers();

		for (River river : rivers)
		{
			int edgeCount = river.getEdges().size();
			int cornerCount = river.getCorners().size();
			if (edgeCount > cornerCount - 1)
			{
				File failedMapsDir = Paths.get("unit test files", failedMapsFolderName).toFile();
				failedMapsDir.mkdirs();
				String failedMapPath = Paths.get("unit test files", failedMapsFolderName, "subMapRiversHaveNoLoops.png").toString();
				Image map = new MapCreator().createMap(subMapSettings, null, null);
				ImageHelper.getInstance().write(map, failedMapPath);
				fail("River has a loop: " + edgeCount + " edges but only " + cornerCount + " corners.\nFailed map written to: "
						+ failedMapPath);
			}
		}
	}
}
