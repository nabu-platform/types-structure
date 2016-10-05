package be.nabu.libs.types.structure;

import be.nabu.libs.types.api.Type;
import be.nabu.libs.types.properties.SimpleProperty;

public class SuperTypeProperty extends SimpleProperty<Type> {

	private static SuperTypeProperty instance = new SuperTypeProperty();
	
	public static SuperTypeProperty getInstance() {
		return instance;
	}
	
	public SuperTypeProperty() {
		super(Type.class);
	}

	@Override
	public String getName() {
		return "superType";
	}

}
