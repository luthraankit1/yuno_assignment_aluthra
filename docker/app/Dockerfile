FROM eclipse-temurin:8-jdk

WORKDIR /home/ubuntu

COPY etc /home/ubuntu

USER root

RUN apt update; apt install dos2unix -y

RUN dos2unix /home/ubuntu/start.sh; chmod +x /home/ubuntu/start.sh

EXPOSE 8080

CMD ["/bin/bash", "/home/ubuntu/start.sh"]