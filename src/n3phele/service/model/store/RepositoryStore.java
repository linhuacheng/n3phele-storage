package n3phele.service.model.store;

import java.net.URI;
import java.util.HashMap;

import n3phele.service.model.core.Credential;
import n3phele.service.model.repository.Repository;
import n3phele.storage.CloudStorage;

public class RepositoryStore {
	
	private static HashMap<Integer, Repository> repositoryMap = new HashMap<Integer, Repository>();
	
	static {
		//adding a sample repository
		Credential chanduAWSCreds = new Credential("AKIAJQ4UK2TUFDOI3ZQA", "pODzu2s8SfmgQE1LRRANS32zLLQjWqOBHxvoOcMP").encrypt();
		Repository repo = new Repository("repo1", "repo1 description", chanduAWSCreds, URI.create("https://s3.amazonaws.com"), "n3phele-test", "S3", null, true);
		repo.setId(1l);
		repositoryMap.put(1, repo);
		
		Credential chanduGoogleCred = new Credential("181673875910@developer.gserviceaccount.com","WEB-INF/data/b64713b4d6f38c36d0dd5b4f83fee7d5bebc04af-privatekey.p12").encrypt();
		Repository repo2 = new Repository("Google Drive Repo", "Google Drive Repo", chanduGoogleCred, URI.create("https://www.googleapis.com/drive/v2/files"), "n3phele-test", CloudStorage.GDRIVE, null, true);
		repo2.setId(2l);
		repositoryMap.put(2, repo2);
		//add additional repositories
		
		Credential testGoogleCred = new Credential("181673875910@developer.gserviceaccount.com","test/gdrive/b64713b4d6f38c36d0dd5b4f83fee7d5bebc04af-privatekey.p12").encrypt();
		Repository testGoogleRepo = new Repository("Google Drive Repo", "Google Drive Repo", testGoogleCred, URI.create("https://www.googleapis.com/drive/v2/files"), "n3phele-test", CloudStorage.GDRIVE, null, true);
		testGoogleRepo.setId(3l);
		repositoryMap.put(3, testGoogleRepo);
		
	}
	public static final Repository getRepositoryById(Integer repoId) {
		
		if (repositoryMap.get(repoId) != null){
			return repositoryMap.get(repoId);
		}
		return null;
	}

}
