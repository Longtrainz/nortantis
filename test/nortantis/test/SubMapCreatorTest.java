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

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
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
}
