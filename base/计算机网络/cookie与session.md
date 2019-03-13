## cookie

### 分类

Cookie总是保存在客户端中，按在客户端中的存储位置，可分为内存Cookie和硬盘Cookie。

内存Cookie由[浏览器](https://zh.wikipedia.org/wiki/%E6%B5%8F%E8%A7%88%E5%99%A8)维护，保存在[内存](https://zh.wikipedia.org/wiki/%E5%86%85%E5%AD%98)中，浏览器关闭后就消失了，其存在时间是短暂的。硬盘Cookie保存在[硬盘](https://zh.wikipedia.org/wiki/%E7%A1%AC%E7%9B%98)里，有一个过期时间，除非用户手工清理或到了过期时间，硬盘Cookie不会被删除，其存在时间是长期的。所以，按存在时间，可分为非持久Cookie和持久Cookie。

### 用途

因为[HTTP协议](https://zh.wikipedia.org/wiki/HTTP)是无状态的，即[服务器](https://zh.wikipedia.org/wiki/%E6%9C%8D%E5%8A%A1%E5%99%A8)不知道用户上一次做了什么，这严重阻碍了[交互式Web应用程序](https://zh.wikipedia.org/wiki/%E4%BA%A4%E4%BA%92%E5%BC%8FWeb%E5%BA%94%E7%94%A8%E7%A8%8B%E5%BA%8F)的实现。在典型的网上购物场景中，用户浏览了几个页面，买了一盒饼干和两瓶饮料。最后结帐时，由于HTTP的无状态性，不通过额外的手段，服务器并不知道用户到底买了什么，所以Cookie就是用来绕开HTTP的无状态性的“额外手段”之一。服务器可以设置或读取Cookies中包含信息，借此维护用户跟服务器[会话](https://zh.wikipedia.org/wiki/%E4%BC%9A%E8%AF%9D_(%E8%AE%A1%E7%AE%97%E6%9C%BA%E7%A7%91%E5%AD%A6))中的状态。

在刚才的购物场景中，当用户选购了第一项商品，服务器在向用户发送网页的同时，还发送了一段Cookie，记录着那项商品的信息。当用户访问另一个页面，浏览器会把Cookie发送给服务器，于是服务器知道他之前选购了什么。用户继续选购饮料，服务器就在原来那段Cookie里追加新的商品信息。结帐时，服务器读取发送来的Cookie就行了。

Cookie另一个典型的应用是当登录一个网站时，网站往往会请求用户输入用户名和密码，并且用户可以勾选“下次自动登录”。如果勾选了，那么下次访问同一网站时，用户会发现没输入用户名和密码就已经登录了。这正是因为前一次登录时，服务器发送了包含登录凭据（用户名加密码的某种[加密](https://zh.wikipedia.org/wiki/%E5%8A%A0%E5%AF%86)形式）的Cookie到用户的硬盘上。第二次登录时，如果该Cookie尚未到期，浏览器会发送该Cookie，服务器验证凭据，于是不必输入用户名和密码就让用户登录了。

### Cookie的缺陷

1. Cookie会被附加在每个HTTP请求中，所以无形中增加了流量。
2. 由于在HTTP请求中的Cookie是明文传递的，所以安全性成问题，除非用[HTTPS](https://zh.wikipedia.org/wiki/HTTPS)。
3. Cookie的大小限制在**4KB左右**，对于复杂的存储需求来说是不够用的。[[3\]](https://zh.wikipedia.org/wiki/Cookie#cite_note-3)

### 使用Cookies

用户可以改变浏览器的设置，以使用Cookies。同时一些浏览器自带或安装开发者工具包允许用户查看、修改或删除特定网站的Cookies信息。

### 识别功能

如果在一台计算机中安装多个浏览器，每个浏览器都会以独立的空间存放Cookie。因为Cookie中不但可以确认用户信息，还能包含计算机和浏览器的信息，所以一个用户使用不同的浏览器登录或者用不同的计算机登录，都会得到不同的Cookie信息，另一方面，对于在同一台计算机上使用同一浏览器的多用户群，Cookie不会区分他们的身份，除非他们使用不同的用户名登录。

（2）工作原理

1、创建Cookie

当用户第一次浏览某个使用Cookie的网站时，该网站的服务器就进行如下工作：

①该用户生成一个唯一的识别码（Cookie id），创建一个Cookie对象；

②默认情况下它是一个会话级别的cookie，存储在浏览器的内存中，用户退出浏览器之后被删除。如果网站希望浏览器将该Cookie存储在磁盘上，则需要设置最大时效（maxAge），并给出一个以秒为单位的时间（将最大时效设为0则是命令浏览器删除该Cookie）；

③将Cookie放入到HTTP响应报头，将Cookie插入到一个 Set-Cookie HTTP请求报头中。

④发送该HTTP响应报文。

### 2、设置存储Cookie

浏览器收到该响应报文之后，根据报文头里的Set-Cookied特殊的指示，生成相应的Cookie，保存在客户端。该Cookie里面记录着用户当前的信息。

### 3、发送Cookie


当用户再次访问该网站时，浏览器首先检查所有存储的Cookies，如果某个存在该网站的Cookie（即该Cookie所声明的作用范围大于等于将要请求的资源），则把该cookie附在请求资源的HTTP请求头上发送给服务器。

### 4、读取Cookie

 服务器接收到用户的HTTP请求报文之后，从报文头获取到该用户的Cookie，从里面找到所需要的东西。

### cookie 的大小

各浏览器之间对cookie的不同限制：



|            | IE6.0        | IE7.0/8.0    | Opera        | FF           | Safari       | Chrome       |
| ---------- | ------------ | ------------ | ------------ | ------------ | ------------ | ------------ |
| cookie个数 | 每个域为20个 | 每个域为50个 | 每个域为30个 | 每个域为50个 | 没有个数限制 | 每个域为53个 |
| cookie大小 | 4095个字节   | 4095个字节   | 4096个字节   | 4097个字节   | 4097个字节   | 4097个字节   |

总之，在进行页面cookie操作的时候，应该尽量保证cookie个数小于20个，总大小 小于4KB

## session

（1）简介

Session代表服务器与浏览器的一次会话过程，这个过程是连续的，也可以时断时续的。Session是一种服务器端的机制，Session 对象用来存储特定用户会话所需的信息。

Session由服务端生成，保存在服务器的内存、缓存、硬盘或数据库中。

（2）工作原理

1、创建Session

当用户访问到一个服务器，如果服务器启用Session，服务器就要为该用户创建一个SESSION，在创建这个SESSION的时候，服务器首先检查这个用户发来的请求里是否包含了一个SESSION ID，如果包含了一个SESSION ID则说明之前该用户已经登陆过并为此用户创建过SESSION，那服务器就按照这个SESSION ID把这个SESSION在服务器的内存中查找出来（如果查找不到，就有可能为他新创建一个），如果客户端请求里不包含有SESSION ID，则为该客户端创建一个SESSION并生成一个与此SESSION相关的SESSION ID。这个SESSION ID是唯一的、不重复的、不容易找到规律的字符串，这个SESSION ID将被在本次响应中返回到客户端保存，而保存这个SESSION ID的正是COOKIE，这样在交互过程中浏览器可以自动的按照规则把这个标识发送给服务器。 

2、使用Session

我们知道在IE中，我们可以在工具的Internet选项中把Cookie禁止，那么会不会出现把客户端的Cookie禁止了，那么SESSIONID就无法再用了呢？找了一些资料说明，可以有其他机制在COOKIE被禁止时仍然能够把Session id传递回服务器。

经常被使用的一种技术叫做URL重写，就是把Session id直接附加在URL路径的后面一种是作为URL路径的附加信息,表现形式为： 

http://…./xxx;jSession=ByOK3vjFD75aPnrF7C2HmdnV6QZcEbzWoWiBYEnLerjQ99zWpBng!-145788764； 

另一种是作为查询字符串附加在URL后面，表现形式为： 

http://…../xxx?jSession=ByOK3vjFD75aPnrF7C2HmdnV6QZcEbzWoWiBYEnLerjQ99zWpBng!-145788764 

还有一种就是表单隐藏字段。就是服务器会自动修改表单，添加一个隐藏字段，以便在表单提交时能够把Session id传递回服务器。

（3）作用


Session的根本作用就是在服务端存储用户和服务器会话的一些信息。典型的应用有：

1、判断用户是否登录。

2、购物车功能。



三、Cookie和Session的区别

1、存放位置不同

Cookie保存在客户端，Session保存在服务端。

2 、存取方式的不同

 Cookie中只能保管ASCII字符串，假如需求存取Unicode字符或者二进制数据，需求先进行编码。Cookie中也不能直接存取Java对象。若要存储略微复杂的信息，运用Cookie是比拟艰难的。 

而Session中能够存取任何类型的数据，包括而不限于String、Integer、List、Map等。Session中也能够直接保管Java Bean乃至任何Java类，对象等，运用起来十分便当。能够把Session看做是一个Java容器类。 

3、安全性（隐私策略）的不同 

Cookie存储在浏览器中，对客户端是可见的，客户端的一些程序可能会窥探、复制以至修正Cookie中的内容。而Session存储在服务器上，对客户端是透明的，不存在敏感信息泄露的风险。 假如选用Cookie，比较好的方法是，敏感的信息如账号密码等尽量不要写到Cookie中。最好是像Google、Baidu那样将Cookie信息加密，提交到服务器后再进行解密，保证Cookie中的信息只要本人能读得懂。而假如选择Session就省事多了，反正是放在服务器上，Session里任何隐私都能够有效的保护。 

4、有效期上的不同 

只需要设置Cookie的过期时间属性为一个很大很大的数字，Cookie就可以在浏览器保存很长时间。 由于Session依赖于名为JSESSIONID的Cookie，而Cookie JSESSIONID的过期时间默许为–1，只需关闭了浏览器（一次会话结束），该Session就会失效。

5、对服务器造成的压力不同 

Session是保管在服务器端的，每个用户都会产生一个Session。假如并发访问的用户十分多，会产生十分多的Session，耗费大量的内存。而Cookie保管在客户端，不占用服务器资源。假如并发阅读的用户十分多，Cookie是很好的选择。

6、 跨域支持上的不同 

Cookie支持跨域名访问，例如将domain属性设置为“.baidu.com”，则以“.baidu.com”为后缀的一切域名均能够访问该Cookie。跨域名Cookie如今被普遍用在网络中。而Session则不会支持跨域名访问。Session仅在他所在的域名内有效。 

