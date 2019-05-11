<h3><i>Version 2.3.0</i></h3>
<ul>
    <li>修改 包结构，对程序进行模组化</li>
    <li>增加 被动更新模式</li>
</ul>

<h3><i>Version 2.2.1</i></h3>
<ul>
    <li>修改 域名配置不再需要域名名称，名称将会在启动时从cloudflare处获取</li>
    <li>修改 不再使用操作系统ping指令更新域名ip。启动时从cloudflare处获取域名ip，之后一旦更新成功，域名ip将自动变为本地ip</li>
    <li>增加 支持JavaScript脚本获取IP地址</li>
</ul>

<h3><i>Version 2.2.0</i></h3>
<ul>
    <li>删除 冗余代码</li>
    <li>添加 源代码文件license</li>
    <li>添加 控制台命令功能</li>
    <li>修改 ConfigurationJson类为Configuration类</li>
    <li>修改 log4j2日志内容，缩短记录内容</li>
    <li>修改 log4j2日志文件策略，不再将错误信息打印于控制台</li>
    <li>修复 配置文件创建工具 创建配置文件错误的问题</li>
    <li>修复 DNS记录查询及快捷配置文件创建工具 无法正常退出的问题</li>
</ul>

<h3><i>Version 2.1.1</i></h3>
<ul>
    <li>修复 部分情况下出现无法读取jar包内文件的问题</li>
    <li>修改 log4j2日志文件行为策略</li>
</ul>

<h3><i>Version 2.1.0</i></h3>
<ul>
    <li>添加 ChangeLog.md 更新日志</li>
    <li>添加 自定义日志文件储存位置(logHome)的全局json配置</li>
    <li>修改 log4j2日志文件命名</li>
    <li>修改 log4j2日志文件策略,RollingRandomAccessFile改用RollingFile,并启用createOnDemand策略</li>
    <li>修改 初始化配置文件的方式</li>
    <li>修复 DNS记录查询及快捷配置文件创建工具 创建配置文件错误的问题</li>
    <li>修复 在某些情况下命令行参数无效的问题</li>
</ul>

<h3><i>Version 2.0.0</i></h3>
<ul>
    <li>发行 初始版本</li>
</ul>