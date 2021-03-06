package org.oiue.service.auth.impl;

import java.io.Serializable;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;

import org.oiue.service.auth.AuthService;
import org.oiue.service.auth.AuthServiceManager;
import org.oiue.service.log.LogService;
import org.oiue.service.log.Logger;
import org.oiue.service.online.Online;
import org.oiue.tools.string.StringUtil;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;

@SuppressWarnings({ "rawtypes", "serial" })
public class AuthServiceManagerImpl implements AuthServiceManager, ManagedService, Serializable {
    private Logger logger;
    private String login_type = "type";

    private Map<String, AuthService> auths = new HashMap<>();

    public AuthServiceManagerImpl(LogService logService) {
        logger = logService.getLogger(getClass());
    }

    @Override
    public Online login(Map per) {

        String type = (String) per.remove(login_type);
        
        if(StringUtil.isEmptys(type)){
            String msg ="the key["+login_type+"] con't null or empty!";
            logger.error(msg+":"+per);
            throw new RuntimeException(msg);
        }
        AuthService auth = auths.get(type);

        if(auth==null){
            String msg ="AuthService the key["+type+"] con't find!";
            logger.error(msg+":"+per);
            throw new RuntimeException(msg);
        }
        return auth.login(per);
    }

    @Override
    public boolean logout(Map per) {

        String type = (String) per.remove(login_type);
        
        if(!StringUtil.isEmptys(type)){
            AuthService auth = auths.get(type);
            if(auth!=null)
                auth.logout(per);
        }
        return true;
    }


    @Override
    public boolean registerAuthService(String name, AuthService auth) {
        if (auths.containsKey(name)) {
            return false;
        } else
            auths.put(name, auth);
        return true;
    }

    @Override
    public boolean unRegisterAuthService(String name) {
        if (auths.containsKey(name)) {
            auths.remove(name);
            return true;
        } else
            return false;
    }

    @Override
    public void updated(Dictionary<String, ?> props) throws ConfigurationException {
        String login_type = props.get("loginType")+"";
        if(!StringUtil.isEmptys(login_type)){
            this.login_type=login_type;
        }
    }

    @Override
    public void unregister() {
        // TODO Auto-generated method stub
        
    }

}
