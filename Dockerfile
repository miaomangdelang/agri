# java8运行环境
FROM openjdk:8-jdk-alpine
# 作者名称
MAINTAINER Joing.Yao

# 切换工作目录
WORKDIR /home

# 添加pfsc.jar文件到docker环境内
ADD target/pfsc.jar /home/pfsc.jar
# 暴露端口8080
EXPOSE 8080
# 运行命令
ENTRYPOINT ["java", "-server", "-Xms256m", "-Xmx256m", "-jar", "/home/pfsc.jar"]