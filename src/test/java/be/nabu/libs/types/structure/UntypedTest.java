package be.nabu.libs.types.structure;

import junit.framework.TestCase;

public class UntypedTest extends TestCase {
	public void testUntyped() {
		UntypedStructure structure = new UntypedStructure();
		StructureInstance newInstance = structure.newInstance();
		newInstance.set("test", "something");
		assertEquals("something", newInstance.get("test"));
	
	}
}
