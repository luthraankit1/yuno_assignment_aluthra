FROM mcr.microsoft.com/mssql/server:2025-latest

WORKDIR /usr/src/app

USER root
RUN apt update; apt install dos2unix -y

COPY config /usr/src/app

RUN dos2unix /usr/src/app/start.sh; chmod +x /usr/src/app/start.sh

ENV SA_PASSWORD Abcd@1234
ENV ACCEPT_EULA Y

EXPOSE 1433

CMD ["/bin/bash", "./start.sh"]