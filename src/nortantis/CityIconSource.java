package nortantis;

/**
 * Identifies a city icon group within a specific art pack, used for multi-source city generation.
 */
public class CityIconSource
{
	public final String artPack;
	public final String groupId;

	public CityIconSource(String artPack, String groupId)
	{
		this.artPack = artPack;
		this.groupId = groupId;
	}
}
