package n3phele.service.model.store;

import java.net.URI;
import java.util.HashMap;

import n3phele.service.model.core.Credential;
import n3phele.service.model.repository.Repository;

public class RepositoryStore {
	
	private static HashMap<Integer, Repository> repositoryMap = new HashMap<Integer, Repository>();
	
	static {
		//adding a sample repository
		Credential nigelCredential = new Credential("nigel.cook@hp.com:11520587032204", "changeme").encrypt();
		Repository repo = new Repository("repo1", "repo1 description", nigelCredential, URI.create("https://region-a.geo-1.identity.hpcloudsvc.com:35357"), "ddd", "Swift", null, false);
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
