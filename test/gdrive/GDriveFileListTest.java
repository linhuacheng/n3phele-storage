package gdrive;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URI;
import java.util.List;

import junit.framework.Assert;
import n3phele.service.model.core.Credential;
import n3phele.service.model.repository.FileNode;
import n3phele.service.model.repository.Repository;
import n3phele.service.model.store.RepositoryStore;
import n3phele.storage.googledrive.CloudStorageImpl;

import org.junit.BeforeClass;
import org.junit.Test;

public class GDriveFileListTest {
	
	private static CloudStorageImpl unit;
	private static Repository repo = null;
	private static Repository repoBadCred = null;
	private static Credential nigelCredential = null;
	private static Credential nigelCredential2 = null;
	
	
	@BeforeClass
	public static void setUp() throws Exception {
		unit = new CloudStorageImpl();
		repo = RepositoryStore.getRepositoryById(2);
		
	}


	@Test
	public void testPutObject() throws FileNotFoundException{
		File file = new File("test/gdrive/document.txt");
		InputStream inputStream = new FileInputStream(file);
		URI path = unit.putObject(repo, inputStream, "text/plain", "document.txt");
		Assert.assertNotNull(path);
		
	}
	@Test
	public void testgetTopOfList() {
		List<FileNode> result = unit.getFileList(repo, "document", -1);

		Assert.assertNotNull(result.size());

	}
	
	@Test
	public void testCreateBucket() {
		unit.createBucket(repo);
		
		boolean bucketFound = false;
		List<FileNode> result = unit.getFileList(repo, "", -1);
		for (FileNode file: result) {
			if (repo.getRoot().equals(file.getName())) {
				bucketFound = true;
				break;
			}
		}
						
		Assert.assertTrue("Bucket not found.", bucketFound);
	}
	
}
