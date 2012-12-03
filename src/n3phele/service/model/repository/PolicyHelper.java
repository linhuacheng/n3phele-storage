/**
 * @author Nigel Cook
 *
 * (C) Copyright 2010-2011. All rights reserved.
 */
package n3phele.service.model.repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.amazonaws.auth.policy.Action;
import com.amazonaws.auth.policy.Condition;
import com.amazonaws.auth.policy.Policy;
import com.amazonaws.auth.policy.Principal;
import com.amazonaws.auth.policy.Resource;
import com.amazonaws.auth.policy.Statement;
import com.amazonaws.auth.policy.Statement.Effect;
import com.amazonaws.auth.policy.actions.S3Actions;
import com.amazonaws.util.json.JSONArray;
import com.amazonaws.util.json.JSONException;
import com.amazonaws.util.json.JSONObject;

public class PolicyHelper {
	private static Logger log = Logger.getLogger(PolicyHelper.class.getName());  

	public static Policy parse(String s) {
		Policy result = null;
		try {
			JSONObject jo = new JSONObject(s);
			String id = jo.getString("Id");
			result = new Policy(id);
			JSONArray statementArray = jo.getJSONArray("Statement");
			List<Statement> statements = new ArrayList<Statement>();
			if(statementArray != null) {
				for(int i=0; i < statementArray.length(); i++) {
					JSONObject js = statementArray.getJSONObject(i);
					Statement statement = new Statement(Effect.valueOf((js.getString("Effect"))));
					String sid = js.getString("Sid");
					statement.setId(sid);
					if(js.has("Action"))
							statement.setActions(parseActions(js.get("Action")));
					if(js.has("Resource"))
						statement.setResources(parseResources(js.get("Resource")));
					if(js.has("Principal"))
						statement.setPrincipals(parsePrincipal(js.get("Principal")));
					if(js.has("Condition"))
						statement.setConditions(parseCondition(js.get("Condition")));
					statements.add(statement);
				}
				result.setStatements(statements);
			}
		} catch (JSONException e) {
			log.log(Level.SEVERE, "error parsing policy", e);
		}
		return result;
	}

	private static List<Condition> parseCondition(Object o) {
		List<Condition> result = new ArrayList<Condition>();
		
		return result;
	}

	private static List<Principal> parsePrincipal(Object o) {
		List<Principal> result = new ArrayList<Principal>();
		if(o instanceof JSONArray) {
			JSONArray jsonArray = (JSONArray)o;
			for(int i=0; i < jsonArray.length(); i++) {
				try {
					JSONObject jo = jsonArray.getJSONObject(i);
					Principal principal = new Principal(jo.getString("AWS"));
					result.add(principal);
				} catch (JSONException e) {
					
				}
			}
		} else if(o instanceof JSONObject) {
			try {
				JSONObject jo = (JSONObject)o;
				Principal principal;
				principal = new Principal(jo.getString("AWS"));
				result.add(principal);
			} catch (JSONException e) {
;
			}

		}
		return result;
	}

	private static List<Resource> parseResources(Object o) {
		List<Resource> result = new ArrayList<Resource>();
		if(o instanceof JSONArray) {
			JSONArray jsonArray = (JSONArray)o;
			for(int i=0; i < jsonArray.length(); i++) {
				try {
					String resource = (String) jsonArray.get(i);
					Resource r = new Resource(resource);
					result.add(r);
				} catch (JSONException e) {
					
				}
			}
		} else if(o instanceof String){
			result.add(new Resource((String)o));
		}
		return result;
	}

	private static Collection<Action> parseActions(Object o) {
		List<Action> result = new ArrayList<Action>();
		if(o instanceof JSONArray) {
			JSONArray jsonArray = (JSONArray)o;
			for(int i=0; i < jsonArray.length(); i++) {
				try {
					String action = (String) jsonArray.get(i);
					Action r = to(action);
					result.add(r);
				} catch (JSONException e) {
					
				}
			}
		} else if(o instanceof String) {
			result.add(to((String)o));
		}
		return result;
	}
	
	private static Map<String, S3Actions> map = null;
	private static S3Actions to(String value) {
		if(map == null) {
			map = new HashMap<String, S3Actions>();
			for(S3Actions x : S3Actions.values()) {
				map.put(x.getActionName(), x);
			}
		}
		return map.get(value);
	}
}
