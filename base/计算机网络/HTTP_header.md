**HTTP头字段**（英语：HTTP header fields）是指在[超文本传输协议](https://zh.wikipedia.org/wiki/%E8%B6%85%E6%96%87%E6%9C%AC%E4%BC%A0%E8%BE%93%E5%8D%8F%E8%AE%AE)（HTTP）的请求和响应消息中的消息头部分。它们定义了一个超文本传输协议事务中的操作参数。HTTP头部字段可以自己根据需要定义，因此可能在 Web 服务器和浏览器上发现非标准的头字段。

## 基本格式

协议头的字段，是在请求（request）或响应（response）行（一条消息的第一行内容）之后传输的。协议头的字段是以明文的[字符串](https://zh.wikipedia.org/wiki/%E5%AD%97%E7%AC%A6%E4%B8%B2)格式传输，是以冒号分隔的键名与键值对，以回车(CR)加换行(LF)符号序列结尾。协议头部分的结尾以一个空白字段标识，结果就是，也就是传输两个连续的CR+LF。在历史上，很长的行曾经可能以多个短行的形式传输；在下一行的开头，输出一个空格(SP)或者一个水平制表符(HT)，表示它是一个后续行。在如今，这种换行形式已经被废弃[[1\]](https://zh.wikipedia.org/wiki/HTTP%E5%A4%B4%E5%AD%97%E6%AE%B5#cite_note-1)。

## 类型

HTTP 头字段根据实际用途被分为以下 4 种类型：

- 通用头字段(英语：General Header Fields)
- 请求头字段(英语：Request Header Fields)
- 响应头字段(英语：Response Header Fields)
- 实体头字段(英语：Entity Header Fields)

## 字段名

在 [RFC 7230](https://tools.ietf.org/html/rfc7230)、[RFC 7231](https://tools.ietf.org/html/rfc7231)、[RFC 7232](https://tools.ietf.org/html/rfc7232)、[RFC 7233](https://tools.ietf.org/html/rfc7233)、[RFC 7234](https://tools.ietf.org/html/rfc7234) 和 [RFC 7235](https://tools.ietf.org/html/rfc7235) 中，对一组核心字段进行了标准化。有一份对于这些字段的官方的登记册，以及 一系列的补充规范 ，由互联网号码分配局（IANA）维护。各个应用程序也可以自行定义额外的字段名字及相应的值。[头字段的永久登记表](http://www.iana.org/assignments/message-headers/message-headers.xml#perm-headers)和[临时登记表](http://www.iana.org/assignments/message-headers/message-headers.xml#prov-headers)目前由[IANA](https://zh.wikipedia.org/wiki/IANA)维护。其他的字段名称和允许的值可以由各应用程序定义。

按照惯例，非标准的协议头字段是在字段名称前加上`X-`[[2\]](https://zh.wikipedia.org/wiki/HTTP%E5%A4%B4%E5%AD%97%E6%AE%B5#cite_note-2)前缀来标识。但这一惯例已在2012年6月被废弃，因为按照这种惯例，非标准字段变成标准字段时会引起很多不方便之处。[[3\]](https://zh.wikipedia.org/wiki/HTTP%E5%A4%B4%E5%AD%97%E6%AE%B5#cite_note-3)以前曾经有的使用`Downgraded-`的限制也在2013年3月被解除。[[4\]](https://zh.wikipedia.org/wiki/HTTP%E5%A4%B4%E5%AD%97%E6%AE%B5#cite_note-4)。

## 字段值

某些字段中可以包含注释内容（例如User-Agent、Server和Via字段中)，这些注释内容可由应用程序忽略[[5\]](https://zh.wikipedia.org/wiki/HTTP%E5%A4%B4%E5%AD%97%E6%AE%B5#cite_note-5)。

很多字段的值中可以包含带有权重的[质量](https://zh.wikipedia.org/wiki/%E8%B4%A8%E9%87%8F)（quality，常被简称为Q）的键值对，指定的“重量”会在[内容协商](https://zh.wikipedia.org/wiki/%E5%86%85%E5%AE%B9%E5%8D%8F%E5%95%86)的过程中使用[[6\]](https://zh.wikipedia.org/wiki/HTTP%E5%A4%B4%E5%AD%97%E6%AE%B5#cite_note-6)。

## 大小限制

标准中没有对每个协议头字段的名称和值的大小设置任何限制，也没有限制字段的个数。然而，出于实际场景及安全性的考虑，大部分的服务器、客户端和代理软件都会实施一些限制。例如，[Apache](https://zh.wikipedia.org/wiki/Apache) 2.3服务器在默认情况下限制每个字段的大小不得超过8190字节，同时，单个请求中最多有100个头字段[[7\]](https://zh.wikipedia.org/wiki/HTTP%E5%A4%B4%E5%AD%97%E6%AE%B5#cite_note-7)。

## 请求字段 

| Accept                                                       | 能够接受的回应内容类型（Content-Types）。参见[内容协商](https://zh.wikipedia.org/wiki/%E5%86%85%E5%AE%B9%E5%8D%8F%E5%95%86)。 | `Accept: text/plain`                                         | 常设                                                         |
| ------------------------------------------------------------ | ------------------------------------------------------------ | ------------------------------------------------------------ | ------------------------------------------------------------ |
| Accept-Charset                                               | 能够接受的字符集                                             | `Accept-Charset: utf-8`                                      | 常设                                                         |
| Accept-Encoding                                              | 能够接受的编码方式列表。参考[HTTP压缩](https://zh.wikipedia.org/wiki/HTTP%E5%8E%8B%E7%BC%A9)。 | `Accept-Encoding: gzip, deflate`                             | 常设                                                         |
| Accept-Language                                              | 能够接受的回应内容的自然语言列表。参考 [内容协商](https://zh.wikipedia.org/wiki/%E5%86%85%E5%AE%B9%E5%8D%8F%E5%95%86) 。 | `Accept-Language: en-US`                                     | 常设                                                         |
| Accept-Datetime                                              | 能够接受的按照时间来表示的版本                               | `Accept-Datetime: Thu, 31 May 2007 20:35:00 GMT`             | 临时                                                         |
| Authorization                                                | 用于超文本传输协议的认证的认证信息                           | `Authorization: Basic QWxhZGRpbjpvcGVuIHNlc2FtZQ==`          | 常设                                                         |
| [Cache-Control](https://zh.wikipedia.org/wiki/%E7%BD%91%E9%A1%B5%E5%BF%AB%E7%85%A7) | 用来指定在这次的请求/响应链中的所有缓存机制 都必须 遵守的指令 | `Cache-Control: no-cache`                                    | 常设                                                         |
| Connection                                                   | 该浏览器想要优先使用的连接类型[[1\]](https://zh.wikipedia.org/w/index.php?title=HTTP%E5%A4%B4%E5%AD%97%E6%AE%B5&action=submit#cite_note-rfc7230_connection-1) | `Connection: keep-alive``Connection: Upgrade`                | 常设                                                         |
| Cookie                                                       | 之前由服务器通过 Set- Cookie （下文详述）发送的一个 超文本传输协议Cookie | `Cookie: $Version=1; Skin=new;`                              | 常设: 标准                                                   |
| Content-Length                                               | 以 八位字节数组 （8位的字节）表示的请求体的长度              | `Content-Length: 348`                                        | 常设                                                         |
| Content-MD5                                                  | 请求体的内容的二进制 MD5 散列值，以 Base64 编码的结果        | `Content-MD5: Q2hlY2sgSW50ZWdyaXR5IQ==`                      | 过时的[[2\]](https://zh.wikipedia.org/w/index.php?title=HTTP%E5%A4%B4%E5%AD%97%E6%AE%B5&action=submit#cite_note-2) |
| Content-Type                                                 | 请求体的 多媒体类型 （用于POST和PUT请求中）                  | `Content-Type: application/x-www-form-urlencoded`            | 常设                                                         |
| Date                                                         | 发送该消息的日期和时间(按照 RFC 7231 中定义的"超文本传输协议日期"格式来发送) | `Date: Tue, 15 Nov 1994 08:12:31 GMT`                        | 常设                                                         |
| Expect                                                       | 表明客户端要求服务器做出特定的行为                           | `Expect: 100-continue`                                       | 常设                                                         |
| From                                                         | 发起此请求的用户的邮件地址                                   | `From: user@example.com`                                     | 常设                                                         |
| Host                                                         | 服务器的域名(用于虚拟主机 )，以及服务器所监听的[传输控制协议](https://zh.wikipedia.org/wiki/%E4%BC%A0%E8%BE%93%E6%8E%A7%E5%88%B6%E5%8D%8F%E8%AE%AE)端口号。如果所请求的端口是对应的服务的标准端口，则端口号可被省略。[[3\]](https://zh.wikipedia.org/w/index.php?title=HTTP%E5%A4%B4%E5%AD%97%E6%AE%B5&action=submit#cite_note-3) 自超文件传输协议版本1.1（HTTP/1.1）开始便是必需字段。 | `Host: en.wikipedia.org:80``Host: en.wikipedia.org`          | 常设                                                         |
| If-Match                                                     | 仅当客户端提供的实体与服务器上对应的实体相匹配时，才进行对应的操作。主要作用时，用作像 PUT 这样的方法中，仅当从用户上次更新某个资源以来，该资源未被修改的情况下，才更新该资源。 | `If-Match: "737060cd8c284d8af7ad3082f209582d"`               | 常设                                                         |
| If-Modified-Since                                            | 允许在对应的内容未被修改的情况下返回304未修改（ 304 Not Modified ） | `If-Modified-Since: Sat, 29 Oct 1994 19:43:31 GMT`           | 常设                                                         |
| If-None-Match                                                | 允许在对应的内容未被修改的情况下返回304未修改（ 304 Not Modified ），参考 超文本传输协议 的[实体标记](https://zh.wikipedia.org/wiki/HTTP_ETag) | `If-None-Match: "737060cd8c284d8af7ad3082f209582d"`          | 常设                                                         |
| If-Range                                                     | 如果该实体未被修改过，则向我发送我所缺少的那一个或多个部分；否则，发送整个新的实体 | `If-Range: "737060cd8c284d8af7ad3082f209582d"`               | 常设                                                         |
| If-Unmodified-Since                                          | 仅当该实体自某个特定时间已来未被修改的情况下，才发送回应。   | `If-Unmodified-Since: Sat, 29 Oct 1994 19:43:31 GMT`         | 常设                                                         |
| Max-Forwards                                                 | 限制该消息可被代理及网关转发的次数。                         | `Max-Forwards: 10`                                           | 常设                                                         |
| Origin                                                       | 发起一个针对 跨来源资源共享 的请求（要求服务器在回应中加入一个‘访问控制-允许来源’（'Access-Control-Allow-Origin'）字段）。 | `Origin: http://www.example-social-network.com`              | 常设: 标准                                                   |
| Pragma                                                       | 与具体的实现相关，这些字段可能在请求/回应链中的任何时候产生多种效果。 | `Pragma: no-cache`                                           | 常设但不常用                                                 |
| Proxy-Authorization                                          | 用来向代理进行认证的认证信息。                               | `Proxy-Authorization: Basic QWxhZGRpbjpvcGVuIHNlc2FtZQ==`    | 常设                                                         |
| Range                                                        | 仅请求某个实体的一部分。字节偏移以0开始。参见[字节服务](https://zh.wikipedia.org/w/index.php?title=%E5%AD%97%E8%8A%82%E6%9C%8D%E5%8A%A1&action=edit&redlink=1)。 | `Range: bytes=500-999`                                       | 常设                                                         |
| [Referer](https://zh.wikipedia.org/wiki/HTTP%E5%8F%83%E7%85%A7%E4%BD%8D%E5%9D%80) [*sic*] [[4\]](https://zh.wikipedia.org/w/index.php?title=HTTP%E5%A4%B4%E5%AD%97%E6%AE%B5&action=submit#cite_note-4) | 表示浏览器所访问的前一个页面，正是那个页面上的某个链接将浏览器带到了当前所请求的这个页面。 | `Referer: http://en.wikipedia.org/wiki/Main_Page`            | 常设                                                         |
| TE                                                           | 浏览器预期接受的传输编码方式：可使用回应协议头 Transfer-Encoding 字段中的值；另外还可用"trailers"（与"分块 "传输方式相关）这个值来表明浏览器希望在最后一个尺寸为0的块之后还接收到一些额外的字段。 | `TE: trailers, deflate`                                      | 常设                                                         |
| User-Agent                                                   | 浏览器的[浏览器身份标识字符串](https://zh.wikipedia.org/wiki/%E7%94%A8%E6%88%B7%E4%BB%A3%E7%90%86) | `User-Agent: Mozilla/5.0 (X11; Linux x86_64; rv:12.0) Gecko/20100101 Firefox/21.0` | 常设                                                         |
| Upgrade                                                      | 要求服务器升级到另一个协议。                                 | `Upgrade: HTTP/2.0, SHTTP/1.3, IRC/6.9, RTA/x11`             | 常设                                                         |
| Via                                                          | 向服务器告知，这个请求是由哪些代理发出的。                   | `Via: 1.0 fred, 1.1 example.com (Apache/1.1)`                | 常设                                                         |
| Warning                                                      | 一个一般性的警告，告知，在实体内容体中可能存在错误。         | `Warning: 199 Miscellaneous warning`                         | 常设                                                         |

### 常见的非标准请求字段

| 字段名                                                       | 说明                                                         | 示例                                                         |
| ------------------------------------------------------------ | ------------------------------------------------------------ | ------------------------------------------------------------ |
| X-Requested-With                                             | 主要用于标识 Ajax 及可扩展标记语言 请求。大部分的JavaScript框架会发送这个字段，且将其值设置为 XMLHttpRequest | `X-Requested-With: XMLHttpRequest`                           |
| [DNT](https://zh.wikipedia.org/wiki/%E8%AF%B7%E5%8B%BF%E8%BF%BD%E8%B8%AA)[[5\]](https://zh.wikipedia.org/w/index.php?title=HTTP%E5%A4%B4%E5%AD%97%E6%AE%B5&action=submit#cite_note-5) | 请求某个网页应用程序停止跟踪某个用户。在火狐浏览器中，相当于X-Do-Not-Track协议头字段（自 Firefox/4.0 Beta 11 版开始支持）。[Safari](https://zh.wikipedia.org/wiki/Safari) 和 [Internet Explorer](https://zh.wikipedia.org/wiki/Internet_Explorer) 9 也支持这个字段。2011年3月7日，草案提交IETF。[[6\]](https://zh.wikipedia.org/w/index.php?title=HTTP%E5%A4%B4%E5%AD%97%E6%AE%B5&action=submit#cite_note-6) 万维网协会 的跟踪保护工作组正在就此制作一项规范。[[7\]](https://zh.wikipedia.org/w/index.php?title=HTTP%E5%A4%B4%E5%AD%97%E6%AE%B5&action=submit#cite_note-7) | `DNT: 1 (DNT启用)``DNT: 0 (DNT被禁用)`                       |
| [X-Forwarded-For](https://zh.wikipedia.org/wiki/X-Forwarded-For)[[8\]](https://zh.wikipedia.org/w/index.php?title=HTTP%E5%A4%B4%E5%AD%97%E6%AE%B5&action=submit#cite_note-8) | 一个事实标准 ，用于标识某个通过超文本传输协议代理或负载均衡连接到某个网页服务器的客户端的原始互联网地址 | `X-Forwarded-For: client1, proxy1, proxy2``X-Forwarded-For: 129.78.138.66, 129.78.64.103` |
| X-Forwarded-Host[[9\]](https://zh.wikipedia.org/w/index.php?title=HTTP%E5%A4%B4%E5%AD%97%E6%AE%B5&action=submit#cite_note-9) | 一个事实标准 ，用于识别客户端原本发出的 `Host` 请求头部[[10\]](https://zh.wikipedia.org/w/index.php?title=HTTP%E5%A4%B4%E5%AD%97%E6%AE%B5&action=submit#cite_note-10)。 | `X-Forwarded-Host: en.wikipedia.org:80``X-Forwarded-Host: en.wikipedia.org` |
| X-Forwarded-Proto[[11\]](https://zh.wikipedia.org/w/index.php?title=HTTP%E5%A4%B4%E5%AD%97%E6%AE%B5&action=submit#cite_note-11) | 一个事实标准，用于标识某个超文本传输协议请求最初所使用的协议。[[12\]](https://zh.wikipedia.org/w/index.php?title=HTTP%E5%A4%B4%E5%AD%97%E6%AE%B5&action=submit#cite_note-12) | `X-Forwarded-Proto: https`                                   |
| Front-End-Https[[13\]](https://zh.wikipedia.org/w/index.php?title=HTTP%E5%A4%B4%E5%AD%97%E6%AE%B5&action=submit#cite_note-13) | 被微软的服务器和负载均衡器所使用的非标准头部字段。           | `Front-End-Https: on`                                        |
| X-Http-Method-Override[[14\]](https://zh.wikipedia.org/w/index.php?title=HTTP%E5%A4%B4%E5%AD%97%E6%AE%B5&action=submit#cite_note-14) | 请求某个网页应用程序使用该协议头字段中指定的方法（一般是PUT或DELETE）来覆盖掉在请求中所指定的方法（一般是POST）。当某个浏览器或防火墙阻止直接发送PUT 或DELETE 方法时（注意，这可能是因为软件中的某个漏洞，因而需要修复，也可能是因为某个配置选项就是如此要求的，因而不应当设法绕过），可使用这种方式。 | `X-HTTP-Method-Override: DELETE`                             |
| X-ATT-DeviceId[[15\]](https://zh.wikipedia.org/w/index.php?title=HTTP%E5%A4%B4%E5%AD%97%E6%AE%B5&action=submit#cite_note-15) | 使服务器更容易解读AT&T设备User-Agent字段中常见的设备型号、固件信息。 | `X-Att-Deviceid: GT-P7320/P7320XXLPG`                        |
| X-Wap-Profile[[16\]](https://zh.wikipedia.org/w/index.php?title=HTTP%E5%A4%B4%E5%AD%97%E6%AE%B5&action=submit#cite_note-16) | 链接到互联网上的一个XML文件，其完整、仔细地描述了正在连接的设备。右侧以为AT&T Samsung Galaxy S2提供的XML文件为例。 | `x-wap-profile:http://wap.samsungmobile.com/uaprof/SGH-I777.xml` |
| Proxy-Connection[[17\]](https://zh.wikipedia.org/w/index.php?title=HTTP%E5%A4%B4%E5%AD%97%E6%AE%B5&action=submit#cite_note-17) | 该字段源于早期超文本传输协议版本实现中的错误。与标准的连接（Connection）字段的功能完全相同。 | `Proxy-Connection: keep-alive`                               |
| X-Csrf-Token[[18\]](https://zh.wikipedia.org/w/index.php?title=HTTP%E5%A4%B4%E5%AD%97%E6%AE%B5&action=submit#cite_note-18) | 用于防止 [跨站请求伪造](https://zh.wikipedia.org/wiki/%E8%B7%A8%E7%AB%99%E8%AF%B7%E6%B1%82%E4%BC%AA%E9%80%A0)。 辅助用的头部有 `X-CSRFToken`[[19\]](https://zh.wikipedia.org/w/index.php?title=HTTP%E5%A4%B4%E5%AD%97%E6%AE%B5&action=submit#cite_note-19) 或 `X-XSRF-TOKEN`[[20\]](https://zh.wikipedia.org/w/index.php?title=HTTP%E5%A4%B4%E5%AD%97%E6%AE%B5&action=submit#cite_note-20) | `X-Csrf-Token: i8XNjC4b8KVok4uw5RftR38Wgp2BFwql`             |