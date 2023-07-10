# C-Helper
C-Helper是一个针对C语言初学者的静态分析工具。
它可以发现并指出初学者容易出现的源代码中的问题。

## 许可
C-Helper是在MIT许可下发布的。
参见LICENSE以了解更多信息。

##  安装
C-Helper是一个Eclipse插件。
本节描述了从获得Eclipse到安装C-Helper的步骤。

### All in One Eclipse 使用准备
Eclipse可以从官方网站和志愿者那里获得各种软件包。
本节介绍了如何安装All in One Eclipse，它被认为是对首次使用的用户来说最简单的。

All in One Eclipse只针对Windows发布、
如果您使用的是其他操作系统，请从Eclipse官方网站下载相应的Eclipse。

1. 下载
All in One Eclipse是一个由MergeDoc项目分发的软件包、
顾名思义，一开始就包含了有用的插件。
它也被翻译成了日语。

    它可以从[Pleiades - Eclipse Plug-in日文转换插件](http://mergedoc.sourceforge.jp)下载。
在撰写本文时，"Eclipse 4.2 Juno Pleiades All in One "是最新版本。
下载最新版本。

    All in One Eclipse有平台版、终极版、Java版、C/C++版和其他类型，取决于开发目标。
此外，每种类型都有完整版或标准版。所包含的插件和软件是不同的。对于C语言开发，请选择Ultimate或C/C++完整版；

2. 安装
安装All in One Eclipse是非常容易的。只需将下载的 zip 文件解压到一个合适的位置。应该下载一个诸如pleiades-e4.2-cpp-32bit-jre\_20121123.zip的文件、将其解压到一个合适的目录。

### 准备C-Helper
C-Helper是一个Eclipse插件。
它可以从更新网站上安装。

通过更新站点安装该插件的过程与其他Eclipse插件的过程相同。
该程序是[如何安装额外的Eclipse插件-ID-Blogger|Infinity Dimensions](http://www.infinity-dimensions.com/blog/archives/eclipse-plugin-install.html)中的图片来了解更多信息。

在安装C-Helper的过程中，有两个特定的信息

- 在'Add Repository'中要指定的位置如下。
名称'可以是可选的。

        https://github.com/uchan-nos/c-helper/raw/master/site

- 安装_Static Analyzers_ > _C-Helper_!

这样就完成了C-Helper的准备工作。

### 如何使用。
! [C-Helper图标](https://github.com/uchan-nos/c-helper/raw/master/icons/analysis.png)
是启动C-Helper的图标。
如果你在编辑器中打开某个C语言程序的源代码时点击这个图标、
就会立即进行分析，检测到的问题会在 "问题 "视图中列出。
