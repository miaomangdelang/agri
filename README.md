### 爬虫获取全国农业信息网 ###

```shell
# 构建镜像
docker rm -f pfsc
docker image rm pfsc:1.0.0
docker build -t pfsc:1.0.0 .
docker run --name pfsc -p 8080:8080 -v /home/log/pfsc:/home/appLog -e TZ="Asia/Shanghai" -d pfsc:1.0.0
```/