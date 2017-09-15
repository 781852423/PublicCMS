package com.publiccms.controller.admin;

import static com.publiccms.common.constants.CommonConstants.getDefaultPage;

import javax.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.util.UrlPathHelper;

import com.publiccms.common.base.AbstractController;

/**
 * 
 * IndexAdminController 统一分发Controller
 * 主要做模版分发，和IndexController一起做统一模版分发，数据读取操作由FreeMarker指令扩展来处理
 */
@Controller
public class IndexAdminController extends AbstractController {
    private UrlPathHelper urlPathHelper = new UrlPathHelper();

    /**
     * 页面请求统一分发
     * 
     * @param request
     * @return
     */
    @RequestMapping("/**")
    public String page(HttpServletRequest request) {
        String path = urlPathHelper.getLookupPathForRequest(request);
        if (notEmpty(path)) {
            if (SEPARATOR.equals(path) || path.endsWith(SEPARATOR)) {
                path += getDefaultPage();
            }
            int index = path.lastIndexOf(DOT);
            path = path.substring(path.indexOf(SEPARATOR) > 0 ? 0 : 1, index > -1 ? index : path.length());
        }
        return path;
    }
}