package n3phele;

import junit.framework.Assert;

import org.junit.Test;

public class MatchesTest {
	@Test
	public void testEntireMatch() {
		boolean result = "123456".matches("[0-9]*");
		Assert.assertTrue(result);
	}
	@Test
	public void testPartialMatch() {
		boolean result = "123456AB".matches("[0-9]*");
		Assert.assertFalse(result);
	}
}
