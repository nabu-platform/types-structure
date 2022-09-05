package be.nabu.libs.types.structure;

import java.util.Set;

import be.nabu.libs.property.api.Property;
import be.nabu.libs.property.api.Value;
import be.nabu.libs.types.api.DefinedType;
import be.nabu.libs.types.properties.IdProperty;

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

	@Override
	public Set<Property<?>> getSupportedProperties(Value<?>...values) {
		Set<Property<?>> properties = super.getSupportedProperties(values);
		properties.add(IdProperty.getInstance());
		return properties;
	}

}
