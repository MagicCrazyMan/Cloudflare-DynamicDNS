# Cloudflare-DynamicDNS
<p>基于Java的 Cloudflare DynamicDNS 命令行应用</br><strong>当前版本 2.1.0</strong></p>
<p><i>
  该应用暂时只有<strong>中文</strong>版本！
  </br>
  This program has only <strong>Chinese</strong> version present!
</i></p>

<h3>介绍</h3>
<p>本应用主要为拥有动态公网IP的家庭宽带用户提供解决方案</br><i><strong>一次操作，一劳永逸</strong></i></p>

<h3>使用前须知</h3>
<ol>
  <li>应用基于 Cloudflare DNS Web API 开发，现阶段<strong>仅可用于</strong> Cloudflare DNS 服务</li>
  <li>应用基于 OpenJDK 1.8.0_191 开发，请使用<strong> JRE 1.8.0_191 或以上版本 </strong>运行</li>
  <li>应用为<strong>命令行</strong>应用，暂无GUI界面</li>
  <li>应用<strong>必须</strong>部署于需要动态更新IP的网络内的一台设备</li>
  <li>应用使用<strong>JSON</strong>作为配置文件，使用前最好有基本的JSON知识</li>
  <li>应用<strong>不提供</strong>对配置文件的任何加密功能，配置文件均为<strong>明文</strong>记录，用户<strong>必须</strong>自行保证配置文件的安全</li>
  <li>Cloudflare Global API Key 一旦泄露，请<strong>立即</strong>前往 Cloudflare 官网重置 Global API Key</li>
</ol>

<h3>基础用法</h3>
<ol>
  <li>下载应用程序并放置在合适的文件夹中</li>
  <li>确定操作系统已经安装 JRE 1.8.0_191 或以上版本</li>
  <li>使用命令 <code>java -jar <程序包名称>.jar</code> 执行程序</li>
  <li>首次执行会自动创建一个模板配置文件 <code>template.json</code> </li>
  <li>根据实际情况配置文件并重命名为 <code>config.json</code> </li>
  <li>重新执行程序，正式开始运行</li>
</ol>

<h3>命令行参数</h3>
<ul>
  <li>-c,--config <FILE></br>指定配置文件,默认为当前目录的config.json</li>
  <li>-b,--baidu</br>使用<a href="http://www.baidu.com/s?wd=ip">百度</a>查询网络IP，不建议使用，有可能存在泄漏IP的风险，建议仅在无额外服务器可进行IP查询的情况下使用</li>
  <li>-j,--json</br>运行配置文件创建工具</li>
  <li>-l,--list</br>运行DNS查询工具并进行配置文件创建</li>
  <li>-h,--help</br>显示所有可用参数</br>
  <li>-v,--version</br>查看当前版本</li>
</ul>

<h3>JSON配置文件格式</h3>
<p>此处使用 Java 语言进行简单表述，查看源代码以获取更详细的格式
  </br>
  有点复杂，希望你不会看晕
</p>
<pre><code>ConfigurationJson
|-(String) whereGetYourIP   //全局变量，获取IP的地址
|-(String) logFileHome    //全局变量，日志文件存放位置
|-(int) defaultSleepSconds  //全局变量，默认线程休眠的时间，如ip未变更以及ttl为1(Automatic)是
|-(int) failedSleepSeconds  //全局变量，发生错误后线程休眠的时间，如连接网络失败
|-(ArrayList<Acccount>) accounts  //含有所有 Cloudflare Account 的 ArrayList
  |-(String) email  //Cloudflare Account 对应的 Email
  |-(String) key  //Cloudflare Account 对应的 Global API Key
  |-(ArrayList<Domain>) Domains   //包含所有该 Cloudflare Account 下需要动态更新的域名的 ArrayList
    |-(String) nickname   //为线程取一个你喜欢的名字，每一个域名都会有一个线程负责更新
    |-(String) domain   //域名的完整名称
    |-(String) zone   //根域名的 ZoneID，每一个根域名都会一个唯一的Zone ID
    |-(String) identifier   //该域名的 identifier ID
    |-(String) type   //该域名的类型
    |-(int) ttl   //该域名的TTL值
    |-(boolean) proixed   //是否使用 Cloudflare CDN代理</pre></code>  
<ul>注意：
  <li>一个配置文件可以含有多个 Cloudflare Account，每一个账户有一个对应的 Email 和 Global API Key</li>
  <li>一个 Cloudflare Account 可以含有多个 Domain，每一个 Domain 都有自己对应的信息</li>
  <li>
  whereGetYourIP 的URL应该以JSON的方式返回ip信息，并且格式要求为 <code>{"ip":"0.0.0.0"}</code>
  </li>
</ul>

<h3>一些自带的小工具</h3>
<ol>
  <li>
    <h4>配置文件创建工具 <code>-j,--json</code> </h4>
    <p>
      你可能在想，woc！这配置文件这么复杂，手写岂不是要死！？就算有模板那也是要白给的。
      </br>
      没错！的确是这样。而且对于很多只想单纯使用的用户来说也太复杂了，所以就有了这个工具。
      </br>
      <s>其实是因为我懒得手动创建配置文件。</s>
    </p>
    <p>只要使用 <code>java -jar <程序包名称>.jar -j</code> 就可以使用这个工具</p>
    <p>注：该工具也为命令行应用，无GUI界面，且现阶段文字排版样式可能不太友好</p>
</li>
<li>
  <h4>DNS记录查询及快捷配置文件创建工具 <code>-l,--list</code> </h4>
  <p>
    你可能又想了，Cloudflare 查询DNS记录太不友好了吧？查个identifier还要用个Web API，太秀了。
    </br>
    yeap，所以就有了这个工具。而且它还拥有快捷的配置文件创建功能。
    </br>
    <s>实际上查询DNS也方便不了多少。</s>
    </br>
    但为单个 Cloudflare Account 创建配置文件时确实更方便一点。
  </p>
  <p>只要使用 <code>java -jar <程序包名称>.jar -l</code> 就可以使用这个工具</p>
  <ol>注：
    <li>该工具同样也是命令行应用，无GUI界面，且现阶段文字排版样式也可能不太友好</li>
    <li>快捷创建配置文件模式仅可以为一个 Cloudflare Account 下含有的多个域名创建对应的配置文件</li>
  </ol>
</li>
</ol>

<h3>开发依赖</h3>
<ol>
  <li>Google Gson 2.8.5</li>
  <li>Jsoup 1.11.3</li>
  <li>Apache log4j 2.11.2</li>
  <li>Apache Commons CLI 1.4</li>
</ol>
  
<h3>已知问题</h3>
<ol>
  <li>在部分SSH客户端远程使用本应用时，会出现部分功能按键会变成乱码的情况(如 Backspace, Delete 等)，此应为该部分SSH客户端本身的问题(因为部分由其他语言编写的命令行程序也存在这种问题)，此问题会尽量解决，但不能保证可以完美解决。</br><i>在实体机的终端上使用暂时未发现这种问题</i></li>
  <li>同样在部分SSH客户端远程使用本应用时，会出现输入不可见内容(如 Global API Key)后会造成所有的输入内容都变成不可见内容的问题，且在不同的SSH客户端上可能会有不同的表现。可能是本应用设计上的问题，也可能是SSH客户端的问题，一样会尽量解决，但不能保证完美解决。</br><i>在实体机的终端上使用暂时未发现这种问题</i></li>
</ol>

<h3>反馈问题</h3>
<ol>
  <li>可以直接在这个page反馈</li>
  <li>也可以直接发邮件到 magiccrazymiao@gmail.com </li>
</ol>  
<h3>后记</h3>
<h4>关于版本号</h4>
<p>
  你可能已经发现，这应用版本号是从 <code>2.0.0</code> 开始的，那之前的版本去哪了呢？
  </br>
  其实，在这个版本之前的所有版本都是我自己使用的版本，那些版本所有信息都是被硬编码进去的，增添域名都需要修改源代码，是无法发布出来的
  </br>
  现在这个应用已经开发到可以 基 本 可 用 的版本，但我不想抛弃旧的版本号重新开始算，所以之前的版本号就都被保留下来了，而这个版本就从  <code>2.0.0</code> 开始起算
  </br>
  在此之前曾经存在过的版本有 <code>1.0.0</code> <code>1.0.2</code> <code>1.1.0</code> <code>1.1.1</code> <code>1.2.0</code> <code>1.2.1</code> <code>1.3.0</code> <code>1.3.1</code> <code>1.4.0</code>
  </br>
</p>
<h4>关于这个应用</h4>
<p>其实做这个纯属是为了好玩和锻炼自己，因为根本不知道有多少人会使用 动态IP + Cloudflare 的这种组合处理动态IP。<s>（可能只有我一个了吧）。</s>如果你发现这个应用对你有用，那就算是遇到了同道中人啦</p>
<p>
  我是一个新手菜鸡，求轻喷
</p>
 
