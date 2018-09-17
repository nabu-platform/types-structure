package be.nabu.libs.types.structure;

import be.nabu.libs.types.api.DefinedType;

public class DefinedStructure extends Structure implements DefinedType {

	private String id;
	
	@Override
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}	
	
	@Override
	public String toString() {
		return "structure[" + getId() + "]";
	}


}
