表结构

1. zabbix server -> twatch_zabbix_server
2. 专线交换机表 -> twatch_device
3. 专线表 -> twatch_leased_line
4. 企业专线表 -> twatch_enterprise_leased_line
5. 系统配置 -> twatch_system_setting

```sql
-- 
CREATE TABLE twatch_zabbix_server
(
  id serial NOT NULL, -- id标识
  name character varying, -- 名称
  url character varying, -- api地址
  "user" character varying, -- 用户名
  password character varying, -- 密码
  create_time timestamp with time zone DEFAULT now(), -- 记录创建时间
  CONSTRAINT twatch_zabbix_server_pkey PRIMARY KEY (id)
)
WITHOUT OIDS;
ALTER TABLE twatch_zabbix_server OWNER TO postgres;
COMMENT ON TABLE twatch_zabbix_server IS 'zabbixServer表';
COMMENT ON COLUMN twatch_zabbix_server.id IS 'id标识';
COMMENT ON COLUMN twatch_zabbix_server.name IS '名称';
COMMENT ON COLUMN twatch_zabbix_server.url IS 'api地址';
COMMENT ON COLUMN twatch_zabbix_server.user IS '用户名';
COMMENT ON COLUMN twatch_zabbix_server.password IS '密码';
COMMENT ON COLUMN twatch_zabbix_server.create_time IS '记录创建时间';

-- 专线交换机表
CREATE TABLE twatch_device
(
  id serial NOT NULL, -- id标识
  name character varying, -- name-value对
  ip character varying, -- name-value对
  snmp_port integer DEFAULT 0, -- 属性
  private character varying, -- 属性
  comment character varying DEFAULT '',
  active integer DEFAULT 1, -- 是否激活 1激活, 0不激活
  zabbix_server_id integer,
  create_time timestamp with time zone DEFAULT now(), -- 记录创建时间
  CONSTRAINT twatch_device_pkey PRIMARY KEY (id),
  CONSTRAINT twatch_device_name_unique UNIQUE (name)
)
WITHOUT OIDS;
ALTER TABLE twatch_device OWNER TO postgres;
COMMENT ON TABLE twatch_device IS '专线交换机表';
COMMENT ON COLUMN twatch_device.id IS 'id标识';
COMMENT ON COLUMN twatch_device.name IS '设备名称';
COMMENT ON COLUMN twatch_device.ip IS 'ip地址';
COMMENT ON COLUMN twatch_device.snmp_port IS 'snmp端口';
COMMENT ON COLUMN twatch_device.private IS 'private';
COMMENT ON COLUMN twatch_device.comment IS '备注';
COMMENT ON COLUMN twatch_device.active IS '是否激活 1激活, 0不激活';
COMMENT ON COLUMN twatch_device.zabbix_server_id IS '设备所属的zabbix server';
COMMENT ON COLUMN twatch_device.create_time IS '记录创建时间';

-- 专线表
-- Table: twatch_leased_line

-- DROP TABLE twatch_leased_line;

CREATE TABLE twatch_leased_line
(
  id serial NOT NULL, -- id标识
  device_id bigint NOT NULL,
  name character varying,
  port integer DEFAULT 0,
  target_ip character varying,
  vrf character varying DEFAULT '',
  traffic_port character varying DEFAULT '',
  carrier character varying DEFAULT '',
  bandwidth integer DEFAULT 0,
  area character varying DEFAULT '',
  idc character varying DEFAULT '',
  category character varying DEFAULT '',
  comment character varying DEFAULT '',
  create_time timestamp with time zone DEFAULT now(), -- 记录创建时间
  ip character varying DEFAULT '',
  customer_link_man character varying DEFAULT '',
  customer_link_number character varying DEFAULT '',
  CONSTRAINT twatch_leased_line_pkey PRIMARY KEY (id),
  CONSTRAINT twatch_leased_line_device_id_name_unique UNIQUE (device_id, name),
  CONSTRAINT twatch_leased_line_device_id_port_unique UNIQUE (device_id, port)
)
WITHOUT OIDS;
ALTER TABLE twatch_leased_line OWNER TO postgres;
COMMENT ON TABLE twatch_leased_line IS '专线表';
COMMENT ON COLUMN twatch_leased_line.id IS 'id标识';
COMMENT ON COLUMN twatch_leased_line.device_id IS '交换机id';
COMMENT ON COLUMN twatch_leased_line.name IS '名称';
COMMENT ON COLUMN twatch_leased_line.port IS '端口';
COMMENT ON COLUMN twatch_leased_line.target_ip IS '目标ip';
COMMENT ON COLUMN twatch_leased_line.vrf IS 'VRF';
COMMENT ON COLUMN twatch_leased_line.traffic_port IS '流量端口';
COMMENT ON COLUMN twatch_leased_line.carrier IS '运营商';
COMMENT ON COLUMN twatch_leased_line.bandwidth IS '带宽';
COMMENT ON COLUMN twatch_leased_line.area IS '地区';
COMMENT ON COLUMN twatch_leased_line.idc IS '机房';
COMMENT ON COLUMN twatch_leased_line.category IS '类别, 数据/语音';
COMMENT ON COLUMN twatch_leased_line.comment IS '备注';
COMMENT ON COLUMN twatch_leased_line.create_time IS '记录创建时间';
COMMENT ON COLUMN twatch_leased_line.ip IS '天润侧ip';
COMMENT ON COLUMN twatch_leased_line.customer_link_man IS '客户IT联系人';
COMMENT ON COLUMN twatch_leased_line.customer_link_number IS '客户IT联系电话';

-- 企业专线
-- Table: twatch_enterprise_leased_line

-- DROP TABLE twatch_enterprise_leased_line;

CREATE TABLE twatch_enterprise_leased_line
(
  id serial NOT NULL, -- id标识
  enterprise_id bigint NOT NULL,
  leased_line_id bigint NOT NULL,
  comment character varying DEFAULT '',
  create_time timestamp with time zone DEFAULT now(), -- 记录创建时间
  CONSTRAINT twatch_enterprise_leased_line_pkey PRIMARY KEY (id),
  CONSTRAINT twatch_enterprise_leased_line_enterprise_id_leased_line_id_uniq UNIQUE (enterprise_id, leased_line_id)
)
WITHOUT OIDS;
ALTER TABLE twatch_enterprise_leased_line OWNER TO postgres;
COMMENT ON TABLE twatch_enterprise_leased_line IS '企业专线表';
COMMENT ON COLUMN twatch_enterprise_leased_line.id IS 'id标识';
COMMENT ON COLUMN twatch_enterprise_leased_line.enterprise_id IS '企业编号';
COMMENT ON COLUMN twatch_enterprise_leased_line.leased_line_id IS '专线id';
COMMENT ON COLUMN twatch_enterprise_leased_line.comment IS '备注';
COMMENT ON COLUMN twatch_enterprise_leased_line.create_time IS '记录创建时间';


-- 系统配置
-- Table: twatch_system_setting

-- DROP TABLE twatch_system_setting;

CREATE TABLE twatch_system_setting
(
  id serial NOT NULL, -- id标识
  name character varying, -- name-value对
  value character varying, -- name-value对
  property character varying, -- 属性
  create_time timestamp with time zone DEFAULT now(), -- 记录创建时间
  CONSTRAINT twatch_system_setting_pkey PRIMARY KEY (id)
)
WITHOUT OIDS;
ALTER TABLE twatch_system_setting OWNER TO postgres;
COMMENT ON TABLE twatch_system_setting IS '系统设置表';
COMMENT ON COLUMN twatch_system_setting.id IS 'id标识';
COMMENT ON COLUMN twatch_system_setting.name IS 'name-value对';
COMMENT ON COLUMN twatch_system_setting.value IS 'name-value对';
COMMENT ON COLUMN twatch_system_setting.property IS '属性';
COMMENT ON COLUMN twatch_system_setting.create_time IS '记录创建时间';
```









```java
import com.alibaba.dubbo.rpc.*;
import com.tinet.ctilink.cache.CacheKey;
import com.tinet.ctilink.cache.RedisService;
import com.tinet.ctilink.conf.ApiResult;
import com.tinet.ctilink.util.AuthenticUtil;
import com.tinet.ctilink.util.RemoteClient;
import com.tinet.ctilink.util.SystemSettingMacro;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.http.HttpServletRequest;

/**
 * @author 王扶摇
 * @Title: ProviderFilter
 * @ProjectName cti-link-paas
 * @date 2018/7/26 11:12
 */
public class ProviderFilter implements com.alibaba.dubbo.rpc.Filter {
    private final static Logger logger = LoggerFactory.getLogger(ProviderFilter.class);

    @Autowired
    private RedisService redisService;

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        long startTimeMillis = System.currentTimeMillis();
        Result result = null;
        String m = "";

        String clientIp = RpcContext.getContext().getRemoteHost();
        Object request = RpcContext.getContext().getRequest();
        if (request != null && request instanceof HttpServletRequest) {
            clientIp = RemoteClient.getIpAddr((HttpServletRequest) request);
        }

        //IP控制 + 限流
        if (StringUtils.isNotEmpty(SystemSettingMacro.CONF_WHITE_IP_LIST)) {
            if (StringUtils.isEmpty(clientIp)
                    || !AuthenticUtil.isInWhiteIpList(clientIp, SystemSettingMacro.CONF_WHITE_IP_LIST.split(","))) {
                result = new RpcResult(new ApiResult<>(ApiResult.FAIL_RESULT, "ip is not allowed"));
                return result;
            }
        }

        if (SystemSettingMacro.CONF_REQUEST_LIMIT_COUNT_VALUE > 0) {
            if (!AuthenticUtil.validateFrequency(redisService, CacheKey.CONF_REQUEST_COUNT
                    , SystemSettingMacro.CONF_REQUEST_LIMIT_COUNT_PROPERTY
                    , SystemSettingMacro.CONF_REQUEST_LIMIT_COUNT_VALUE)) {
                result = new RpcResult(new ApiResult<>(ApiResult.FAIL_RESULT, "request is too frequently"));
                return result;
            }
        }
        result = invoker.invoke(invocation);
        return result;
    }
}
```

dubbo filter

通过dubbo 的过滤器扩展，实现白名单和ip 限流的功能。Filter 通过SPI 实现，在 MATE-INFO /dubbo 下新建一个文本文件名称是 com.alibaba.dubbo.rpc.Filter 过滤器的全称，内容

```
provider=com.tinet.twatch.conf.filter.ProviderFilter
```



```java
public static boolean isInWhiteIpList(String ip, String[] patternList) {
    for (String pattern : patternList) {
        if (ip.equals(pattern)) {
            return true;
        }

        String[] ipSub = ip.split("\\.");
        String[] patternSub = pattern.split("\\.");
        boolean match = true;
        for (int i = 0; i < 3; i++) {
            if (patternSub[i].equals("*")) {
                continue;
            }
            try {
                if (!ipSub[i].equals(patternSub[i])
                        && Integer.parseInt(ipSub[i]) != Integer.parseInt(patternSub[i])) {
                    match = false;
                }
            } catch (Exception e) {
                e.printStackTrace();
                match = false;
            }
        }
        if (match) {
            return true;
        }
    }
    return false;
}
```

限流：通过 redis、 incrby、 ttl、 expire 命令实现。首先通过RPC在通过 RPCcontext.getContext.getRequest()  获取到调用者的request 请求后。在通过RemoteClient.getIpAddr((HttpServletRequest)request)。

如果 SystemSettingMacro.CONF_REQUEST_LIMIT_COUNT_VALUE > 0 限流通过次数。调用 validateFrequency。

```java
//多长时间多少次
public static boolean validateFrequency(RedisService redisService, String key, String property, int limit) {
    //查询时间
    int second = getSecond(property);

    long count = redisService.incrby(Const.REDIS_DB_NON_CONFIGURE_INDEX, key, 1);
    if (count == 1) {
        redisService.expire(Const.REDIS_DB_NON_CONFIGURE_INDEX, key, second, TimeUnit.SECONDS);
    }
    if (count > limit) {
        //保护代码，万一重置key的超时时间出错，用来补救。
        if (redisService.ttl(Const.REDIS_DB_NON_CONFIGURE_INDEX, key) < 0) {
            redisService.expire(Const.REDIS_DB_NON_CONFIGURE_INDEX, key, second, TimeUnit.SECONDS);
            return true;
        }
        return false;
    }

    return true;
}
```

首先获取每次限流的控制时间。（60 秒）接下来调用redis 的incrby 命令获取key 为conf.Request.count 命令。如果如果 返回值为1，则设置过期时间60 秒。

如果超过了limit 则，先 ttl 判断过期时间，如果小于 0 （-2），则重新设置过期时间。并返回 true 否则返回false ps:如果重置key 的超时时间出错。则重设超时时间

IP 限流

------

```java
@Path("interface/v1/device")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public interface TWatchDeviceService {
    /**
     * 根据传递的pojo创建一条switch表记录
     *
     * @return com.tinet.twatch.conf.commons.ApiResult<T>
     * @Date 23:30 2018/7/29
     * @Param [tWatchSwitch]
     */
    @POST
    @Path("create")
    ApiResult<TWatchDevice> createDevice(TWatchDevice tWatchDevice);

    /**
     * 获取全部的交换机信息
     *
     * @return com.tinet.twatch.conf.commons.ApiResult<T>
     * @Date 14:43 2018/7/29
     * @Param []
     */
    @POST
    @Path("list")
    ApiResult<List<TWatchDevice>> listDevice(TWatchDevice tWatchDevice);

    /**

     *
     * @return com.tinet.twatch.conf.commons.ApiResult<T>
     * @Date 14:45 2018/7/29
     * @Param []
     */
    @POST
    @Path("get")
    ApiResult<TWatchDevice> getDevice(TWatchDevice tWatchDevice);


     *
     * @return com.tinet.twatch.conf.commons.ApiResult<T>
     * @Date 14:46 2018/7/29
     * @Param []
     */
    @POST
    @Path("update")
    ApiResult<TWatchDevice> updateDevice(TWatchDevice tWatchDevice);

    /**

     *
     * @return com.tinet.twatch.conf.commons.ApiResult<T>
     * @Date 14:47 2018/7/29
     * @Param []
     */
    @POST
    @Path("delete")
    ApiResult deleteDevice(TWatchDevice tWatchDevice);

}
```

加载缓存通过 spring 提供的监听器。与模板模式，按接口类型注入。reloadCache

```java
@Component
public class ApplicationStarter implements ApplicationListener<ContextRefreshedEvent> {
    private Logger logger = LoggerFactory.getLogger(ApplicationStarter.class);

    @Autowired
    private CacheReloadThread cacheReloadThread;

    @Override
    public void onApplicationEvent(final ContextRefreshedEvent event) {

        // 设置JVM的DNS缓存时间
        // http://docs.amazonaws.cn/AWSSdkDocsJava/latest/DeveloperGuide/java-dg-jvm-ttl.html
        java.security.Security.setProperty("networkaddress.cache.ttl", "60");

        //加载缓存
        cacheReloadThread.start();

        logger.info("twatch-conf启动成功");
        System.out.println("twatch-conf启动成功");
    }
}
```

```java
@Component
public class CacheReloadThread extends Thread {

    private Logger logger = LoggerFactory.getLogger(CacheReloadThread.class);

    @Autowired
    private List<TWatchCacheInterface> cacheInterfaceList;

    @Override
    public void run() {
        long beginTime = System.currentTimeMillis();
        logger.info("Start loading cache，time：" + beginTime);
        Optional.ofNullable(cacheInterfaceList)
                .ifPresent(cacheInterfaceList -> cacheInterfaceList.stream()
                .forEach(TWatchCacheInterface::reloadCache));
        long endTime  = System.currentTimeMillis();
        logger.info("End of loading cache，time：" + endTime + " Time consuming：" + (beginTime - endTime) + " ms");
    }

}
```