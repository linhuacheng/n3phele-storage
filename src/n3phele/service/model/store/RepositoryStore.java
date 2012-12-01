package n3phele.service.model.store;

import java.net.URI;
import java.util.HashMap;

import n3phele.service.model.core.Credential;
import n3phele.service.model.repository.Repository;

public class RepositoryStore {
	
	private static HashMap<Integer, Repository> repositoryMap = new HashMap<Integer, Repository>();
	
	static {
		//adding a sample repository
		Credential nigelCredential = new Credential("AKIAJQ4UK2TUFDOI3ZQA", "pODzu2s8SfmgQE1LRRANS32zLLQjWqOBHxvoOcMP").encrypt();
		Repository repo = new Repository("repo1", "repo1 description", nigelCredential, URI.create("https://s3.amazonaws.com"), "n3phele-test", "S3", null, true);
		repositoryMap.put(1, repo);
		
		//add additional repositories
	}
	public static final Repository getRepositoryById(Integer repoId) {
		
		if (repositoryMap.get(repoId) != null){
			return repositoryMap.get(repoId);
		}
		return null;
	}

}
