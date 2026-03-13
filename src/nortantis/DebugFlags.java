package nortantis;

import nortantis.util.Assets;

public class DebugFlags
{
	/**
	 * Causes the replacement draw bounds for incremental updates to be drawn onto the map.
	 */
	private static boolean showIncrementalUpdateBounds = false;

	/**
	 * Prints how long incremental updates take.
	 */
	private static boolean printIncrementalUpdateTimes = false;

	/**
	 * Causes the indexes of edges to be printed to standard out when adding rivers in the Land and Water tool. This is useful when you're
	 * debugging a need to find the index of an edge for setting a conditional breakpoint.
	 */
	private static boolean printRiverEdgeIndexes = false;

	/**
	 * Causes the indexes of centers to be printed when hovering over them in the Land and Water tool. This is useful when you're debugging
	 * a need to find the index of a center for setting a conditional breakpoint.
	 */
	private static boolean printCenterIndexes = false;

	private static boolean printIconsBeingEdited = false;

	private static boolean writeBeforeAndAfterJsonWhenSavePromptShows = false;

	private static int[] indexesOfCentersToHighlight = new int[] {};

	private static int[] indexesOfEdgesToHighlight = new int[] {};

	private static int[] indexesOfCornersToHighlight = new int[] {374, 378, 378, 399, 399, 390, 390, 360, 360, 325, 325, 329, 329, 394, 394, 343, 343, 393, 393, 339, 339, 350, 350, 391, 391, 418, 418, 389, 389, 404, 404, 417, 417, 466, 466, 546, 546, 577, 577, 586, 586, 626, 626, 680, 680, 683, 683, 713, 713, 782, 782, 783, 783, 837, 837, 868, 868, 916, 916, 935, 935, 984, 984, 914, 914, 926, 926, 950, 950, 956, 956, 971, 971, 1047, 1047, 1012, 1012, 1033, 1033, 953, 953, 1029, 1029, 1120, 1120, 1133, 1133, 1190, 1190, 1178, 1178, 1260, 1260, 1303, 1303, 1327, 1327, 1322, 1322, 1362, 1362, 1334, 1334, 1300, 1300, 1289, 1289, 1272, 1272, 1276, 1276, 1254, 1254, 1265, 1265, 1245, 1245, 1269, 1269, 1313, 1313, 1342, 1342, 1457, 1457, 1404, 1404, 1406, 1406, 1470, 1470, 1532, 1532, 1539, 1539, 1566, 1566, 1615, 1615, 1613, 1613, 1588, 1588, 1666, 1666, 1677, 1677, 1698, 1698, 1783, 1783, 1751, 1751, 1709, 1709, 1799, 1799, 1830, 1830, 1782, 1782, 1818, 1818, 1907, 1907, 1867, 1867, 1872, 1872, 1814, 1814, 1793, 1793, 1771, 1771, 1717, 1717, 1740, 1740, 1700, 1700, 1639, 1639, 1658, 1658, 1591, 1591, 1589, 1589, 1586, 1586, 1585, 1585, 1493, 1493, 1453, 1453, 1360, 1360, 1403, 1403, 1353, 1353, 1385, 1385, 1342};

	private static boolean drawRegionBoundaryPathJoins = false;

	private static boolean drawCorners = false;

	private static boolean drawVoronoi = false;

	private static boolean drawRoadDebugInfo = false;

	public static boolean showIncrementalUpdateBounds()
	{
		return !Assets.isRunningFromJar() && showIncrementalUpdateBounds;
	}

	public static boolean printIncrementalUpdateTimes()
	{
		return !Assets.isRunningFromJar() && printIncrementalUpdateTimes;
	}

	public static boolean printRiverEdgeIndexes()
	{
		return !Assets.isRunningFromJar() && printRiverEdgeIndexes;
	}

	public static boolean printCenterIndexes()
	{
		return !Assets.isRunningFromJar() && printCenterIndexes;
	}

	public static int[] getIndexesOfCentersToHighlight()
	{
		if (Assets.isRunningFromJar())
		{
			return new int[] {};
		}
		return indexesOfCentersToHighlight;
	}

	public static int[] getIndexesOfEdgesToHighlight()
	{
		if (Assets.isRunningFromJar())
		{
			return new int[] {};
		}
		return indexesOfEdgesToHighlight;
	}

	public static int[] getIndexesOfCornersToHighlight()
	{
		if (Assets.isRunningFromJar())
		{
			return new int[] {};
		}
		return indexesOfCornersToHighlight;
	}

	public static boolean shouldWriteBeforeAndAfterJsonWhenSavePromptShows()
	{
		return !Assets.isRunningFromJar() && writeBeforeAndAfterJsonWhenSavePromptShows;
	}

	public static boolean printIconsBeingEdited()
	{
		return !Assets.isRunningFromJar() && printIconsBeingEdited;
	}

	public static boolean drawRegionBoundaryPathJoins()
	{
		return !Assets.isRunningFromJar() && drawRegionBoundaryPathJoins;
	}

	public static boolean drawCorners()
	{
		return !Assets.isRunningFromJar() && drawCorners;
	}

	public static boolean drawVoronoi()
	{
		return !Assets.isRunningFromJar() && drawVoronoi;
	}

	public static boolean drawRoadDebugInfo()
	{
		return !Assets.isRunningFromJar() && drawRoadDebugInfo;
	}
}
