package tollenaar.stephen.ItemSorter.Util;

import java.util.ArrayList;
import java.util.List;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

/**
 * Small text helpers, replacing the deprecated commons-lang3
 * org.apache.commons.lang3.text.WordUtils this plugin used to lean on.
 */
public final class Text {

	private Text() {
	}

	/**
	 * Title-cases every whitespace separated word and lower-cases the rest, the
	 * same way WordUtils.capitalizeFully did.
	 *
	 * @param value - the text to convert.
	 * @return The converted text, or the input when null or empty.
	 */
	public static String capitalizeFully(String value) {
		if (value == null || value.isEmpty()) {
			return value;
		}
		StringBuilder out = new StringBuilder(value.length());
		boolean startOfWord = true;
		for (char c : value.toCharArray()) {
			if (Character.isWhitespace(c)) {
				startOfWord = true;
				out.append(c);
			} else if (startOfWord) {
				out.append(Character.toTitleCase(c));
				startOfWord = false;
			} else {
				out.append(Character.toLowerCase(c));
			}
		}
		return out.toString();
	}

	/**
	 * Legacy section-sign text as a component, the conversion the deprecated
	 * ItemMeta.setLore(List<String>) and BookMeta.setPages(List<String>) did.
	 *
	 * @param value - legacy text.
	 * @return The equivalent component.
	 */
	public static Component legacy(String value) {
		return LegacyComponentSerializer.legacySection().deserialize(value);
	}

	/**
	 * A component back as legacy section-sign text, the conversion the
	 * deprecated ItemMeta.getLore() and BookMeta.getPage(int) did.
	 *
	 * @param value - the component.
	 * @return The equivalent legacy text.
	 */
	public static String legacy(Component value) {
		return LegacyComponentSerializer.legacySection().serialize(value);
	}

	/**
	 * Legacy text lines as components, for ItemMeta.lore and BookMeta.pages.
	 *
	 * @param values - legacy text lines.
	 * @return The equivalent components.
	 */
	public static List<Component> legacy(List<String> values) {
		List<Component> out = new ArrayList<>(values.size());
		for (String value : values) {
			out.add(legacy(value));
		}
		return out;
	}
}
