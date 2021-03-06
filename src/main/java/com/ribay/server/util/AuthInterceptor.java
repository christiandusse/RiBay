package com.ribay.server.util;

import com.ribay.server.repository.AuthenticationRepository;
import com.ribay.server.util.session.SessionData;
import com.ribay.server.util.session.SessionUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
public class AuthInterceptor extends HandlerInterceptorAdapter {

    private static final Logger logger = LoggerFactory.getLogger(AuthInterceptor.class);

    @Autowired
    private RequestScopeData requestData;

    @Autowired
    private AuthenticationRepository authRepository;

    @Autowired
    private SessionUtil sessionUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) throws Exception {

        // get or generate session
        SessionData session = sessionUtil.readSession(request);

        if (session.isNew()) {
            // if created session. make sure that a cookie is generated
            sessionUtil.writeSession(response, session);
        }

        // set session data of request scope data so that the actual rest services can use that
        requestData.init(request, response, session);

        return super.preHandle(request, response, handler);
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        super.postHandle(request, response, handler, modelAndView);

        if ((request.getRequestURI() != null) && request.getRequestURI().startsWith("/image/")) {
            // do not save last session access when loading images
            // we do this because the client will load multiple images at once ...
            // ... and while performing an other action that will save the last session access,
            // so we do not have to set the last session access multiple times at once.
        } else {
            try {
                String sessionId = requestData.getSessionId();
                authRepository.saveLastAccess(sessionId);
            } catch (Exception e) {
                logger.error("Was not able to save last access time for session", e);
            }
        }
    }

}
