package be.nabu.libs.types.structure;

import java.util.Collections;
import java.util.List;

import be.nabu.libs.types.ParsedPath;
import be.nabu.libs.types.TypeUtils;
import be.nabu.libs.types.api.ComplexContent;
import be.nabu.libs.types.api.ComplexType;
import be.nabu.libs.types.api.Element;
import be.nabu.libs.types.api.Type;

/**
 * This is wrapped around another complex content and adds new fields
 * It is effectively a downcast of a complex content instance
 */
public class StructureInstanceDowncastReference extends StructureInstance {
	
	private ComplexContent reference;
	
	public static ComplexContent downcast(ComplexContent instance, ComplexType cast) {
		// check if the target structure is a child type of the current one
		List<Type> upcastPath = TypeUtils.getUpcastPath(cast, instance.getType());
		// it's a child type
		if (!upcastPath.isEmpty()) {
			// first we need to strip any upcast layers that exist, the target type may be hidden beneath them
			// if we strip an upcast layer and the next object is no longer a supertype of the target element, we stop there and wrap it
			if (instance instanceof StructureInstanceUpcastReference) {
				ComplexContent lastMatch = instance;
				while (instance instanceof StructureInstanceUpcastReference) {
					// get the more specific child type
					instance = ((StructureInstanceUpcastReference) instance).getReference();
					// if the type matches, just send it back
					if (instance.getType().equals(cast))
						return instance;
					
					// we have diverged, we need to stop and downcast
					if (TypeUtils.getUpcastPath(cast, instance.getType()).isEmpty()) {
						instance = lastMatch;
						break;
					}
					else
						lastMatch = instance;
				}
				
				// we need to recalculate the upcast path
				upcastPath = TypeUtils.getUpcastPath(cast, instance.getType());
			}
			// we need to invert the upcast path to create a downcast path
			Collections.reverse(upcastPath);
			// the first element is the current one (the way the upcast method works)
			upcastPath.remove(0);
			for (Type type : upcastPath)
				instance = new StructureInstanceDowncastReference(instance, (Structure) type);
			return new StructureInstanceDowncastReference(instance, cast);
		}
		// this is where we diverge from java: there is no direct link between the instance and the target
		// but they may share a common parent
		// to fix this, we will _strip_ downcasts which loses information until we find a common parent, then we add new downcasts
		// the parent values are shared between the two instances
		else {
			while (instance instanceof StructureInstanceDowncastReference) {
				instance = ((StructureInstanceDowncastReference) instance).getReference();
				if (instance.getType().equals(cast)) {
					return instance;
				}
				upcastPath = TypeUtils.getUpcastPath(cast, instance.getType());
				// we have found a common parent, start rewrapping
				if (!upcastPath.isEmpty()) {
					Collections.reverse(upcastPath);
					upcastPath.remove(0);
					for (Type type : upcastPath)
						instance = new StructureInstanceDowncastReference(instance, (Structure) type);
					return new StructureInstanceDowncastReference(instance, cast);
				}
			}
		}
		return null;
	}

	StructureInstanceDowncastReference(ComplexContent reference, ComplexType childType) {
		if (!reference.getType().equals(childType.getSuperType()))
			throw new RuntimeException("Can only create a downcast reference around an immediate parent");
		this.reference = reference;
		setType(childType);
	}
	
	public ComplexContent getReference() {
		return reference;
	}

	@Override
	public void set(String path, Object value) {
		ParsedPath parsedPath = ParsedPath.parse(path);
		Element<?> element = getType().get(parsedPath.getName());
		if (element == null) {
			throw new NullPointerException("Can not find field " + parsedPath.getName() + " in " + getType());
		}
		// optimized version: checks if the parent of the target element is this type
		// TO BE VALIDATED (changed @ 2016-03-21)
		if (getType().equals(element.getParent())) {
			super.set(path, value);
		}
		else {
			reference.set(path, value);
		}
//		// it's a local child, set it in this instance
//		if (TypeUtils.getLocalChild(getType(), parsedPath.getName()) != null)
//			super.set(path, value);
//		// otherwise, set it in the parent instance
//		else
//			reference.set(path, value);
	}

	@Override
	public Object get(String path) {
		ParsedPath parsedPath = ParsedPath.parse(path);
		if (TypeUtils.getLocalChild(getType(), parsedPath.getName()) != null)
			return super.get(path);
		else
			return reference.get(path);
	}
	
	@Override
	public String toString() {
		return "instance[" + getType().toString() + "]";
	}
}
