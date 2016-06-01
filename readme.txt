## 一个DownloadManager使用demo
- 设置观察者支持进度查询
- 下载进度全局广播，支持所有页面进度条更新
- 下载完成安装提示

## 优点
- 下载重定向、断线重连等完全交给DownloadManager处理。
- 稳定

## 缺点
- 魅族MX3 Flyme 4.5.7.1A getUriForDownloadedFile、DownloadManager.query(query)方法被篡改，无法查询到下载进度（目前仅发现这个版本）
- 乐视，下载完成的进度无法查询。（借别人手机测试的，型号忘记了。。。。。。）
- 由于厂家对DownloadManager的定制，移动网络下下载可能会没有提示且直接暂停（例如小米）。
