package io.openshift.launchpad.ui.booster;

import java.util.List;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.core.HttpHeaders;

import io.openshift.launchpad.MissionControl;
import org.jboss.forge.addon.ui.context.UIContext;

/**
 * Get a list of project that the user already has
 */
@ApplicationScoped
public class ProjectList {
    @Inject
    private MissionControl missionControlFacade;

    public List<String> getProjects(UIContext context)
    {
        Map<Object, Object> attributeMap = context.getAttributeMap();
        List<String> authList = (List<String>) attributeMap.get(HttpHeaders.AUTHORIZATION);
        String authHeader = (authList == null || authList.isEmpty()) ? null : authList.get(0);
        return missionControlFacade.getProjects(authHeader);
    }
}
