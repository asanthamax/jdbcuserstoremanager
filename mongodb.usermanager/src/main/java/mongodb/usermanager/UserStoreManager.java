package mongodb.usermanager;


import java.util.*;

import javax.activation.DataSource;

import org.apache.commons.logging.LogFactory;
import org.apache.juli.logging.Log;
import org.jasypt.util.password.StrongPasswordEncryptor;
import org.wso2.carbon.user.api.*;
import org.wso2.carbon.user.api.Properties;
import org.wso2.carbon.user.core.claim.ClaimManager;
import org.wso2.carbon.user.core.UserStoreConfigConstants;
import org.wso2.carbon.user.core.UserStoreException;
import org.wso2.carbon.user.core.common.AbstractUserStoreManager;
import org.wso2.carbon.user.core.common.RoleContext;
import org.wso2.carbon.user.core.jdbc.JDBCUserStoreConstants;
import org.wso2.carbon.user.core.jdbc.JDBCUserStoreManager;
import org.wso2.carbon.user.core.tenant.Tenant;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.WriteConcern;

public class UserStoreManager extends AbstractUserStoreManager{

    private DBCollection collection = null;
    protected int tenantId;
    protected UserRealm jdbcRealm;
    protected ProfileConfigurationManager profileManager; 
	private static Log log = (Log) LogFactory.getLog(UserStoreManager.class);
	private static StrongPasswordEncryptor passwordEncryptor = new StrongPasswordEncryptor();
	
	public UserStoreManager(RealmConfiguration realmConfig, int tenantId) {
		
		if(log.isDebugEnabled())
		{
			log.debug("Started "+System.currentTimeMillis());
		}
        this.realmConfig = realmConfig;
        this.tenantId = tenantId;
    }
	public UserStoreManager()
	{
		MongoClient client = new MongoClient(UserStoreConfigConstants.connectionURL);
		if(log.isDebugEnabled())
		{
			log.debug("Started "+System.currentTimeMillis());
		}
		DB db = (DB)client.getDatabase("test");
		if(db!=null)
		{
			collection = db.getCollection("UM_ROLE");
			DBObject object = new BasicDBObject("UM_ROLE_NAME","Admin");
			this.tenantId = Integer.parseInt(collection.findOne(object).get("UM_TENANT_ID").toString());
		}
		else{
			
			log.error("No Available Database found!");
		}
		
	}
	public UserStoreManager(RealmConfiguration realmConfig, ClaimManager
	           claimManager, ProfileConfigurationManager profileManager, UserRealm realm, Integer tenantId)
	{
		this.realmConfig = realmConfig;
		this.claimManager = claimManager;
		this.tenantId = tenantId;
		this.jdbcRealm = realm;
		this.profileManager = profileManager;
		if(log.isDebugEnabled())
		{
			log.debug("Started "+System.currentTimeMillis());
		}
	}
	
	protected DB getDBConnection() throws UserStoreException
	{
		MongoClient mongoClient = new MongoClient(UserStoreConfigConstants.connectionURL);
		mongoClient.setWriteConcern(WriteConcern.JOURNALED);
		DB db = (DB) mongoClient.getDatabase("test");
		if(db == null)
		{
			throw new UserStoreException("Error While make Connection to DB");
		}
		else{
			return db;
		}
	}
	
	public String[] getAllProfileNames() throws UserStoreException {
		// TODO Auto-generated method stub
		DB db= getDBConnection();
		String[] profileNames=null;
		log.info("DB Connection Made Successfully");
		collection = db.getCollection("UM_USER_ATTRIBUTE");
		DBObject objet = new BasicDBObject("UM_TENANT_ID",tenantId);
		List list = collection.distinct("UM_PROFILE_ID", objet);
		if(!list.isEmpty())
		{
			for(int i=0;i<list.size();i++)
			{
				profileNames[i]=list.get(i).toString();
			}
			return extracted(profileNames);
		}
		else{
				
			throw new UserStoreException("User Profile is Empty");
		}
			
		
		
	}

	public Date getPasswordExpirationTime(String arg0) throws UserStoreException {
		// TODO Auto-generated method stub
		return null;
	}

	public String[] getProfileNames(String userName) throws UserStoreException {
		// TODO Auto-generated method stub
		DB db=getDBConnection();
		String[] profileNames = null;
		log.info("retrieving Profile Names...");
		collection = db.getCollection("UM_USER_ATTRIBUTE");
		DBCollection collection2 = db.getCollection("UM_USER");
		DBObject object = new BasicDBObject("UM_USER_NAME",userName).append("UM_TENANT_ID", tenantId); 
		List list = collection2.distinct("UM_USER_ID",object);
		if(!list.isEmpty())
		{
			for(int i=0;i<list.size();i++)
			{
				object = new BasicDBObject("UM_USER_ID",list.get(i).toString()).append("UM_TENANT_ID", tenantId);
				List profiles = collection.distinct("UM_PROFILE_ID",object);
				if(!profiles.isEmpty())
				{
					for(int j=0;j<profiles.size();j++)
					{
						profileNames[j] = profiles.get(j).toString();
					}
				}
			}
			return extracted(profileNames);
		}
		else{
			
			throw new UserStoreException("User not exsists");
		}
	}
	
	private String[] extracted(String[] profileNames) throws UserStoreException {
		if(profileNames == null)
		{
			throw new UserStoreException("No Any Profile found!");
			
		}
		return profileNames;
	}

	public Map<String, String> getProperties(Tenant tenant) throws UserStoreException {

		Map<String, String> properties = new HashMap<String, String>();

			properties.put("givenName", "CWA-test");
			properties.put("mail", "cwa-test@test.com");

		return properties;
	}

	public RealmConfiguration getRealmConfiguration() {
		// TODO Auto-generated method stub
		return null;
	}

	public int getTenantId() throws UserStoreException {
		// TODO Auto-generated method stub
		return this.tenantId;
	}

	public int getTenantId(String userName) throws UserStoreException {
		// TODO Auto-generated method stub
		if(this.tenantId != 0)
		{
			throw new UserStoreException("Not Allowed to Perform this operation");
		}
		DB db = getDBConnection();
		collection = db.getCollection("UM_USER");
		DBObject object = new BasicDBObject("UM_USER_NAME", userName);
		Object tenants = collection.findOne(object).get("UM_TENANT_ID");
		if(tenants == null)
		{
			throw new UserStoreException("User Not Exsits...");
		}
		this.tenantId = Integer.parseInt(tenants.toString());
		return this.tenantId;
	}

	public int getUserId(String userName) throws UserStoreException {
		// TODO Auto-generated method stub
		DB db = getDBConnection();
		collection = db.getCollection("UM_USER");
		DBObject object = new BasicDBObject("UM_USER_NAME",userName);
		Object user_id = collection.findOne(object).get("UM_ID");
		if(user_id == null)
		{
			throw new UserStoreException("User not exsists");
		}
		return Integer.parseInt(user_id.toString());
	}

	public boolean isBulkImportSupported() throws UserStoreException {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean isReadOnly() throws UserStoreException {
		// TODO Auto-generated method stub
		return false;
	}

	public void addRememberMe(String arg0, String arg1) throws org.wso2.carbon.user.api.UserStoreException {
		// TODO Auto-generated method stub
		
	}

	public Properties getDefaultUserStoreProperties() {


		Properties defaultUserStoreProperties;

		Properties properties = new Properties();
		properties.setMandatoryProperties(MongoDBUserStoreConstants.CUSTOM_UM_MANDATORY_PROPERTIES.toArray(
				new Property[MongoDBUserStoreConstants.CUSTOM_UM_MANDATORY_PROPERTIES.size()]));
		properties.setOptionalProperties(MongoDBUserStoreConstants.CUSTOM_UM_OPTIONAL_PROPERTIES.toArray
				(new Property[MongoDBUserStoreConstants.CUSTOM_UM_OPTIONAL_PROPERTIES.size()]));
		properties.setAdvancedProperties(MongoDBUserStoreConstants.CUSTOM_UM_ADVANCED_PROPERTIES.toArray
				(new Property[MongoDBUserStoreConstants.CUSTOM_UM_ADVANCED_PROPERTIES.size()]));
		return properties;

	}

	private static <T> T[] concat(T[] first, T[] second) {
		T[] result = Arrays.copyOf(first, first.length + second.length);
		System.arraycopy(second, 0, result, first.length, second.length);
		return result;
	}

	public Map<String, String> getProperties(org.wso2.carbon.user.api.Tenant arg0)
			throws org.wso2.carbon.user.api.UserStoreException {
		// TODO Auto-generated method stub
		return null;
	}

	public boolean isMultipleProfilesAllowed() {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean isValidRememberMeToken(String arg0, String arg1) throws org.wso2.carbon.user.api.UserStoreException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	protected RoleContext createRoleContext(String arg0) throws UserStoreException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected void doAddRole(String arg0, String[] arg1, boolean arg2) throws UserStoreException {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void doAddUser(String arg0, Object arg1, String[] arg2, Map<String, String> arg3, String arg4,
			boolean arg5) throws UserStoreException {
		// TODO Auto-generated method stub
		
	}

	
	@Override
	protected boolean doAuthenticate(String arg0, Object arg1) throws UserStoreException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	protected boolean doCheckExistingRole(String arg0) throws UserStoreException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	protected boolean doCheckExistingUser(String arg0) throws UserStoreException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean doCheckIsUserInRole(String arg0, String arg1) throws UserStoreException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	protected void doDeleteRole(String arg0) throws UserStoreException {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void doDeleteUser(String arg0) throws UserStoreException {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void doDeleteUserClaimValue(String arg0, String arg1, String arg2) throws UserStoreException {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void doDeleteUserClaimValues(String arg0, String[] arg1, String arg2) throws UserStoreException {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected String[] doGetDisplayNamesForInternalRole(String[] arg0) throws UserStoreException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected String[] doGetExternalRoleListOfUser(String arg0, String arg1) throws UserStoreException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected String[] doGetRoleNames(String arg0, int arg1) throws UserStoreException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected String[] doGetSharedRoleListOfUser(String arg0, String arg1, String arg2) throws UserStoreException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected String[] doGetSharedRoleNames(String arg0, String arg1, int arg2) throws UserStoreException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected String[] doGetUserListOfRole(String arg0, String arg1) throws UserStoreException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected String[] doListUsers(String arg0, int arg1) throws UserStoreException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected void doSetUserClaimValue(String arg0, String arg1, String arg2, String arg3) throws UserStoreException {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void doSetUserClaimValues(String arg0, Map<String, String> arg1, String arg2) throws UserStoreException {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void doUpdateCredential(String arg0, Object arg1, Object arg2) throws UserStoreException {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void doUpdateCredentialByAdmin(String arg0, Object arg1) throws UserStoreException {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void doUpdateRoleListOfUser(String arg0, String[] arg1, String[] arg2) throws UserStoreException {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void doUpdateRoleName(String arg0, String arg1) throws UserStoreException {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void doUpdateUserListOfRole(String arg0, String[] arg1, String[] arg2) throws UserStoreException {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected String[] getUserListFromProperties(String arg0, String arg1, String arg2) throws UserStoreException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected Map<String, String> getUserPropertyValues(String arg0, String[] arg1, String arg2)
			throws UserStoreException {
		// TODO Auto-generated method stub
		return null;
	}

}
