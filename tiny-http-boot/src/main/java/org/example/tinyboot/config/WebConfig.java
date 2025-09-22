package org.example.tinyboot.config;

import org.example.tinyboot.web.AccessLogFilter;
import org.example.tinyboot.web.RequestIdFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WebConfig {

  @Bean
  public FilterRegistrationBean<RequestIdFilter> rerquestIdFilter(){
    var bean = new FilterRegistrationBean<RequestIdFilter>(new RequestIdFilter());
    bean.setOrder(1);
    return bean;
  }

  @Bean
  public FilterRegistrationBean<AccessLogFilter> accessLogFilter() {
    var bean = new FilterRegistrationBean<AccessLogFilter>(new AccessLogFilter());
    bean.setOrder(2);
    return bean;
  }
}
