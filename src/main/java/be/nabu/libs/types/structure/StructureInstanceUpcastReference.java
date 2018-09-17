package be.nabu.libs.types.structure;

import java.util.List;

import be.nabu.libs.types.TypeUtils;
import be.nabu.libs.types.api.ComplexContent;
import be.nabu.libs.types.api.ComplexType;
import be.nabu.libs.types.api.Type;

/**
 * Each structure instance can only have one definition at runtime,
 * however if you cast structure of type A to a type B for your local flow,
 * the instance will be wrapped around a reference which takes the new definition
 * 
 * Any values are still written to the original instance but can only be
 * accessed by a properly cast instance.
 * 
 * @author alex
 *
 */
public class StructureInstanceUpcastReference extends StructureInstance {
	
	public static StructureInstanceUpcastReference upcast(ComplexContent instance, ComplexType cast) {
		// the type we are casting to is a base type
		if (!TypeUtils.getAllChildrenIterator(cast).hasNext()) {
			// upcast all the way to the root
			while (instance.getType().getSuperType() instanceof ComplexType)
				instance = new StructureInstanceUpcastReference(instance, (ComplexType) instance.getType().getSuperType());
			// wrap an additional empty wrapper around it
			return new StructureInstanceUpcastReference(instance, new Structure());
		}
		else {
			// an upcast path must exist
			List<Type> path = TypeUtils.getUpcastPath(instance.getType(), cast);
			if (path.isEmpty())
				return null;
			for (Type upcast : path)
				instance = new StructureInstanceUpcastReference(instance, (ComplexType) upcast);
			return (StructureInstanceUpcastReference) instance;
		}
	}
	
	private ComplexContent reference;
	
	StructureInstanceUpcastReference(ComplexContent reference, ComplexType cast) {
		this.reference = reference;
		setType(cast);
	}
	
	public ComplexContent getReference() {
		return reference;
	}

	/**
	 * Delegate to reference
	 */
	@Override
	public void set(String path, Object value) {
		reference.set(path, value);
	}

	/**
	 * Delegate to reference
	 */
	@Override
	public Object get(String path) {
		return reference.get(path);
	}

	@Override
	public String toString() {
		return "instance[+" + getType().toString() + "]";
	}
}
