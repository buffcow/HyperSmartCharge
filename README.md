# 智能断充（HyperSmartCharge）
为支持**电池充电保护**的设备添加自定义断充阈值设置，妈妈再也不用担心我的手机过充啦！

Add custom charge protect value setting for devices that support **battery charge protection**.

> [!WARNING]  
> 本模块的使用所产生的所有后果，由使用者自行承担，项目组不承担任何责任，使用者应自行评估并承担相关风险。  
> 本项目不对任何任何衍生项目负责。

## 使用
1. 在 LSPosed 管理器中激活模块
2. 作用域勾选 安全服务(**`com.miui.securitycenter`**)
3. **重启手机**
4. 转到 **`省电与电池`** —— **`电池保护`** —— **`智能断充`** 设置阈值

## 注意
1. 需要设备本身支持电池保护功能并搭载 HyperOS 的系统
2. 支持调节范围为 20-100，推荐设置在 70-90 之间
3. 目前仅在 小米14(pro) 下测试通过

## 下载
[LSPosed 仓库](https://github.com/Xposed-Modules-Repo/cn.buffcow.hypersc/releases)

## 无效
请先检查设备是否支持电池保护功能，模块是否正常激活，并且作用域是否勾选。
<br>如果排查后仍有错误，请提交issue。或联系酷安[@buffcow](http://www.coolapk.com/u/1188320)(qingyu)

## 致谢
模块使用 [Yuki Hook API](https://github.com/fankes/YukiHookAPI) 构建
