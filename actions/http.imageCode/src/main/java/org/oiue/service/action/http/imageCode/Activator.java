package org.oiue.service.action.http.imageCode;

import java.util.Dictionary;

import org.oiue.service.action.api.ActionService;
import org.oiue.service.log.LogService;
import org.oiue.service.log.Logger;
import org.oiue.service.osgi.FrameActivator;
import org.oiue.service.osgi.MulitServiceTrackerCustomizer;
import org.osgi.service.http.HttpService;

public class Activator extends FrameActivator {

    @Override
    public void start() throws Exception {
        this.start(new MulitServiceTrackerCustomizer() {
            private String url = getProperty("org.oiue.service.action.http.root") + "/CheckCodeImage";
            private HttpService httpService;
            private ImageCodeServlet imageCode;

            @Override
            public void removedService() {
                httpService.unregister(url);
            }

            @SuppressWarnings("unused")
            @Override
            public void addingService() {
                httpService = getService(HttpService.class);
                LogService logService = getService(LogService.class);
                ActionService actionService = getService(ActionService.class);

                imageCode = new ImageCodeServlet(logService);
                Logger log = logService.getLogger(this.getClass());
                log.debug("绑定url：" + url);
                try {
                    httpService.registerServlet(url, imageCode, null, null);
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
            }

            @Override
            public void updated(Dictionary<String, ?> props) {
                imageCode.updated(props);
            }
        }, HttpService.class, ActionService.class, LogService.class);
    }

    @Override
    public void stop() throws Exception {}
}
