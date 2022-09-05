package be.nabu.libs.types.structure;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import be.nabu.libs.converter.ConverterFactory;
import be.nabu.libs.property.ValueUtils;
import be.nabu.libs.types.BaseTypeInstance;
import be.nabu.libs.types.CollectionHandlerFactory;
import be.nabu.libs.types.ComplexContentWrapperFactory;
import be.nabu.libs.types.ParsedPath;
import be.nabu.libs.types.SimpleTypeWrapperFactory;
import be.nabu.libs.types.TypeConverterFactory;
import be.nabu.libs.types.TypeUtils;
import be.nabu.libs.types.api.CollectionHandlerProvider;
import be.nabu.libs.types.api.ComplexContent;
import be.nabu.libs.types.api.ComplexType;
import be.nabu.libs.types.api.DefinedType;
import be.nabu.libs.types.api.Element;
import be.nabu.libs.types.api.SimpleType;
import be.nabu.libs.types.api.Type;
import be.nabu.libs.types.api.TypeInstance;
import be.nabu.libs.types.api.TypedCollectionHandlerProvider;
import be.nabu.libs.types.java.BeanInstance;
import be.nabu.libs.types.java.BeanResolver;
import be.nabu.libs.types.java.BeanType;
import be.nabu.libs.types.properties.CollectionHandlerProviderProperty;

/**
 * When initializing a new structure instance as a child
 * 	NEED TO SET THE DEFINITION!!
 * 
 * 1) when instantiating a new structure instance, need to instantiate all the levels!! necessary for upcasting
 * 2) once 1) is done, finish the upcast logic in cast()
 * 
 * @author alex
 *
 */
public class StructureInstance implements ComplexContent {

	private ComplexType definition;
	
	private Map<String, Object> values = new HashMap<String, Object>();
	
	public StructureInstance(ComplexType type) {
		setType(type);
	}
	
	StructureInstance() {
		// internal
	}
	
	@Override
	public boolean equals(Object object) {
		if (!(object instanceof StructureInstance))
			return false;
		// if they are the same object, sure!
		if (this == object) {
			return true;
		}
		StructureInstance other = (StructureInstance) object;
		// compare the definition
		if (!getType().equals(other.getType()))
			return false;
		// check each child in the definition, the values must match
		// it is possible that one instance is an upcast of a broader instance hence containing more values
		// these additional values are *not* checked, only the ones that are covered in the definition
		for (Element<?> element : TypeUtils.getAllChildren(getType())) {
			Object value = get(element.getName());
			Object otherValue = other.get(element.getName());
			if ((value == null && otherValue != null) || (value != null && otherValue == null))
				return false;
			else if (value != null && !value.equals(otherValue))
				return false;
		}
		return true;
	}
	
	protected void setType(ComplexType definition) {
		this.definition = definition;
	}

	@Override
	public ComplexType getType() {
		return definition;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private Object convert(Object value, Element<?> definition, boolean wantIndex) {
		if (value == null)
			return null;
		// if the target is an Object, allow anything
		if (definition.getType() instanceof BeanType && ((BeanType) definition.getType()).getBeanClass().equals(java.lang.Object.class)) {
			return value;
		}
		// if the target is an assignable class, allow it
		else if (definition.getType() instanceof BeanType && ((BeanType) definition.getType()).getBeanClass().isAssignableFrom(value.getClass())) {
			return value;
		}
		else if (definition.getType() instanceof BeanType && value instanceof ComplexContent) {
			// if we have a bean instance _or_ the target is an interface (which means we can dynamically wrap anything), unwrap the complex content into a bean
			if (value instanceof BeanInstance || ((BeanType) definition.getType()).getBeanClass().isInterface()) {
				Object potential = getAsBean((ComplexContent) value, ((BeanType) definition.getType()).getBeanClass());
				if (potential != null) {
					return potential;
				}	
			}
			// it is possible that we "masked" the content, that means the type would be the same, but the content would _not_ be a bean
			// we assume the target is not an interface, which means dynamic proxying won't work
			// when communicating with java, this will pose a problem, but as long as we stay away from java bindings, it shouldn't matter too much
			// for java communication, it is the tooling that wraps around that which should take care of final conversion
			// if the types are compatible, we return the value itself
			else if (TypeUtils.isExtension(((ComplexContent) value).getType(), definition.getType())) {
				return value;
			}
			// this is the old code, where we always unwrap the bean. this will fail for the combination of masked content and non-proxyable classes
			// we leave this here to get the "original" failure mode
			Object potential = getAsBean((ComplexContent) value, ((BeanType) definition.getType()).getBeanClass());
			if (potential != null) {
				return potential;
			}
		}
		Type type = value instanceof ComplexContent ? ((ComplexContent) value).getType() : SimpleTypeWrapperFactory.getInstance().getWrapper().wrap(value.getClass());
		if (type == null) {
			type = BeanResolver.getInstance().resolve(value.getClass());
		}
		if (type.equals(definition.getType())) {
			return value;
		}
		// the types are not the same instance, but they are two instances of the same type
		// this should not occur in production except perhaps for a really badly timed reload
		// this "should" be ok from a value perspective however, so we'll allow it for now (started allowing it @25-02-2020)
		else if (type instanceof DefinedType && definition.getType() instanceof DefinedType && ((DefinedType) type).getId().equals(((DefinedType) definition.getType()).getId())) {
			return value;
		}
		
		// need to wrap class
		TypeInstance targetType = new BaseTypeInstance(type);
		if (TypeUtils.isSubset(targetType, definition))
			return value;
		// if the definition is a simple type and we have a complex simple type, we use the value content instead of the actual type
		else if (definition.getType() instanceof SimpleType && type instanceof ComplexType && type instanceof SimpleType && value instanceof ComplexContent) {
			value = ((ComplexContent) value).get(ComplexType.SIMPLE_TYPE_VALUE);
		}
		Object converted = TypeConverterFactory.getInstance().getConverter().convert(value, targetType, definition);
		// if we were unable to convert and the value is a collection of size 1 and the target is not a collection, let's try to just cast the first element
		// the target may be a list, but if we set it in an indexed way, we still may want to convert it
		// the usecase was: we have a list where we draw a line from, we add a filter so we only get a single result and draw it to the [0] element of a target list
		// in this case, the definition would state that it is a list, that's why we added the wantIndex boolean in case we want indexed access to the list
		if (converted == null && (!definition.getType().isList(definition.getProperties()) || wantIndex)) {
			CollectionHandlerProvider handler = CollectionHandlerFactory.getInstance().getHandler().getHandler(value.getClass());
			if (handler != null) {
				Collection collection = handler.getAsCollection(value);
				if (collection.size() == 1) {
					Object next = collection.iterator().next();
					return convert(next, definition, wantIndex);
				}
				else if (collection.size() == 0) {
					// although there is no guarantee the types would've matched had they been there
					// however, in general we don't get to this code if the parent types don't match so in general this is a direct set
					// directly setting an empty list could be seen as setting a "null" value
					return null;
//					throw new IllegalArgumentException("The empty collection can not be converted to the single item: " + definition.getType());
				}
				else {
					throw new IllegalArgumentException("The non-empty collection '" + value + "' for field '" + definition.getName() + "' can not be converted from " + targetType.getType() + " to the single item of type " + definition.getType());		
				}
			}
		}
		// if we can't convert it at the high level but we have a low level conversion, use that
		// this can be used for example for clob > string conversion where clobs are _not_ wrapped as simple types
		if (converted == null && definition.getType() instanceof SimpleType && ((ConverterFactory.getInstance().getConverter().canConvert(value.getClass(), ((SimpleType) definition.getType()).getInstanceClass())))) {
			return ConverterFactory.getInstance().getConverter().convert(value, ((SimpleType) definition.getType()).getInstanceClass());
		}
		if (converted == null)
			throw new IllegalArgumentException("The value '" + value + "' can not be converted from " + targetType.getType() + " to " + definition.getType());
		return converted;
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public void set(String path, Object value) {
		ParsedPath parsedPath = ParsedPath.parse(path);
		Element<?> definition = getType().get(parsedPath.getName());
		if (definition == null)
			throw new IllegalArgumentException("Could not find definition for '" + parsedPath.getName() + "' in: " + getType());
		
		if (parsedPath.getIndex() != null) {
			// check if it is actually a list
			if (!definition.getType().isList(definition.getProperties()))
				throw new IllegalArgumentException("The field " + parsedPath.getName() + " is not a list, can not access it through an index");
			
			Object collection = values.get(parsedPath.getName());
			CollectionHandlerProvider collectionHandler = ValueUtils.getValue(new CollectionHandlerProviderProperty(), definition.getProperties());
			// defaults to a list
			if (collectionHandler == null) {
				collectionHandler = CollectionHandlerFactory.getInstance().getHandler().getHandler(List.class);
			}
			Object index = collectionHandler.unmarshalIndex(parsedPath.getIndex());
			// no list set yet, generate one that matches at least the size of the minOccurs for this element
			if (collection == null && (CREATE_PARENT_FOR_NULL_VALUE || value != null)) {
				int size = index instanceof Integer ? ((Integer) index) + 1 : 1;
				// the collection handler must allow a null create
				collection = collectionHandler.create(null, size);
				values.put(parsedPath.getName(), collection);
			}
			
			if (collection != null) {
				// simply updating that index
				if (parsedPath.getChildPath() == null) {
					value = convert(value, definition, true);
					values.put(parsedPath.getName(), collectionHandler.set(collection, index, value));
				}
				else {
					Object childObject = collectionHandler.get(collection, index);
					ComplexContent child = null;
					if (childObject != null) {
						child = childObject instanceof ComplexContent ? (ComplexContent) childObject : ComplexContentWrapperFactory.getInstance().getWrapper().wrap(childObject);
					}
					// the child does not yet exist, instantiate it
					if (child == null && (CREATE_PARENT_FOR_NULL_VALUE || value != null)) {
						child = ((ComplexType) definition.getType()).newInstance();
						// if the child is a bean instance, we are going to assume the collection handler requires it
						if (child instanceof BeanInstance) {
							values.put(parsedPath.getName(), collectionHandler.set(collection, index, ((BeanInstance) child).getUnwrapped()));
						}
						else {
							values.put(parsedPath.getName(), collectionHandler.set(collection, index, child));
						}
					}
					if (child != null) {
						child.set(parsedPath.getChildPath().toString(), value);
					}
				}
			}
		}
		// you did not provide an index, but the target is a list
		// this is only valid if you are setting this item (no child path) AND the value is a list
		else if (definition.getType().isList(definition.getProperties())) {
			
			if (parsedPath.getChildPath() != null)
				throw new RuntimeException("Can not access list " + definition.getName() + " without an index");
			else if (value == null)
				values.remove(parsedPath.getName());
			else {
				// in the past this code enforced the "correct" collection handler if dictated by the type, so for instance if the type says its a list and you give an array, it will transform the collection
				// however, this removes some of the flexibility of having for example a jdbc result set as a value
				// most of nabu does (or should) use collection handlers, so the actual type of the collection at runtime does not matter, as long as it is _a_ collection
				// there is one exception: when integrating with java beans, they have a hard requirement for a specific type of collection but it is then up to the bean instance to enforce this, not the structure instance
				// code updated @2016-12-06: this _can_ have unforseen side effects
				CollectionHandlerProvider collectionHandler = CollectionHandlerFactory.getInstance().getHandler().getHandler(value.getClass());
				// if we don't have a collection handler, we assume you meant to target the first element, we retry with an index
				if (collectionHandler == null) {
					set(parsedPath.getName() + "[0]", value);
				}
				// if we are expecting a map and we have a map, just...map it
				// don't convert, the collection handling does not work well with that
				else if (definition.getType() instanceof BeanType && Map.class.isAssignableFrom(((BeanType) definition.getType()).getBeanClass()) && value instanceof Map) {
					values.put(parsedPath.getName(), value);
				}
				// if we have an object, it doesn't matter, just put it
				else if (definition.getType() instanceof BeanType && Object.class.equals(((BeanType) definition.getType()).getBeanClass())) {
					values.put(parsedPath.getName(), value);
				}
				// otherwise we attempt to merge
				else {
					if (collectionHandler instanceof TypedCollectionHandlerProvider && ((TypedCollectionHandlerProvider) collectionHandler).isCompatible(value, definition.getType())) {
						values.put(parsedPath.getName(), value);
					}
					else {
						// if we have a numeric index, we want to set directly in the target element
						// the problem is twofold with the below loop:
						// 1) we don't want to change the collections in the original location, you may need them further
						// 2) some collections are read-only and can not even be updated (e.g. the jdbc array), by forcing numeric access, we force creation of a new, compatible collection
						if (Number.class.isAssignableFrom(collectionHandler.getIndexClass())) {
							// we want to unset the existing collection so we don't manipulate it directly
							set(parsedPath.getName(), null);
							for (Object index : collectionHandler.getIndexes(value)) {
								// iteratively set
								set(parsedPath.getName() + "[" + index + "]", collectionHandler.get(value, index));
							}
						}
						else {
							// need to convert
							for (Object index : collectionHandler.getIndexes(value)) {
								collectionHandler.set(value, index, convert(collectionHandler.get(value, index), definition, false));
							}
							values.put(parsedPath.getName(), value);
						}
					}
				}
			}
		}
		// set value
		else if (parsedPath.getChildPath() == null)
			values.put(parsedPath.getName(), convert(value, definition, false));
		// setting in child
		else {
			Object object = values.get(parsedPath.getName());
			ComplexContent child = object instanceof ComplexContent || object == null ? (ComplexContent) object : ComplexContentWrapperFactory.getInstance().getWrapper().wrap(object);
			if (child == null && (CREATE_PARENT_FOR_NULL_VALUE || value != null)) {
				child = ((ComplexType) definition.getType()).newInstance();
				values.put(parsedPath.getName(), child);
			}
			if (child != null) {
				child.set(parsedPath.getChildPath().toString(), value);
			}
		}
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public Object get(String path) {
		ParsedPath parsedPath = ParsedPath.parse(path);
		Object value = values.get(parsedPath.getName());
		
		// access a specific index
		if (parsedPath.getIndex() != null) {
			Element<?> definition = getType().get(parsedPath.getName());
			
			CollectionHandlerProvider collectionHandler = ValueUtils.getValue(new CollectionHandlerProviderProperty(), definition.getProperties());
			// defaults to a list
			if (collectionHandler == null) {
				collectionHandler = CollectionHandlerFactory.getInstance().getHandler().getHandler(List.class);
			}
			
			Object index = parsedPath.getIndex();
			// if we can't use string indexes, convert it
			if (!collectionHandler.getIndexClass().isAssignableFrom(index.getClass())) {
				index = ConverterFactory.getInstance().getConverter().convert(index, collectionHandler.getIndexClass());
				if (index == null) {
					throw new IllegalArgumentException("Can not convert index '" + parsedPath.getIndex() + "' to: " + collectionHandler.getIndexClass());
				}
			}
			value = collectionHandler.get(value, index);
		}
		
		if (value == null) {
			return null;
		}
		else if (parsedPath.getChildPath() == null) { 
			return value;
		}
		
		if (!(value instanceof ComplexContent)) {
			Object converted = ComplexContentWrapperFactory.getInstance().getWrapper().wrap(value);
			if (converted == null) {
				throw new ClassCastException("The child " + parsedPath.getName() + " is not a complex content");
			}
			value = converted;
		}
		return ((ComplexContent) value).get(parsedPath.getChildPath().toString());
	}
	
	@Override
	public String toString() {
		return "instance[" + getType() + "]";
	}
	
	// we check if there are any bean instances in the cast path that might be ok
	// suppose class A and class B extends A
	// we define a service that requests A and we pass in an instance of B
	// in the current setup, B can be masked as A which makes it very hard for the code to determine whether the A is also of type B (for example optional logic based on more information)
	// so we check if anywhere along the line we obscured a bean instance that is of a compatible type
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static Object getAsBean(ComplexContent content, Class<?> clazz) {
		ComplexContent reference = content;
		while (reference != null) {
			if (reference instanceof BeanInstance && clazz.isAssignableFrom(((BeanInstance) reference).getUnwrapped().getClass())) {
				return ((BeanInstance) reference).getUnwrapped();
			}
			// subtle difference: for example masked content does not have a bean instance but could have a bean type
			else if (reference.getType() instanceof BeanType && clazz.isAssignableFrom(((BeanType) reference.getType()).getBeanClass())) {
				return TypeUtils.getAsBean(reference, ((BeanType) reference.getType()).getBeanClass());
			}
			else if (reference instanceof StructureInstanceDowncastReference) {
				reference = ((StructureInstanceDowncastReference) reference).getReference();
			}
			else if (reference instanceof StructureInstanceUpcastReference) {
				reference = ((StructureInstanceUpcastReference) reference).getReference();
			}
			else {
				break;
			}
			// TODO: in theory we should check if your each type along the way is potentially an extension to the bean type, that would also result in a valid bean
		}
		return null;
	}
}
