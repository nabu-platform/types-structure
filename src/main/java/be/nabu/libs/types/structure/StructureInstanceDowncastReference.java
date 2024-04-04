package be.nabu.libs.types.structure;

import java.util.Collections;
import java.util.List;

import be.nabu.libs.converter.ConverterFactory;
import be.nabu.libs.types.ComplexContentWrapperFactory;
import be.nabu.libs.types.ParsedPath;
import be.nabu.libs.types.SimpleTypeWrapperFactory;
import be.nabu.libs.types.TypeConverterFactory;
import be.nabu.libs.types.TypeUtils;
import be.nabu.libs.types.api.ComplexContent;
import be.nabu.libs.types.api.ComplexType;
import be.nabu.libs.types.api.DefinedSimpleType;
import be.nabu.libs.types.api.Element;
import be.nabu.libs.types.api.SimpleType;
import be.nabu.libs.types.api.Type;
import be.nabu.libs.types.mask.MaskedContent;

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
	public boolean has(String path) {
		return super.has(path) || reference.has(path);
	}

	@Override
	public void set(String path, Object value) {
		ParsedPath parsedPath = ParsedPath.parse(path);
		Element<?> element = getType().get(parsedPath.getName());
		// because we allow restricting of types, it is possible that the field does _not_ exist in the extension, but _does_ exist in the instance we are extending
		// modified @2019-09-28
		if (element == null) {
			element = reference.getType().get(parsedPath.getName());
		}
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
		// if it is a local child
		if (TypeUtils.isLocalChild(getType(), parsedPath.getName())) {
			// check that a value has been set in this instance, if so we ignore any parent value you might have
			if (super.has(parsedPath.getName())) {
				return super.get(path);
			}
			// if not, check if it was restricted and re-added, in that case a valid value may be available in the parent
			else if (getType().getSuperType() instanceof ComplexType && ((ComplexType) getType().getSuperType()).get(parsedPath.getName()) != null) {
				Object object = reference.get(path);
				if (object != null) {
					Element<?> fromElement = reference.getType().get(parsedPath.getName());
					Element<?> toElement = getType().get(parsedPath.getName());
					// simple types are assumed to be immutable and are sent raw
					if (fromElement.getType() instanceof SimpleType && toElement.getType() instanceof SimpleType) {
						// no conversion necessary
						if (fromElement.getType().equals(toElement.getType())) {
							return object;
						}
						// otherwise, we convert it
						return TypeConverterFactory.getInstance().getConverter().convert(object, fromElement, toElement);		
					}
					// if both are complex, we mask it (?) to prevent referential changes on the one hand and to ensure compatibility on the other
					else if (fromElement.getType() instanceof ComplexType && toElement.getType() instanceof ComplexType) {
						ComplexContent complexContent = object instanceof ComplexContent ? (ComplexContent) object : ComplexContentWrapperFactory.getInstance().getWrapper().wrap(object);
						if (complexContent == null) {
							throw new IllegalArgumentException("Could not be cast to complex content for autowrapping: " + object);
						}
						return new MaskedContent(complexContent, (ComplexType) toElement.getType());
					}
					// @2024-03-27: we do not currently support autocasting simple types to complex types and back when restricting and re-adding
					// we do want to allow you to do that though, we just can't automatically port the value so we send back null
					else {
						return null;
					}
				}
			}
			return null;
		}
		else {
			return reference.get(path);
		}
//		if (TypeUtils.getLocalChild(getType(), parsedPath.getName()) != null)
//			return super.get(path);
//		else
//			return reference.get(path);
	}
	
	@Override
	public String toString() {
		return "instance[-" + getType().toString() + "]";
	}
}
