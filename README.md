用于在终端中将图像和GIF动画转换为ANSI彩色/灰度字符画。

支持静态图片（PNG/JPG/JPEG）和GIF动画

核心功能：

* 图像缩放（保持宽高比）

* 使用不同密度的ASCII字符表示灰度

* 支持真彩色（24位RGB）或单色显示

* 对GIF能正确处理帧延迟实现流畅动画

使用方法：

```text
java ANSI <文件路径> [是否彩色(true/false)] [缩放比例]
```
技术特点：

* 使用高质量双三次插值缩放图像

* 智能终端清屏策略（避免闪烁）

* 精确的帧率控制（GIF）

* 自动桌面路径查找（简化文件定位）

程序会在终端全屏显示转换后的字符画，GIF会循环播放动画。

效果：

原图片：

![1](https://github.com/user-attachments/assets/ca40b62b-79b0-47e7-b2fb-0d45260a47b6)

转换后：

![image](https://github.com/user-attachments/assets/f58d5477-7533-4ffb-ba57-5c8786cf51ba)

原gif:

![3](https://github.com/user-attachments/assets/d39ce93d-2b62-4432-9733-deff0162e670)

转换后：

![freecompress-test](https://github.com/user-attachments/assets/9cc96c8a-8bbb-47a0-9fcb-eae7338d8d4b)

