/**
 * 
 */
package org.oiue.service.online.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.text.html.CSS;

import org.oiue.service.cache.CacheServiceManager;
import org.oiue.service.cache.Type;
import org.oiue.service.log.LogService;
import org.oiue.service.log.Logger;
import org.oiue.service.online.OfflineHandler;
import org.oiue.service.online.Online;
import org.oiue.service.online.OnlineHandler;
import org.oiue.service.online.OnlineService;
import org.oiue.tools.string.StringUtil;

/**
 * @author Every
 * 
 */
@SuppressWarnings({ "rawtypes", "unchecked", "serial" })
public class OnlineServiceImpl implements OnlineService {

    public static final String _SYSTEM_ONLINE = "system.online";
    public static String _GLOBAL_CACHE = "redis";

    public static boolean clearClient = false;
    public static clearClient ccs = null;

    private Logger logger;
    private CacheServiceManager cache;
    private LogService logService;

    // private String ip = "";

    private int online_timeout = 600000;

    public void updated(Dictionary props) {
        logger.debug("updated config for OnlineServiceImpl" + props);
        try {
            clearClient = StringUtil.isTrue(props.get("online.clearClient") + "");
            if (clearClient) {
                if (ccs == null) {
                    ccs = new clearClient(logService);
                    new Thread(ccs,CSS.class.getName()).start();
                }
            } else {
                ccs = null;
            }
        } catch (Throwable e) {
            logger.error("updateConfigure is error:" + e.getMessage(), e);
        }
        try {
            String GLOBAL_CACHE = props.get("online.globalCache") + "";
            if (!StringUtil.isEmptys(GLOBAL_CACHE)) {
                _GLOBAL_CACHE = GLOBAL_CACHE;
            }
        } catch (Throwable e) {
            logger.error("updateConfigure[online.globalCache] is error:" + e.getMessage(), e);
        }
        try {
            String timeout = props.get("online.timeout") + "";
            if (!StringUtil.isEmptys(timeout)) {
                online_timeout = Integer.valueOf(timeout) * 1000;
            }
        } catch (Throwable e) {
            logger.error("updateConfigure[online.timeout] is error:" + e.getMessage(), e);
        }

    }

    public OnlineServiceImpl(LogService logService, CacheServiceManager cache) {
        try {
            logger = logService.getLogger(this.getClass());
            this.logService = logService;
            this.cache = cache;
        } catch (Throwable e) {
            logger.error("OnlineServiceImpl is error:" + e.getMessage(), e);
        }
    }

    @Override
    public boolean putOnline(String token, Online online) {
        // if(StringUtil.isEmptys(ip)){
        // throw new RuntimeException("服务尚未初始化！");
        // }
        online.setLastTime(System.currentTimeMillis());
        this.cache.put(_SYSTEM_ONLINE, token, online, Type.ONE);
        // this.cache.getCacheService(_GLOBAL_CACHE).put(token, ip, Type.ONE);

        return true;
    }

    @Override
    public Online getOnlineByToken(String token) {
        return (Online) cache.get(_SYSTEM_ONLINE, token);
    }

    @Override
    public boolean isOnlineByToken(String token) {
        // String lip = this.cache.getCacheService(_GLOBAL_CACHE).get(token)+"";
        // if (ip.equals(lip)) {
        Online online = (Online) cache.get(_SYSTEM_ONLINE, token);
        if (online == null) {
            return false;
        } else {
            online.setLastTime(System.currentTimeMillis());
            return true;
        }
        // }else{
        // // Session session = onlineClient.get(lip);
        // return true;
        // }
    }

    @Override
    public Collection<Online> getOnlines() {
        return ((Map) cache.get(_SYSTEM_ONLINE)).values();
    }

    @Override
    public List<Online> getOnlinesByUserID(String userId) {

        List rtnList = new ArrayList();
        Object tokens = cache.get(_SYSTEM_ONLINE);
        if (tokens instanceof Map) {
            Map<String, Online> tokenm = (Map<String, Online>) tokens;
            for (Iterator iterator = tokenm.values().iterator(); iterator.hasNext();) {
                Online online = (Online) iterator.next();
                if (online != null && userId.equals(online.getUser_id())) {
                    rtnList.add(online);
                }
            }
        }
        return rtnList;
    }

    @Override
    public boolean removeOnlineByToken(String token) {
        try {
            if (token == null)
                return false;
            if (logger.isDebugEnabled())
                logger.debug("removeOnline :" + token);

            if (offline.size() > 0) {
                Online online = (Online) cache.get(_SYSTEM_ONLINE, token);
                if (online != null)
                    for (Iterator iterator = offline.values().iterator(); iterator.hasNext();) {
                        OfflineHandler handler = (OfflineHandler) iterator.next();
                        try {
                            handler.logout(online);
                        } catch (Throwable e) {
                            logger.error(handler + "removeOnline [" + token + "] is error:" + e.getMessage(), e);
                        }
                    }
            }

            cache.delete(_SYSTEM_ONLINE, token);

            // cache.getCacheService(_GLOBAL_CACHE).delete(_SYSTEM_ONLINE, token);

            return true;
        } catch (Throwable e) {
            logger.error("removeOnline [" + token + "] is error:" + e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean removeOnlineByUserId(String userId) {
        try {
            if (logger.isDebugEnabled())
                logger.debug("removeOnline :" + userId);

            List<Online> onlines = this.getOnlinesByUserID(userId);
            for (Online online : onlines) {
                if (online != null) {
                    this.removeOnlineByToken(online.getToken());
                }
            }
            return true;
        } catch (Throwable e) {
            logger.error("removeOnline [" + userId + "] is error:" + e.getMessage(), e);
            return false;
        }
    }

    public class clearClient implements Runnable {
        private Logger logger;

        public clearClient(LogService logService) {
            logger = logService.getLogger(this.getClass());
        }

        @Override
        public void run() {
            if (logger.isDebugEnabled()) {
                logger.debug("clearClient start");
            }
            while (clearClient) {
                try {
                    Long timeout = System.currentTimeMillis() - online_timeout;
                    List<String> logout = new ArrayList<>();
                    Object tokens = cache.get(_SYSTEM_ONLINE);
                    if (tokens instanceof Map) {
                        Map<String, Online> tokenm = (Map<String, Online>) tokens;
                        for (Iterator iterator = tokenm.values().iterator(); iterator.hasNext();) {
                            Online online = (Online) iterator.next();
                            if (online != null && online.getLastTime() < timeout) {
                                logout.add(online.getToken());
                            }
                        }
                    }
                    if (logger.isDebugEnabled()) {
                        logger.debug("clean user :" + logout);
                    }
                    for (String token : logout) {
                        removeOnlineByToken(token);
                    }

                } catch (Throwable e) {
                    logger.error("removeOnline error :" + e.getMessage(), e);
                }

                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    logger.error(e.getMessage(), e);
                }
            }

        }

    }

    private Map<String, OfflineHandler> offline = new HashMap<String, OfflineHandler>();
    private Map<Integer, String> offlineSort = new TreeMap<Integer, String>();
    private Map<String, OnlineHandler> online = new HashMap<String, OnlineHandler>();
    private Map<Integer, String> onlineSort = new TreeMap<Integer, String>();

    @Override
    public synchronized boolean registerOfflineHandler(String name, OfflineHandler handler, int index) {
        if (offlineSort.get(index) != null) {
            throw new RuntimeException("index conflict! name=" + name + ", old index is " + offlineSort.get(index));
        }
        if (offline.get(name) == null) {
            offline.put(name, handler);
            offlineSort.put(index, name);

            Map<String, OfflineHandler> offlineTemp = new LinkedHashMap<String, OfflineHandler>();
            for (Iterator iterator = offlineSort.values().iterator(); iterator.hasNext();) {
                String value = (String) iterator.next();
                offlineTemp.put(value, offline.get(value));
            }
            offline = offlineTemp;
            return true;
        }
        return false;
    }

    @Override
    public synchronized boolean registerOnlineHandler(String name, OnlineHandler handler, int index) {
        if (onlineSort.get(index) != null) {
            throw new RuntimeException("index conflict! name=" + name + ", old index is " + onlineSort.get(index));
        }
        if (online.get(name) == null) {
            online.put(name, handler);
            onlineSort.put(index, name);

            Map<String, OnlineHandler> onlineTemp = new LinkedHashMap<String, OnlineHandler>();
            for (Iterator iterator = offlineSort.values().iterator(); iterator.hasNext();) {
                String value = (String) iterator.next();
                onlineTemp.put(value, online.get(value));
            }
            online = onlineTemp;
            return true;
        }
        return false;
    }

    @Override
    public synchronized boolean unRegisterOfflineHandler(String name) {
        offline.remove(name);
        for (Iterator iterator = offlineSort.values().iterator(); iterator.hasNext();) {
            String names = (String) iterator.next();
            if (name.equals(names))
                iterator.remove();
        }
        return true;
    }

    @Override
    public synchronized boolean unRegisterOnlineHandler(String name) {
        online.remove(name);
        for (Iterator iterator = onlineSort.values().iterator(); iterator.hasNext();) {
            String names = (String) iterator.next();
            if (name.equals(names))
                iterator.remove();
        }
        return true;
    }

}
