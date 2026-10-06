<div align="center">

# 拾光课程表

![GitHub Downloads (all assets, all releases)](https://img.shields.io/github/downloads/XingHeYuZhuan/shiguangschedule/total)  ![GitHub License](https://img.shields.io/github/license/XingHeYuZhuan/shiguangschedule)  ![GitHub Repo stars](https://img.shields.io/github/stars/XingHeYuZhuan/shiguangschedule)

<a href="https://hellogithub.com/repository/XingHeYuZhuan/shiguangschedule" target="_blank"><img src="https://abroad.hellogithub.com/v1/widgets/recommend.svg?rid=9f09baa3931a4f79940100829d4d0ca4&claim_uid=TRP24EpKZ5sx68l&theme=neutral" alt="Featured｜HelloGitHub" style="width: 250px; height: 54px;" width="250" height="54" /></a>
<a href="https://trendshift.io/repositories/45835?utm_source=trendshift-badge&amp;utm_medium=badge&amp;utm_campaign=badge-trendshift-45835" target="_blank" rel="noopener noreferrer"><img src="https://trendshift.io/api/badge/trendshift/repositories/45835/daily?language=Kotlin" alt="XingHeYuZhuan%2Fshiguangschedule | Trendshift" width="250" height="55"/></a>

#### 与我们交流和讨论 ![](https://img.shields.io/badge/QQ_频道-pd68794181-blue)

本仓库为拾光课程表（shiguangschedule）项目的主仓库，包含软件的全部源代码及相关资源。项目采用开源模式，欢迎社区开发者参与贡献和适配。

</div>  


## 预览图
| 周课表页面(支持长按调整课程块位置或者高度) | 课表页面个性化配置(深色模式演示) | 小组件选择 |
| :---: | :---: | :---: |
| ![图片1](/picture/Screenshot_1.png) | ![图片2](/picture/Screenshot_2.png) | ![图片3](/picture/Screenshot_3.png) |

## 项目定位

拾光课程表是一款面向中国高校师生的课程表管理工具，支持通过适配脚本导入各类教务系统课程数据，方便用户高效管理个人课表。项目注重开放性和可扩展性，鼓励社区开发者参与适配和功能完善。


## 关于项目版本
> [!WARNING]
> 从**2.x**版本开始项目已迁移到Compose Multiplatform多平台框架  
> 不再区分正式版本和开发者版本，开发者功能通过在**更多**页面点击应用图标**5次**启用
> - Android：支持Android 8.0 +的系统版本
>   - *主支维护*
> - Desktop (JVM)：支持Windows、macOS、Linux
>   - *主支维护*
>   - **当前平台处于实验性阶段**
> - iOS：本仓库dev_ios分支提供实验性基础支持
>   - *分支维护*
>   - **需自行编译**
> - 其他平台适配正在开发中

## 功能介绍

### 主页面
- 今日课表
  > 顾名思义就是看当天的课程
- 课表
  > 显示一周的课程，左右滑动可以切换周次，点击顶部周次标题会弹出底部选择器用于快速跳转周次(会标记当前日期所在周)
- 我的
  > 也就是设置页面，这里放置所有的配置项

#### 深色适配
- 目前软件所有可以实现深色适配的位置都已全部实现深色适配
- 小组件也拥有深色适配

#### 课表配置
- 支持时间列表与课表绑定,切换不同的课表自动改变时间
- 课表支持独立定义，一周的起始日是周一还是周日
  
#### 小组件  
- 多种功能小组件任其选择  
  > [全部小组件预览图](/picture/all_widget.png)。
- 小组件支持明日预告
  > 超小小组件不支持明日预告,因为只有2x1的尺寸,设计上是用来展示最近一节课的消息  

#### 全局课程管理  
- 独立的课程管理页面将所有课程以最直接的方式全局显示出来，支持快速添加以及修改  

#### 课表页面个性化配置  
- 支持自定义背景图片  
- 支持调整课表格子高度、圆角、间距及透明度  
- 支持自定义课程块颜色  
- 支持调整课程块内容样式  
- 更多功能等你去发现...

#### 课程导入与导出  
- json文件导入与导出，方便课表备份  
- 通用ics文件导出，支持多种设备以及平台的日程导入 
- 在支持的设备上支持同步日历日程账户
- 支持WebDAV云备份和同步
- 教务导入，开源适配仓库开发者深度适配学校作息时间，一键导入  
  > 识别课程与对应作息时间是适配的基础功能，更高级的自动配置开学日期等 与开发者是否适配学校可能拥有的接口有关  

#### 课程提醒相关  
- 课程提醒与上课自动化开启勿扰或者静音  
- 获取整年节假日数据防止节假日课程打扰  
  
#### 语言支持  
- 简体中文 
- 繁体中文 
- 英语  
-----

## 如何参与

1. Fork 本仓库，提交你的改进或教务适配使用的可调用组件。
2. 提交 Pull Request，等待审核合并(main分支已经开启分支保护,提交需要提交到dev分支)。
3. 如有问题或建议，欢迎在 GitHub 提交 Issue 或加入社区讨论。

## 相关链接

- 项目主页：[https://github.com/XingHeYuZhuan/shiguangschedule](https://github.com/XingHeYuZhuan/shiguangschedule)
- 适配脚本仓库：[https://github.com/XingHeYuZhuan/shiguang_warehouse](https://github.com/XingHeYuZhuan/shiguang_warehouse)
- 查看如何适配,Wiki：[https://github.com/XingHeYuZhuan/shiguangschedule/wiki](https://github.com/XingHeYuZhuan/shiguangschedule/wiki)
- 浏览器测试插件:[https://github.com/XingHeYuZhuan/shiguang_Tester](https://github.com/XingHeYuZhuan/shiguang_Tester)  

- ###### 由[@Jursin](https://github.com/Jursin)主导并维护的网站:[https://sgschedule.jursin.top/](https://sgschedule.jursin.top/)  
---

如有问题或建议，欢迎提交 Issue 或 PR。

## 贡献  
欢迎任何人提交你的贡献  
### 教务适配贡献  
<a href="https://github.com/XingHeYuZhuan/shiguang_warehouse/graphs/contributors">
  <img src="https://contrib.rocks/image?repo=XingHeYuZhuan/shiguang_warehouse" />
</a>

### 软件开发贡献  
<a href="https://github.com/XingHeYuZhuan/shiguangschedule/graphs/contributors">
  <img src="https://contrib.rocks/image?repo=XingHeYuZhuan/shiguangschedule" />
</a>


## Star 历史
<a href="https://www.star-history.com/?repos=XingHeYuZhuan%2Fshiguangschedule&type=timeline&legend=top-left">
 <picture>
   <source media="(prefers-color-scheme: dark)" srcset="https://api.star-history.com/chart?repos=XingHeYuZhuan/shiguangschedule&type=timeline&theme=dark&legend=top-left" />
   <source media="(prefers-color-scheme: light)" srcset="https://api.star-history.com/chart?repos=XingHeYuZhuan/shiguangschedule&type=timeline&legend=top-left" />
   <img alt="Star History Chart" src="https://api.star-history.com/chart?repos=XingHeYuZhuan/shiguangschedule&type=timeline&legend=top-left" />
 </picture>
</a>