## AdoptOpenJDK 停止发布 OpenJDK 二进制，而 Eclipse Temurin 是它的延伸，提供更好的稳定性
## 感谢复旦核博士的建议！灰子哥，牛皮！
## ZS-ENG-002：运行镜像与构建基线对齐 JDK 17（D-02 已确认 JDK 17 维护线，Maven enforcer 强制 [17,18)）
FROM eclipse-temurin:17-jre

## 创建目录，并使用它作为工作目录
RUN mkdir -p /zszj-server
WORKDIR /zszj-server
## 将后端项目的 Jar 文件，复制到镜像中
COPY ./target/zszj-server.jar app.jar

## 设置 TZ 时区
ENV TZ=Asia/Shanghai
## 设置 JAVA_OPTS 环境变量，可通过 docker run -e "JAVA_OPTS=" 进行覆盖
ENV JAVA_OPTS="-Xms512m -Xmx512m -Djava.security.egd=file:/dev/./urandom"

## 应用参数
ENV ARGS=""

## 暴露后端项目的 48080 端口
EXPOSE 48080

## 启动后端项目
CMD java ${JAVA_OPTS} -jar app.jar $ARGS
