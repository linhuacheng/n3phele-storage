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
import n3phele.service.model.repository.UploadSignature;
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

    private static String fileName = "document.txt";
    private static String fileName2 = "document.txt2";

    @BeforeClass
    public static void setUp() throws Exception {
        unit = new CloudStorageImpl();
        repo = RepositoryStore.getRepositoryById(2);

    }
    @Test
    public void testCreateBucket() {
        unit.createBucket(repo);

        boolean bucketFound = false;
        String rootFileId = unit.getRootFileId(repo);
        bucketFound = (rootFileId != null  && rootFileId.length() >0 );

        Assert.assertTrue("Bucket not found.", bucketFound);
    }

    @Test
    public void testPutObject() throws FileNotFoundException{
        File file = new File("test/gdrive/document.txt");
        InputStream inputStream = new FileInputStream(file);
        URI path = unit.putObject(repo, inputStream, "text/plain", fileName);
        Assert.assertNotNull(path);

    }
    @Test
    public void testgetTopOfList() {
        List<FileNode> result = unit.getFileList(repo, "document", -1);

        Assert.assertNotNull(result.size());

    }
    @Test
    
    public void testGetURL() throws Exception{
    	testCreateBucket();
    	testPutObject();
    	unit.getURL(repo, "", fileName);
    }
    @Test
    public void testGetMetadata() {
        FileNode meta = unit.getMetadata(repo, fileName);
        Assert.assertEquals(meta.getName(), fileName);

    }

    @Test
    public void testCheckExists() {
        Assert.assertTrue("Check exists failed", unit.checkExists(repo, "document.txt"));
    }

    @Test
    public void testDeleteFile() {
        Assert.assertTrue("File was not deleted.", unit.deleteFile(repo, fileName));
        Assert.assertFalse("Check exists failed", unit.checkExists(repo, "document.txt"));
    }


    @Test
    public void testDeleteFolder() throws FileNotFoundException {
        Assert.assertTrue("Folder was not deleted.", unit.deleteFolder(repo, fileName));
    }

    @Test
    public void testGetUploadSignature(){
    	UploadSignature uploadSignature = unit.getUploadSignature(repo, fileName);
    	Assert.assertNotNull(uploadSignature);
    }

}
