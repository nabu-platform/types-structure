package be.nabu.libs.types.structure;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import be.nabu.libs.types.api.Element;
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
	private Object convert(Object value, Element<?> definition) {
		if (value == null)
			return null;
		if (definition.getType() instanceof BeanType && ((BeanType) definition.getType()).getBeanClass().equals(java.lang.Object.class)) {
			return value;
		}
		else if (definition.getType() instanceof BeanType && ((BeanType) definition.getType()).getBeanClass().isAssignableFrom(value.getClass())) {
			return value;
		}
		Type type = value instanceof ComplexContent ? ((ComplexContent) value).getType() : SimpleTypeWrapperFactory.getInstance().getWrapper().wrap(value.getClass());
		if (type == null) {
			type = BeanResolver.getInstance().resolve(value.getClass());
		}
		if (type.equals(definition.getType())) {
			return value;
		}
		// need to wrap class
		TypeInstance targetType = new BaseTypeInstance(type);
		if (TypeUtils.isSubset(targetType, definition))
			return value;
		Object converted = TypeConverterFactory.getInstance().getConverter().convert(value, targetType, definition);
		// if we were unable to convert and the value is a collection of size 1 and the target is not a collection, let's try to just cast the first element
		if (converted == null && !definition.getType().isList(definition.getProperties())) {
			CollectionHandlerProvider handler = CollectionHandlerFactory.getInstance().getHandler().getHandler(value.getClass());
			if (handler != null) {
				Collection collection = handler.getAsCollection(value);
				if (collection.size() == 1) {
					Object next = collection.iterator().next();
					return convert(next, definition);
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
					value = convert(value, definition);
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
				if (collectionHandler == null) {
					collectionHandler = ValueUtils.getValue(CollectionHandlerProviderProperty.getInstance(), definition.getProperties());
				}
				if (collectionHandler == null) {
					throw new IllegalArgumentException("Can not find a collection handler for: " + value.getClass());
				}
				if (collectionHandler instanceof TypedCollectionHandlerProvider && ((TypedCollectionHandlerProvider) collectionHandler).isCompatible(value, definition.getType())) {
					values.put(parsedPath.getName(), value);
				}
				else {
					// need to convert
					for (Object index : collectionHandler.getIndexes(value)) {
						collectionHandler.set(value, index, convert(collectionHandler.get(value, index), definition));
					}
					values.put(parsedPath.getName(), value);
				}
			}
		}
		// set value
		else if (parsedPath.getChildPath() == null)
			values.put(parsedPath.getName(), convert(value, definition));
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
		Element<?> definition = getType().get(parsedPath.getName());
		
		// access a specific index
		if (parsedPath.getIndex() != null) {
			
			CollectionHandlerProvider collectionHandler = ValueUtils.getValue(new CollectionHandlerProviderProperty(), definition.getProperties());
			// defaults to a list
			if (collectionHandler == null) {
				collectionHandler = CollectionHandlerFactory.getInstance().getHandler().getHandler(List.class);
			}
			
			value = collectionHandler.get(value, parsedPath.getIndex());
		}
		if (value == null) {
			return null;
		}
		else if (parsedPath.getChildPath() == null) { 
			return value;
		}
		else if (!(value instanceof ComplexContent)) {
			throw new ClassCastException("The child " + parsedPath.getName() + " is not a complex content");
		}
		else {
			return ((ComplexContent) value).get(parsedPath.getChildPath().toString());
		}
	}
	
	@Override
	public String toString() {
		return "instance[" + getType() + "]";
	}
}
