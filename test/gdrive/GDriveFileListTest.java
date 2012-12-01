package gdrive;

import java.io.File;
import java.util.List;

import junit.framework.Assert;
import n3phele.service.model.core.Credential;
import n3phele.service.model.repository.FileNode;
import n3phele.service.model.repository.Repository;
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
	public static void setUpBeforeClass() throws Exception {
		File keyFile = new File("test/gdrive/b64713b4d6f38c36d0dd5b4f83fee7d5bebc04af-privatekey.p12");
		GoogleCredential credential = new GoogleCredential.Builder().setTransport(new NetHttpTransport())
	            .setJsonFactory(new JacksonFactory())
	            .setServiceAccountId("chandu.kempaiah@gmail.com")
	            .setServiceAccountScopes(DriveScopes.DRIVE)
	            .setServiceAccountPrivateKeyFromP12File(keyFile)
	            // .setServiceAccountUser("user@example.com")
	            .build();
		
		Credential n2pheleCred = new Credential(credential.getServiceAccountId(), credential.getAccessToken());
		repo = new Repository("repo1", "repo1 description", n2pheleCred, null, "/", "Drive", null, false);
		
	}

	@Before
	public void setUp() throws Exception {
		unit = new CloudStorageImpl();
	}

	@Test
	public void testgetTopOfList() {
		List<FileNode> result = unit.getFileList(repo, "", -1);

		Assert.assertEquals(1, result.size());

	}
	

}
