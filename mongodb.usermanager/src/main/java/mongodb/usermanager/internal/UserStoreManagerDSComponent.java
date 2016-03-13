package mongodb.usermanager.internal;

import org.apache.commons.logging.LogFactory;
import org.apache.juli.logging.Log;
import org.osgi.service.component.ComponentContext;
import org.wso2.carbon.user.core.service.RealmService;

import mongodb.usermanager.UserStoreManager;

/**
 * @scr.component name="mongodb.usermanager.dscomponent" immediate=true
 * @scr.reference name="realm.service"
 * interface="org.wso2.carbon.user.core.service.RealmService"cardinality="1..1"
 * policy="dynamic" bind="setRealmService" unbind="unsetRealmService"
 */public class UserStoreManagerDSComponent {

	private static Log log = (Log) LogFactory.getLog(UserStoreManagerDSComponent.class);
    private static RealmService realmService;

    protected void activate(ComponentContext ctxt) {

        UserStoreManager customUserStoreManager = new UserStoreManager();
        ctxt.getBundleContext().registerService(UserStoreManager.class.getName(), customUserStoreManager, null);
        log.info("MongoDB User Store bundle activated successfully..");
    }

    protected void deactivate(ComponentContext ctxt) {
        if (log.isDebugEnabled()) {
            log.debug("MongoDB User Store Manager is deactivated ");
        }
    }


    protected void setRealmService(RealmService rlmService) {
          realmService = rlmService;
    }

    protected void unsetRealmService(RealmService realmService) {
        realmService = null;
    }
}
