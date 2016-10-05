package be.nabu.libs.types.structure;

import be.nabu.libs.types.api.DefinedType;
import be.nabu.libs.types.api.SimpleType;

public class DefinedSimpleStructure<T> extends SimpleStructure<T> implements DefinedType {

	private String id;
	
	public DefinedSimpleStructure(SimpleType<T> valueField) {
		super(valueField);
	}

	@Override
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}
	

}
