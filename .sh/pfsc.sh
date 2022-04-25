#! /bin/bash
docker rm -f pfsc
docker image rm pfsc:1.0.0
docker build -t pfsc:1.0.0 .
docker run --always --name pfsc -p 8080:8080 -v /home/log/pfsc:/home/appLog -e TZ="Asia/Shanghai" -d pfsc:1.0.0