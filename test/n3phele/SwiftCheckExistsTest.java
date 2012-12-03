package n3phele;

import java.net.URI;

import javax.ws.rs.WebApplicationException;

import junit.framework.Assert;
import n3phele.service.core.ForbiddenException;
import n3phele.service.model.repository.FileNode;
import n3phele.service.model.repository.Repository;
import n3phele.service.model.core.Credential;
import n3phele.storage.swift.CloudStorageImpl;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class SwiftCheckExistsTest {
	private static CloudStorageImpl unit;
	private static Repository repo = null;
	private static Repository repoNew = null;
	private static Repository repoBadCred = null;
	private static Credential nigelCredential = null;
	private static Credential nigelCredential2 = null;
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		nigelCredential2 = new Credential("12365734013392:11520587032204", "changeme").encrypt();
		nigelCredential = new Credential("nigel.cook@hp.com:11520587032204", "changeme").encrypt();
		repo = new Repository("repo1", "repo1 description", nigelCredential, URI.create("https://region-a.geo-1.identity.hpcloudsvc.com:35357"), "ddd", "Swift", null, false);
		repoNew = new Repository("repo1", "repo1 description", nigelCredential, URI.create("https://region-a.geo-1.identity.hpcloudsvc.com:35357"), "ddd", "Swift", null, false);
		repoBadCred = new Repository("repo1", "repo1 description", nigelCredential2, URI.create("https://region-a.geo-1.identity.hpcloudsvc.com:35357"), "unitTest", "Swift", null, false);
	}

	@Before
	public void setUp() throws Exception {
		unit = new CloudStorageImpl(false);
	}

	@Test
	public void testAlreadyExists() {
		boolean result = unit.checkExists(repo, "dir1/2012OpenStack_Guide.pdf");
		Assert.assertEquals(true, result);
	}
	
	@Test
	public void testDoesntExist() {
		boolean result = unit.checkExists(repo, "now/is/the/time");
		Assert.assertEquals(false, result);
	}
	
	@Test
	public void testDirectoryReference() {
		boolean result = unit.checkExists(repo, "dir1/");
		Assert.assertEquals(true, result);
	}
	@Test
	public void testDirectoryReferenceNoSlash() {
		boolean result = unit.checkExists(repo, "dir1");
		Assert.assertEquals(false, result);
	}
	
	@Test
	public void testBadCredential() {
		try {
			boolean result = unit.checkExists(repoBadCred, "blah");
			Assert.fail("Exception expected");
		} catch (Exception e) {
			e.printStackTrace();
			Assert.assertTrue(e instanceof ForbiddenException);
		}

	}
	
	@Test
	public void testBadRepo() {
		String old = repo.getRoot();
		repo.setRoot("Foobar");
		try {
			
			boolean result = unit.checkExists(repo, "blah");
			Assert.assertFalse(result);
		} catch (Exception e) {
			Assert.fail("Exception not expected");
		} finally {
			repo.setRoot(old);
		}

	}
	

}