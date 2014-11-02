
package org.jnbt;

/**
 *
 * @author ferrybig
 */
public final class IntListTag extends Tag {

	/**
	 * The value.
	 */
	private final int[] value;

	/**
	 * Creates the tag.
	 * @param name The name.
	 * @param value The value.
	 */
	public IntListTag(String name, int[] value) {
		super(name);
		this.value = value;
	}

	@Override
	public int[] getValue() {
		return value;
	}

	@Override
	public String toString() {
		StringBuilder hex = new StringBuilder();
		for(int b : value) {
			hex.append(b).append(" ");
		}
		String name = getName();
		String append = "";
		if(name != null && !name.equals("")) {
			append = "(\"" + this.getName() + "\")";
		}
		return "TAG_Byte_Array" + append + ": " + hex.toString();
	}

}
