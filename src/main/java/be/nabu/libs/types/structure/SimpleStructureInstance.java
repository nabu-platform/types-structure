package be.nabu.libs.types.structure;

import be.nabu.libs.types.api.ComplexType;

/**
 * This is much like a regular structure except it has an unnamed value field
 * Note that the value field can be get & set using the reserved "$value" name
 */
public class SimpleStructureInstance extends StructureInstance {
	
	/**
	 * The value it holds
	 */
	private Object value;

	@Override
	public Object get(String path) {
		if (path.equals(ComplexType.SIMPLE_TYPE_VALUE))
			return value;
		else
			return super.get(path);
	}

	@Override
	public void set(String path, Object value) {
		if (path.equals(ComplexType.SIMPLE_TYPE_VALUE))
			this.value = value;
		else
			super.set(path, value);
	}
	
	
}
