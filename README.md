## pvrtool可接收参数

* -c 配置文件名
* -s 输入文件夹，一般选择资源的根目录
* -o 输出文件夹
* -l 语言和地区，默认为zh_CN。一次只能处理一种语言版本
* -p TexturePacker路径, 不带该参数为TexturePacker在win7下的默认安装路径
* -t The Compressonator路径, 不带该参数为The Compressonator在win7下的默认安装路径
* -m FormatFactory路径，不带该参数为FormatFactory在win7下的默认安装路径，mp4压缩用
* -co (只处理配置文件。后面不需参数值)
* -f pvr,dxt,atc,png(只处理指定格式，用逗号分割)
* -v 3表示支持新版TexturePacker3.0+，默认是2.4，这个参数只对pvr有效

## 资源文件里的配置类型
type 表示资源的类型，可以配置为下面几项

* 0	子资源配置文件，pvrtool 会将该文件作为一个资源配置文件对其中的内容进行相同的处理
* 1	图片资源，该类型资源需要填写type1和type2部分，指定具体的处理方式
* 2 字符串配置文件，支持本地化。配置里写的文件名不需要带语言和地区后缀，工具生成后会根据语言和地区参数自动附加。
* 3 动画配置文件
* 4 zilb压缩的图片资源文件
* 5 zilb压缩的其他文件（只能是类型为 8 的文件）
* 6 mp4压缩文件，处理后扩展名为m4a（这个扩展名仍为mp3的话实测播放有问题）。mp4文件占用内存跟mp3相同，运行速度快一倍。
* 8	直接拷贝
* 9	忽略该文件，该文件不会出现在生成后的文件夹中

type1和type2，当资源为图片资源时，才需要配置该项，以指定具体的处理方式。type1表示ios平台（pvr）下的处理方式，type2表示android平台（dxt/atc/png）下的处理方式。具体的值和对应的处理方式如下表（第二行为一个1024\*1024图片的文件/内存峰值大小）：

|type1/type2|pvr|dxt|atc|png|使用场合说明|
|:--:|:--:|:--:|:--:|:--:|:--:|
|1|pvrtc4 512k/1M|dxt3 1M/2M|atc rgba 1M/2M|png8888 不确定/8M|高品质，带alpha|
|2|png8888 不确定/8M|png8888 不确定/8M|png8888 不确定/8M|png8888 不确定/8M|高清，需要无损，一般用在有alpha且需要高清的地方， 比如半透明的按钮|
|3|pvrtc2 256k/512k|dxt3|atc rgba|pvr.ccz(RGBA4444) 一般为png的一半/4M|有透明度的，但不需要高品质的，比如技能|
|4|pvrtc4(no alpha) 512k/1M|dxt3|atc rgba|pvr.ccz(RGB565) 一般为png的一半/4M|用在不带透明度的高品质图片上，质量高于pvrtc4|
|5|pvrtc2(no alpha) 256k/512k|etc rgb 512k/1M|etc rgb 512k/1M|etc rgb 512k/1M	用在不带透明度的低品质图片上|
|6|pvrtc4|dxt3|atc rgba|pvr ccz(RGBA5551)|只带一位透明度的|
7|pvr.ccz(RGBA4444)|pvr.ccz(RGBA4444)|pvr.ccz(RGBA4444)|pvr.ccz(RGBA4444)|png4444格式 (RGBA4444格式对素材进行优化，得到的并不是真正的png素材，处理的素材宽度必须是2的整数倍，高度不要求)。注意：这个处理后会把图片宽高变为2的幂|
8|pvr.ccz(RGBA4444)|pvr.ccz(RGBA4444)|pvr.ccz(RGBA4444)|pvr.ccz(RGBA4444)|png4444格式 (RGBA4444格式对素材进行优化，得到的并不是真正的png素材，处理的素材宽度必须是2的整数倍，高度不要求)。注意：这个不会裁剪透明区域，也不会改变图片尺寸|
9|jpz(jpeg+aphla)|jpz(jpeg+aphla)|jpz(jpeg+aphla)|jpz(jpeg+aphla)|jpz格式为支持透明度的jpeg格式。如果原文件带aphla信息，则用zlib压缩后附加在jpeg数据的后面，如果原文件是jpg或jpeg格式，则直接拷贝文件内容|
10|jpg|jpg|jpg|jpg|jpg格式(质量80%)|

