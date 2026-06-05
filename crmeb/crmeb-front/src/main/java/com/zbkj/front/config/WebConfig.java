package com.zbkj.front.config;

import com.zbkj.common.config.CrmebConfig;
import com.zbkj.common.constants.UploadConstants;
import com.zbkj.common.interceptor.SwaggerInterceptor;
import com.zbkj.front.filter.ResponseFilter;
import com.zbkj.front.interceptor.FrontTokenInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.handler.MappedInterceptor;

/**
 * token验证拦截�?
 * +----------------------------------------------------------------------
 * | CRMEB [ CRMEB赋能开发者，助力企业发展 ]
 * +----------------------------------------------------------------------
 * | Copyright (c) 2016~2025 https://www.crmeb.com All rights reserved.
 * +----------------------------------------------------------------------
 * | Licensed CRMEB并不是自由软件，未经许可不能去掉CRMEB相关版权
 * +----------------------------------------------------------------------
 * | Author: CRMEB Team <admin@crmeb.com>
 * +----------------------------------------------------------------------
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    CrmebConfig crmebConfig;

    // 这里使用一个Bean为的是可以在拦截器中自由注入，也可以在拦截器中使用SpringUtil.getBean 获取
    // 但是觉得这样更优�?

    @Bean
    public HandlerInterceptor frontTokenInterceptor(){
        return new FrontTokenInterceptor();
    }

    @Bean
    public ResponseFilter responseFilter(){ return new ResponseFilter(); }

    @Value("${swagger.basic.username}")
    private String username;
    @Value("${swagger.basic.password}")
    private String password;
    @Value("${swagger.basic.check}")
    private Boolean check;


    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        //添加token拦截�?
        //addPathPatterns添加需要拦截的命名空间�?
        //excludePathPatterns添加排除拦截命名空间

        //前端用户登录token
        registry.addInterceptor(frontTokenInterceptor()).
                addPathPatterns("/api/front/**").
                excludePathPatterns("/api/front/index").
                excludePathPatterns("/api/front/qrcode/**").
                excludePathPatterns("/api/front/login/mobile").
                excludePathPatterns("/api/front/login").
                excludePathPatterns("/api/front/sendCode").
                excludePathPatterns("/api/front/wechat/**").
                excludePathPatterns("/api/front/search/keyword").
                excludePathPatterns("/api/front/share").
                excludePathPatterns("/api/front/article/**").
                excludePathPatterns("/api/front/city/**").
                excludePathPatterns("/api/front/product/hot").
                excludePathPatterns("/api/front/product/good").
                excludePathPatterns("/api/front/products/**").
                excludePathPatterns("/api/front/reply/**").
                excludePathPatterns("/api/front/user/service/**").
                excludePathPatterns("/api/front/logistics").
                excludePathPatterns("/api/front/groom/list/**").
                excludePathPatterns("/api/front/config").
                excludePathPatterns("/api/front/category").
                excludePathPatterns("/api/front/categorybypid/*").
                excludePathPatterns("/api/front/seckill/*").
                excludePathPatterns("/api/front/seckill/list/*").
                excludePathPatterns("/api/front/seckill/detail/*").
                excludePathPatterns("/api/front/seckill/detail").
                excludePathPatterns("/api/front/ios/*").
                excludePathPatterns("/api/front/ios/register/binding/phone").
                excludePathPatterns("/api/front/combination/index").
                excludePathPatterns("/api/front/seckill/index").
                excludePathPatterns("/api/front/bargain/index").
                excludePathPatterns("/api/front/bargain/header").
                excludePathPatterns("/api/front/bargain/detail").
                excludePathPatterns("/api/front/index/product/*").
                excludePathPatterns("/api/front/index/color/config").
                excludePathPatterns("/api/front/image/domain").
                excludePathPatterns("/api/front/product/leaderboard").
                excludePathPatterns("/api/front/product/byids/**").
                excludePathPatterns("/api/front/token/is/exist").
                excludePathPatterns("/api/front/login/config").
                excludePathPatterns("/api/front/copyright/info").
                excludePathPatterns("/api/front/get/bottom/navigation").
                excludePathPatterns("/api/front/agreement/**").
                excludePathPatterns("/api/front/pagediy/**").
                excludePathPatterns("/api/front/menu/user").
                excludePathPatterns("/api/front/combination/header").
                excludePathPatterns("/api/front/combination/detail").
                excludePathPatterns("/swagger-resources/**", "/webjars/**", "/v2/**", "/swagger-ui.html/**");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/");
        registry.addResourceHandler("doc.html")
                .addResourceLocations("classpath:/META-INF/resources/");
        registry.addResourceHandler("/webjars/**")
                .addResourceLocations("classpath:/META-INF/resources/webjars/");

        /** 本地文件上传路径 */
        registry.addResourceHandler(UploadConstants.UPLOAD_FILE_KEYWORD + "/**")
                .addResourceLocations("file:" + crmebConfig.getImagePath() + "/" + UploadConstants.UPLOAD_FILE_KEYWORD + "/");
    }

    @Bean
    public FilterRegistrationBean filterRegister()
    {
        //注册过滤�?
        FilterRegistrationBean registration = new FilterRegistrationBean(responseFilter());
        registration.addUrlPatterns("/api/*");
        return registration;
    }

    /* 必须在此处配置拦截器,要不然拦不到swagger的静态资�?*/
    @Bean
    @ConditionalOnProperty(name = "swagger.basic.enable", havingValue = "true")
    public MappedInterceptor getMappedInterceptor() {
        return new MappedInterceptor(new String[]{"/doc.html", "/webjars/**"}, new SwaggerInterceptor(username, password, check));
    }
}

