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

public class SwiftCreateBucketTest {
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
		boolean result = unit.createBucket(repo);
		Assert.assertEquals(false, result);
	}
	
	@Test
	public void testDoesntExist() {
		boolean result = unit.createBucket(repoNew);
		Assert.assertEquals(false, result);
		Assert.assertTrue(unit.deleteBucket(repoNew));
	}
	
	@Test
	public void testBadCredential() {
		try {
			List<FileNode> result = unit.getFileList(repoBadCred, "", -1);
			Assert.fail("Exception expected");
		} catch (Exception e) {
			Assert.assertTrue(e instanceof ForbiddenException);
		}

	}
	

}