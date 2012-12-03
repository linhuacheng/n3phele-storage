package gdrive;

import java.io.File;
import java.util.List;

import junit.framework.Assert;
import n3phele.service.model.core.Credential;
import n3phele.service.model.repository.FileNode;
import n3phele.service.model.repository.Repository;
import n3phele.service.model.store.RepositoryStore;
import n3phele.storage.googledrive.CloudStorageImpl;


import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson.JacksonFactory;
import com.google.api.services.drive.DriveScopes;

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
	public void testPutObject(){
		unit.putObject(repo, null, "text/plain", "");
		
	}
	@Test
	public void testgetTopOfList() {
		List<FileNode> result = unit.getFileList(repo, "", -1);

		Assert.assertEquals(1, result.size());

	}
	
}
