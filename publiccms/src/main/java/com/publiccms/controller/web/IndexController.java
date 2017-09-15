package com.publiccms.controller.web;

import static com.publiccms.common.api.Config.CONFIG_CODE_SITE;
import static com.publiccms.common.constants.CommonConstants.getDefaultPage;
import static com.publiccms.logic.component.config.LoginConfigComponent.CONFIG_LOGIN_PATH;
import static com.sanluan.common.tools.RequestUtils.getEncodePath;
import static org.apache.commons.lang3.ArrayUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.split;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.util.UrlPathHelper;

import com.publiccms.common.base.AbstractController;
import com.publiccms.entities.sys.SysDomain;
import com.publiccms.entities.sys.SysSite;
import com.publiccms.logic.component.config.ConfigComponent;
import com.publiccms.logic.component.template.MetadataComponent;
import com.publiccms.logic.component.template.TemplateCacheComponent;
import com.publiccms.views.pojo.CmsPageMetadata;
import com.sanluan.common.servlet.PageNotFoundException;

/**
 * 
 * IndexController 统一分发Controller
 * 做统一模版分发，和IndexAdminController一起做，数据读取由FreeMarker指令扩展来处理
 */
@Controller
public class IndexController extends AbstractController {
    @Autowired
    private MetadataComponent metadataComponent;
    @Autowired
    private TemplateCacheComponent templateCacheComponent;
    @Autowired
    private ConfigComponent configComponent;
    private UrlPathHelper urlPathHelper = new UrlPathHelper();

    /**
     * 页面请求统一分发
     * 
     * @param request
     * @param response
     * @param model
     * @return
     * @throws PageNotFoundException
     */
    @RequestMapping({ SEPARATOR, "/**" })
    public String page(@RequestBody(required = false) String body, HttpServletRequest request, ModelMap model)
            throws PageNotFoundException {
        String requestPath = urlPathHelper.getLookupPathForRequest(request);
        if (requestPath.endsWith(SEPARATOR)) {
            requestPath += getDefaultPage();
        }
        SysDomain domain = getDomain(request);
        SysSite site = getSite(request);
        String fullRequestPath = siteComponent.getViewNamePreffix(site, domain) + requestPath;
        String templatePath = siteComponent.getWebTemplateFilePath() + fullRequestPath;
        CmsPageMetadata metadata = metadataComponent.getTemplateMetadata(templatePath, true);
        if (null != metadata) {
            if (metadata.isUseDynamic()) {
                if (metadata.isNeedLogin() && null == getUserFromSession(request.getSession())) {
                    Map<String, String> config = configComponent.getConfigData(site.getId(), CONFIG_CODE_SITE);
                    String loginPath = config.get(CONFIG_LOGIN_PATH);
                    StringBuilder sb = new StringBuilder(REDIRECT);
                    if (notEmpty(loginPath)) {
                        return sb.append(loginPath).append("?returnUrl=")
                                .append(getEncodePath(requestPath, request.getQueryString())).toString();
                    } else {
                        return sb.append(site.getDynamicPath()).toString();
                    }
                }
                model.put("metadata", metadata);
                if (metadata.isNeedBody()) {
                    model.put("body", body);
                }
                if (notEmpty(metadata.getAcceptParamters())) {
                    billingRequestParamtersToModel(request, metadata.getAcceptParamters(), model);
                }
                if (notEmpty(metadata.getCacheTime()) && 0 < metadata.getCacheTime()) {
                    int cacheMillisTime = metadata.getCacheTime() * 1000;
                    String cacheControl = request.getHeader("Cache-Control");
                    String pragma = request.getHeader("Pragma");
                    if (notEmpty(cacheControl) && "no-cache".equalsIgnoreCase(cacheControl)
                            || notEmpty(pragma) && "no-cache".equalsIgnoreCase(pragma)) {
                        cacheMillisTime = 0;
                    }
                    return templateCacheComponent.getCachedPath(requestPath, fullRequestPath, cacheMillisTime,
                            metadata.getAcceptParamters(), request, model);
                }
            } else {
                throw new PageNotFoundException(requestPath);
            }
        }
        return requestPath;
    }

    private void billingRequestParamtersToModel(HttpServletRequest request, String acceptParamters, ModelMap model) {
        for (String paramterName : split(acceptParamters, COMMA_DELIMITED)) {
            String[] values = request.getParameterValues(paramterName);
            if (isNotEmpty(values)) {
                if (1 < values.length) {
                    model.put(paramterName, values);
                } else {
                    model.put(paramterName, values[0]);
                }
            }
        }
    }
}
