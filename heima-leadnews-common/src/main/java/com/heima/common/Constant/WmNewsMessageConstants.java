package com.heima.common.Constant;

public class WmNewsMessageConstants {
    /**
     * Topic规范
      Topic默认8个分区，相同属性的业务数据推送至同一Topic中，Topic命名规范如下：

      Topic_业务名（或者）业务表名
      英文字母统一小写
      短横杠"-"用下划线代替 " _ "
     */
    public static final String WM_NEWS_UP_OR_DOWN_TOPIC="topic_up_or_down_news";
}