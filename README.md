# WirelessHid
基于无线wifi的hid实现（api 21, android5.0以上版本），使用android设备的触摸屏通过wifi网络（局域网最佳）控制pc上的鼠标指针和基本键盘数据输入。

整个架构是C/S架构的，其中android设备是client端，pc（windows/linux）是server端。Android上的输入移动事件通过网络打包发送给pc端，目前的打包是使用的google的protocol buffer，这是一个基于二进制的数据封装和解封装的开源库，详情请见：
https://developers.google.com/protocol-buffers/

各个目录说明：
  1. android 这个目录下的是android上的app源码，是整个架构的client端。目前工程是android studio的工程，可以使用android studio直接打开。
  2. pc 这个目录是pc(linux/windows)上的可执行程序的源码（目前是JAVA实现），同时包含了所需要的protobuf库。可以使用eclipse导入工程。
  3. bin 这个目录下是已经编译好的二进制文件，其中有一个android上的apk文件和一个平台系统无关的可执行的jar文件（在linux/windows上执行：java -jar WirelessHidServer.jar 即可执行），用户可以直接运行使用，无需从源码编译。

2016.06.28 更新
  1. 修改整体架构，将pc端作为server端，android端作为client端。
     pc端一直在监听来自android端的消息，如果android端有链接请求则建立请求；
     如果android端断开链接则pc端继续监听，直到有链接消息。
  2. 增加服务自动发现机制，pc端只需要运行程序即可，android端也只需要
     打开app，然后app会通过UDP的224.0.0.1地址进行组播查找服务，如果找到
     服务，则向服务器发起链接。
  3. 修改音量键对应的键值，音量下键对应方向键下键，音量上键对应方向键上键。

目前实现了以下功能：

  1. 鼠标移动控制
  2. 鼠标左击/右击控制
  3. 鼠标滚动轴控制
  4. 设置鼠标移动和滚轴滚动速度
  5. 键盘主要常用按键控制
  6. 键盘长按连续输入控制
  7. 通过按下手机的音量键的上下键来触发键盘的左右方向键，用于PPT演示时使用
  8. 通过摇晃手机（传感器实现）来触发键盘的右键，用于PPT演示的时候使用
  
目前的已知问题：

  1. 在windows上的性能不佳，有数据丢失的问题，鼠标键盘比较卡顿，linux上非常流畅。
  2. 目前的键值有的不正确（比如菜单键等，按照javadoc的键值不能使用），暂时没有找到合适的键值。
  4. 目前不能支持触摸板手势操作（如放大手势），不能支持键盘的组合键输入。
  5. 设置鼠标速度之后，鼠标坐标点不连贯问题

以下是运行时快照：
android端（client）:
  0. 搜索pc端服务（组播服务发现过程）
![screenshot 0](https://github.com/CreateChance/WirelessHid/blob/master/ScreenShot/Client_0.png)
  1. 触摸板
![screenshot 1](https://github.com/CreateChance/WirelessHid/blob/master/ScreenShot/Client_1.png)
  2. 主键盘
![screenshot 2](https://github.com/CreateChance/WirelessHid/blob/master/ScreenShot/Client_2.png)
  3. 从键盘
![screenshot 3](https://github.com/CreateChance/WirelessHid/blob/master/ScreenShot/Client_3.png)

pc端（server）:
  1. 正在监听android端的服务发现包  
![screenshot 4](https://github.com/CreateChance/WirelessHid/blob/master/ScreenShot/Server_1.png)
  2. 收到android端的服务发现包，响应android的服务链接请求，并且建立链接
![screenshot 5](https://github.com/CreateChance/WirelessHid/blob/master/ScreenShot/Server_2.png)
  3. 收到android端的断开请求包，断开链接，并且重新监听链接请求
![screenshot 6](https://github.com/CreateChance/WirelessHid/blob/master/ScreenShot/Server_3.png)
