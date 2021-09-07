# SystemCameraDemo
Android 相机入门Demo，系统相机的使用和自定义相机


# 图片播放器组件
## 功能说明
- 适配了MTK、RTK、AML三家不同soc厂商的提供的4K、8K高清方案以及Google原生的方式来显示图片。
- 支持本地图片和网络图片。
- 支持自动预加载上一张和下一张图片，来提升用户浏览图片的流畅度。
- 支持对图片进行放大、缩小、旋转操作。
- 提供了自动播放图片的功能。定时切换下一张。
- 可自定义自动播放图片的间隔时间。
- 可自定义自动播放图片的播放模式。默认是循环播放，还支持随机播放和顺序播放。 

## 如何集成
1. 添加依赖
在build.greadle文件中添加如下依赖
implementation 'com.tcl.ff.component:picture-player:1.0.2'//在组件管理中查看最新版本
2. 使用步骤
- 获取播放器管理类对象
```
//PicturePlayerManager类是一个单例，PicturePlayerManager是对外播放图片的公开类，所有的功能接口都是通过它进行调用
PicturePlayerManager playerManager = PicturePlayerManager.getInstance();
```
- 初始化播放器
```
/**
* 此方法必须在主线程进行调用
*参数说明
* context:上下文
* playerType: 播放器类型T_MEDIA_PLAYER(RT2851、MT9615、MT9221、T972、T963),M_MEDIA_PLAYER(MT9652、848、838),COMMON_PLAYER
* pictureModel: 播放图片实体类
* layout:用于显示图片内容View的父布局(View和此父布局同大小)
* decorView: 显示图片Activity的DecorView，因在soc层播放，故需要将osd层所有的View背景设置为透明
*/
playerManager.init(Context context,PicturePlayerType playerType,PictureModel pictureModel,FrameLayout layout,View decorView);
```
- 设置播放状态监听器
```
//会在主线程回调图片播放相关的状态
playerManager.setOnInfoListener(mOnInfoListener)；
```
- 释放播放器资源
```
//在程序的onStop中调用此方法进行播放器资源的释放
playerManager.releasePlayer()；
```
3. 功能接口说明
- 放大
```
playerManager.zoomIn();
```
- 缩小
```
playerManager.zoomOut();
```
- 旋转
```
playerManager.rotatePicture();
```
- 开始/暂停自动播放
```
playerManager.autoPlayOrPause();
```
- 设置播放模式
```
playerManager.setPlayMode(int mode);
```
- 设置播放间隔时间
```
// slideInterval : 自动播放间隔时间，单位s
playerManager.setSlideInterval(int slideInterval);
```

## [版本日志](http://gitlab03.tclking.com/applications/component/android/pictureplayer/wikis/CHANGELOG)
v1.0.0


## 审核人员

* 名称: `任嘉义`
* 部门: `运营技术部`
* 邮箱: `jiayi.ren@tcl.com`
* 
