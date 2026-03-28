package nortantis;

/**
 * Identifies an icon group within a specific art pack, used for multi-source icon generation.
 */
public class IconSource
{
	public final String artPack;
	public final String groupId;

	public IconSource(String artPack, String groupId)
	{
		this.artPack = artPack;
		this.groupId = groupId;
	}
}
