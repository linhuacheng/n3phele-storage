package n3phele;

import java.util.List;

import junit.framework.Assert;
import n3phele.service.core.ForbiddenException;
import n3phele.service.model.repository.FileNode;
import n3phele.service.model.repository.Repository;
import n3phele.service.model.core.Credential;
import n3phele.storage.swift.CloudStorageImpl;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class SwiftGetMetadataTest {
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
		repo = new Repository("repo1", "repo1 description", nigelCredential, null, "ddd", "Swift", null, false);
		repoNew = new Repository("repo1", "repo1 description", nigelCredential, null, "ddd", "Swift", null, false);
		repoBadCred = new Repository("repo1", "repo1 description", nigelCredential2, null, "unitTest", "Swift", null, false);
	}

	@Before
	public void setUp() throws Exception {
		unit = new CloudStorageImpl(false);
	}

	@Test
	public void testAlreadyExists() {
		FileNode result = unit.getMetadata(repo, "dir1/2012OpenStack_Guide.pdf");
		Assert.assertEquals(441011, result.getSize());
		Assert.assertEquals("dir1/", result.getPath());
		Assert.assertEquals("2012OpenStack_Guide.pdf", result.getName());
	}
	
	@Test
	public void testDoesntExist() {
		FileNode result = unit.getMetadata(repo, "now/is/the/time");
		Assert.assertEquals(null, result);
	}
	
	@Test
	public void testDirectoryReference() {
		FileNode result = unit.getMetadata(repo, "dir1/");
		Assert.assertEquals("dir1", result.getName());
	}
	@Test
	public void testDirectoryReferenceNoSlash() {
		FileNode result = unit.getMetadata(repo, "dir1");
		Assert.assertEquals(null, result);
	}
	
	@Test
	public void testBadCredential() {
		try {
			FileNode result = unit.getMetadata(repoBadCred, "blah");
			Assert.fail("Exception expected");
		} catch (Exception e) {
			Assert.assertTrue(e instanceof ForbiddenException);
		}

	}
	
	@Test
	public void testBadRepo() {
		String old = repo.getRoot();
		repo.setRoot("Foobar");
		try {
			
			FileNode result = unit.getMetadata(repo, "blah");
			Assert.assertNull(result);
		} catch (Exception e) {
			Assert.fail("Exception not expected");
		} finally {
			repo.setRoot(old);
		}

	}
	

}