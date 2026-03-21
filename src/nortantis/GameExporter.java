package nortantis;

import nortantis.editor.Road;
import nortantis.geom.Point;
import nortantis.geom.Rectangle;
import nortantis.graph.voronoi.Center;
import nortantis.graph.voronoi.Edge;
import nortantis.platform.Color;
import nortantis.platform.Image;
import nortantis.platform.ImageType;
import nortantis.platform.Painter;
import nortantis.util.FileHelper;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.*;
import java.util.stream.Collectors;

public class GameExporter
{
	private final WorldGraph graph;
	private final MapSettings settings;

	/**
	 * The actual pixel dimensions of the graph content area.
	 */
	private final int graphWidth;
	private final int graphHeight;

	public GameExporter(WorldGraph graph, MapSettings settings)
	{
		this.graph = graph;
		this.settings = settings;

		Rectangle bounds = graph.bounds;
		this.graphWidth = (int) Math.ceil(bounds.width);
		this.graphHeight = (int) Math.ceil(bounds.height);
	}

	@SuppressWarnings("unchecked")
	public void exportAll(String folderPath, Image mapImage)
	{
		FileHelper.createFolder(folderPath);

		int mapImageWidth = mapImage != null ? mapImage.getWidth() : graphWidth;
		int mapImageHeight = mapImage != null ? mapImage.getHeight() : graphHeight;

		String json = buildWorldJson(mapImageWidth, mapImageHeight).toJSONString();
		FileHelper.writeToFile(folderPath + "/world.json", json);
		exportRegionMask(folderPath + "/region_mask.png", mapImageWidth, mapImageHeight);

		if (mapImage != null)
		{
			mapImage.write(folderPath + "/map.png");
		}
	}

	@SuppressWarnings("unchecked")
	JSONObject buildWorldJson(int mapImageWidth, int mapImageHeight)
	{
		JSONObject root = new JSONObject();
		root.put("metadata", buildMetadata(mapImageWidth, mapImageHeight));
		root.put("regions", buildRegions());
		root.put("cities", buildCities());
		root.put("roads", buildRoads());
		root.put("rivers", buildRivers());
		return root;
	}

	@SuppressWarnings("unchecked")
	private JSONObject buildMetadata(int mapImageWidth, int mapImageHeight)
	{
		JSONObject meta = new JSONObject();
		meta.put("seed", settings.randomSeed);
		meta.put("worldSize", settings.worldSize);
		meta.put("graphWidth", graphWidth);
		meta.put("graphHeight", graphHeight);
		meta.put("mapImageWidth", mapImageWidth);
		meta.put("mapImageHeight", mapImageHeight);

		int borderX = (mapImageWidth - graphWidth) / 2;
		int borderY = (mapImageHeight - graphHeight) / 2;
		meta.put("borderOffsetX", borderX);
		meta.put("borderOffsetY", borderY);

		meta.put("worldTitle", collectWorldTitle());
		return meta;
	}

	private String collectWorldTitle()
	{
		if (settings.edits != null && settings.edits.text != null)
		{
			for (MapText text : settings.edits.text)
			{
				if (text.type == TextType.Title && text.value != null)
				{
					return text.value;
				}
			}
		}
		return "";
	}

	@SuppressWarnings("unchecked")
	private JSONArray buildRegions()
	{
		Map<Integer, Set<Integer>> regionNeighbors = computeRegionNeighbors();
		Map<Integer, String> dominantTerrains = computeDominantTerrains();
		Map<Integer, String> regionNames = collectRegionNames();

		JSONArray regionsArr = new JSONArray();
		for (Region region : graph.regions.values())
		{
			JSONObject rObj = new JSONObject();
			rObj.put("id", region.id);
			rObj.put("name", regionNames.getOrDefault(region.id, "Region " + region.id));
			rObj.put("color", colorToHex(region.backgroundColor));

			Point centroid = computeRegionCentroid(region);
			rObj.put("center", pointToJson(centroid));

			Set<Integer> neighbors = regionNeighbors.getOrDefault(region.id, Collections.emptySet());
			JSONArray neighborsArr = new JSONArray();
			neighborsArr.addAll(neighbors);
			rObj.put("neighborRegionIds", neighborsArr);

			rObj.put("terrain", dominantTerrains.getOrDefault(region.id, "UNKNOWN"));

			JSONArray polygonArr = new JSONArray();
			for (Center center : region.getCenters())
			{
				JSONObject cObj = new JSONObject();
				cObj.put("centerIndex", center.index);
				JSONArray cornersArr = new JSONArray();
				List<Edge> orderedEdges = center.orderEdgesAroundCenter();
				for (Edge edge : orderedEdges)
				{
					if (edge.v0 != null)
					{
						cornersArr.add(pointToJson(edge.v0.loc));
					}
					if (edge.v1 != null)
					{
						cornersArr.add(pointToJson(edge.v1.loc));
					}
				}
				cObj.put("corners", cornersArr);
				cObj.put("loc", pointToJson(center.loc));
				polygonArr.add(cObj);
			}
			rObj.put("cells", polygonArr);

			regionsArr.add(rObj);
		}
		return regionsArr;
	}

	@SuppressWarnings("unchecked")
	private JSONArray buildCities()
	{
		Map<Integer, String> cityNames = collectCityNames();

		JSONArray citiesArr = new JSONArray();
		int cityId = 0;
		for (Center center : graph.centers)
		{
			if (!center.isCity)
			{
				continue;
			}

			JSONObject cObj = new JSONObject();
			cObj.put("id", cityId);
			cObj.put("centerIndex", center.index);
			cObj.put("x", center.loc.x);
			cObj.put("y", center.loc.y);
			cObj.put("regionId", center.region != null ? center.region.id : -1);
			cObj.put("name", cityNames.getOrDefault(center.index, "City " + cityId));
			cityId++;
			citiesArr.add(cObj);
		}
		return citiesArr;
	}

	@SuppressWarnings("unchecked")
	private JSONArray buildRoads()
	{
		JSONArray roadsArr = new JSONArray();
		if (settings.edits == null || settings.edits.roads == null)
		{
			return roadsArr;
		}

		for (Road road : settings.edits.roads)
		{
			if (road.path == null || road.path.size() < 2)
			{
				continue;
			}
			JSONObject rObj = new JSONObject();
			JSONArray pathArr = new JSONArray();
			for (Point p : road.path)
			{
				pathArr.add(pointToJson(p));
			}
			rObj.put("path", pathArr);
			roadsArr.add(rObj);
		}
		return roadsArr;
	}

	@SuppressWarnings("unchecked")
	private JSONArray buildRivers()
	{
		JSONArray riversArr = new JSONArray();
		for (Edge edge : graph.edges)
		{
			if (!edge.isRiver() || edge.isOceanOrLakeOrShore())
			{
				continue;
			}
			if (edge.v0 == null || edge.v1 == null)
			{
				continue;
			}
			JSONObject rObj = new JSONObject();
			rObj.put("level", edge.river);
			rObj.put("from", pointToJson(edge.v0.loc));
			rObj.put("to", pointToJson(edge.v1.loc));
			riversArr.add(rObj);
		}
		return riversArr;
	}

	private Map<Integer, Set<Integer>> computeRegionNeighbors()
	{
		Map<Integer, Set<Integer>> result = new HashMap<>();
		for (Edge edge : graph.edges)
		{
			if (edge.d0 == null || edge.d1 == null)
			{
				continue;
			}
			Region r0 = edge.d0.region;
			Region r1 = edge.d1.region;
			if (r0 != null && r1 != null && r0 != r1)
			{
				result.computeIfAbsent(r0.id, k -> new TreeSet<>()).add(r1.id);
				result.computeIfAbsent(r1.id, k -> new TreeSet<>()).add(r0.id);
			}
		}
		return result;
	}

	private Map<Integer, String> computeDominantTerrains()
	{
		Map<Integer, String> result = new HashMap<>();
		for (Region region : graph.regions.values())
		{
			Map<Biome, Integer> counts = new EnumMap<>(Biome.class);
			for (Center c : region.getCenters())
			{
				if (c.biome != null)
				{
					counts.merge(c.biome, 1, Integer::sum);
				}
			}
			Biome dominant = counts.entrySet().stream()
					.max(Map.Entry.comparingByValue())
					.map(Map.Entry::getKey)
					.orElse(null);
			result.put(region.id, dominant != null ? dominant.name() : "UNKNOWN");
		}
		return result;
	}

	/**
	 * Matches city text labels to the nearest center that is a city,
	 * then generates fantasy names for any unmatched cities via NameCreator.
	 */
	private Map<Integer, String> collectCityNames()
	{
		Map<Integer, String> result = new HashMap<>();

		List<Center> cityCenters = graph.centers.stream().filter(c -> c.isCity).collect(Collectors.toList());
		if (cityCenters.isEmpty())
		{
			return result;
		}

		double actualScale = settings.generatedWidth > 0
				? graph.bounds.width / settings.generatedWidth
				: 1.0;

		if (settings.edits != null && settings.edits.text != null)
		{
			Set<Integer> claimedCenters = new HashSet<>();
			List<MapText> cityTexts = settings.edits.text.stream()
					.filter(t -> t.type == TextType.City && t.location != null && t.value != null)
					.collect(Collectors.toList());

			for (MapText text : cityTexts)
			{
				Center closest = null;
				double minDist = Double.MAX_VALUE;
				for (Center city : cityCenters)
				{
					if (claimedCenters.contains(city.index))
					{
						continue;
					}
					Point cityInvariant = city.loc.mult(1.0 / actualScale);
					double dist = cityInvariant.distanceTo(text.location);
					if (dist < minDist)
					{
						minDist = dist;
						closest = city;
					}
				}
				if (closest != null)
				{
					result.put(closest.index, text.value);
					claimedCenters.add(closest.index);
				}
			}
		}

		List<Center> unnamed = cityCenters.stream()
				.filter(c -> !result.containsKey(c.index))
				.collect(Collectors.toList());
		if (!unnamed.isEmpty())
		{
			try
			{
				NameCreator fallback = new NameCreator(settings);
				Random rng = new Random(settings.textRandomSeed + 7);
				CityType[] cityTypes = CityType.values();
				for (Center city : unnamed)
				{
					try
					{
						CityType ct = cityTypes[rng.nextInt(cityTypes.length)];
						String name = fallback.generateNameOfType(TextType.City, ct, true);
						result.put(city.index, name);
					}
					catch (Exception ignored)
					{
					}
				}
			}
			catch (Exception ignored)
			{
			}
		}

		return result;
	}

	/**
	 * Matches region text labels to regions by proximity to region centroid,
	 * then generates fantasy names for any unmatched regions via NameCreator.
	 * Title text (TextType.Title) is excluded — it represents the world name, not a region.
	 */
	private Map<Integer, String> collectRegionNames()
	{
		Map<Integer, String> result = new HashMap<>();

		double actualScale = settings.generatedWidth > 0
				? graph.bounds.width / settings.generatedWidth
				: 1.0;

		if (settings.edits != null && settings.edits.text != null)
		{
			List<MapText> regionTexts = settings.edits.text.stream()
					.filter(t -> t.type == TextType.Region
							&& t.location != null && t.value != null)
					.collect(Collectors.toList());

			Set<Integer> claimedRegions = new HashSet<>();
			for (MapText text : regionTexts)
			{
				Point textLocScaled = text.location.mult(actualScale);
				Region closest = null;
				double minDist = Double.MAX_VALUE;
				for (Region region : graph.regions.values())
				{
					if (claimedRegions.contains(region.id))
					{
						continue;
					}
					Point centroid = computeRegionCentroidRaw(region);
					double dist = centroid.distanceTo(textLocScaled);
					if (dist < minDist)
					{
						minDist = dist;
						closest = region;
					}
				}
				if (closest != null)
				{
					result.put(closest.id, text.value);
					claimedRegions.add(closest.id);
				}
			}
		}

		List<Region> unnamed = graph.regions.values().stream()
				.filter(r -> !result.containsKey(r.id))
				.collect(Collectors.toList());
		if (!unnamed.isEmpty())
		{
			try
			{
				NameCreator fallback = new NameCreator(settings);
				for (Region region : unnamed)
				{
					try
					{
						String name = fallback.generateNameOfType(TextType.Region, null, true);
						result.put(region.id, name);
					}
					catch (Exception ignored)
					{
					}
				}
			}
			catch (Exception ignored)
			{
			}
		}

		return result;
	}

	private Point computeRegionCentroidRaw(Region region)
	{
		double sumX = 0, sumY = 0;
		int count = 0;
		for (Center c : region.getCenters())
		{
			sumX += c.loc.x;
			sumY += c.loc.y;
			count++;
		}
		return count > 0 ? new Point(sumX / count, sumY / count) : new Point(0, 0);
	}

	/**
	 * Creates the region mask at the same pixel dimensions as the map image.
	 * The graph content is centered within the image using the border offset.
	 */
	void exportRegionMask(String filePath, int mapImageWidth, int mapImageHeight)
	{
		int borderX = (mapImageWidth - graphWidth) / 2;
		int borderY = (mapImageHeight - graphHeight) / 2;

		Image mask = Image.create(mapImageWidth, mapImageHeight, ImageType.RGB);
		try (Painter p = mask.createPainter())
		{
			p.setColor(Color.create(0, 0, 0));
			p.fillRect(0, 0, mapImageWidth, mapImageHeight);
			p.translate(borderX, borderY);

			graph.drawPolygons(p, graph.centers, center ->
			{
				if (center.region == null || center.isWater)
				{
					return Color.create(0, 0, 0);
				}
				int regionId = center.region.id;
				int r = (regionId >> 8) & 0xFF;
				int g = regionId & 0xFF;
				return Color.create(r, g, 1);
			});
		}

		mask.write(filePath);
	}

	@SuppressWarnings("unchecked")
	private static JSONObject pointToJson(Point p)
	{
		JSONObject obj = new JSONObject();
		obj.put("x", Math.round(p.x * 100.0) / 100.0);
		obj.put("y", Math.round(p.y * 100.0) / 100.0);
		return obj;
	}

	private static String colorToHex(Color color)
	{
		if (color == null)
		{
			return "#808080";
		}
		return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
	}

	private Point computeRegionCentroid(Region region)
	{
		double sumX = 0, sumY = 0;
		int count = 0;
		for (Center c : region.getCenters())
		{
			sumX += c.loc.x;
			sumY += c.loc.y;
			count++;
		}
		if (count == 0)
		{
			return new Point(0, 0);
		}
		return new Point(sumX / count, sumY / count);
	}
}
