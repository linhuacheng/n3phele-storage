package n3phele;

import junit.framework.Assert;
import n3phele.service.model.repository.FileNode;
import n3phele.service.model.repository.Repository;
import n3phele.service.model.core.Credential;
import n3phele.storage.swift.CloudStorageImpl;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class SwiftDeleteTest {
	private static CloudStorageImpl unit;
	private static Repository repo = null;
	private static Repository repoBadCred = null;
	private static Credential nigelCredential = null;
	private static Credential nigelCredential2 = null;
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		nigelCredential2 = new Credential("12365734013392:11520587032204", "changeme").encrypt();
		nigelCredential = new Credential("nigel.cook@hp.com:11520587032204", "changeme").encrypt();
		repo = new Repository("unit-testing", "testing repo", nigelCredential, null, "n3phele-unit-test", "Swift", null, false);
		repoBadCred = new Repository("repo1", "repo1 description", nigelCredential2, null, "ddd", "Swift", null, false);
		unit = new CloudStorageImpl(false);
		testcreateSomeObjects();
	}

	@Before
	public void setUp() throws Exception {

	}

	public static void testcreateSomeObjects() {
		unit.createObject(repo, "file1", "some content");
		unit.createObject(repo, "folder1/file2.txt", "now is the time");
		unit.createObject(repo, "folder1/file3.txt", "now is the time");
		unit.createObject(repo, "folder1/folder2/file3.txt", "now is the time");
		
		unit.getFileList(repo, null, -1);
		unit.getFileList(repo, "folder1/", -1);

	}
	
	@Test
	public void testDeleteFile() {
		boolean result = unit.deleteFile(repo, "file1");
		Assert.assertTrue("File deleted", result);
		//result = unit.deleteFile(repo,"file1");
		//Assert.assertTrue("delete failed - file doesnt exist", result);
	}
	
	@Test
	public void testDeleteFolder() {
		boolean result = unit.deleteFolder(repo, "folder1");
		Assert.assertTrue(result);
	}

}
