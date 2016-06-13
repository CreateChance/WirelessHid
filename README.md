# WirelessHid
基于无线wifi的hid实现，使用android设备的触摸屏通过wifi网络（局域网最佳）控制pc上的鼠标指针和基本键盘数据输入。

整个架构是C/S架构的，其中android设备是server端，pc（windows/linux）是client端。Android上的输入移动事件通过网络打包发送给pc端，目前的打包是使用的google的protocol buffer，这是一个基于二进制的数据封装和解封装的开源库，详情请见：
https://developers.google.com/protocol-buffers/

目前实现了以下功能：

  1. 鼠标移动控制
  2. 鼠标左击/右击控制
  3. 鼠标滚动轴控制
  4. 键盘主要常用按键控制
  4. 键盘长按连续输入控制
  
目前的已知问题：

  1. 在windows上的性能不佳，有数据丢失的问题，鼠标键盘比较卡顿，linux上非常流畅。
  2. 目前的键值有的不正确（比如菜单键，win徽标键等），暂时没有找到合适的键值。
  3. 对特殊键盘的支持不够，比如上下左右移动键，home键，page up/down等
  4. 目前不能支持触摸板手势操作（如放大手势），不能支持键盘的组合键输入。

以下是运行时快照：
android端（server）:
![screenshot 1](https://github.com/CreateChance/WirelessHid/blob/master/ScreenShot/Screenshot_1.png)
![screenshot 2](https://github.com/CreateChance/WirelessHid/blob/master/ScreenShot/Screenshot_2.png)
pc端（client）:

![screenshot 1](https://github.com/CreateChance/WirelessHid/blob/master/ScreenShot/Selection_015.png)
