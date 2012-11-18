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

public class SwiftObjectCreateTest {
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
	}

	@Before
	public void setUp() throws Exception {
		unit = new CloudStorageImpl(false);
	}

	@Test
	public void testcreateSomeObjects() {
		unit.createObject(repo, "file1", "");
		unit.createObject(repo, "xfer/orca/test/file.txt", "now is the time");
		unit.createObject(repo, "folder1/file2.txt", "now is the time");
		unit.createObject(repo, "folder1/file3.txt", "now is the time");
		
		unit.getFileList(repo, null, -1);
		unit.getFileList(repo, "folder1/", -1);

	}
	

}
