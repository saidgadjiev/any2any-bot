FROM ubuntu:18.04

WORKDIR /app
ENV DEBIAN_FRONTEND noninteractive 

RUN apt-get update -y -qq
RUN apt-get install -y -qq openjdk-11-jre wget gdebi p7zip-rar locales

# Locale
RUN sed -i -e \
  's/# ru_RU.UTF-8 UTF-8/ru_RU.UTF-8 UTF-8/' /etc/locale.gen \
   && locale-gen

ENV LANG ru_RU.UTF-8
ENV LANGUAGE ru_RU:ru
ENV LC_LANG ru_RU.UTF-8
ENV LC_ALL ru_RU.UTF-8

RUN wget https://github.com/wkhtmltopdf/wkhtmltopdf/releases/download/0.12.5/wkhtmltox_0.12.5-1.bionic_amd64.deb
RUN gdebi --n wkhtmltox_0.12.5-1.bionic_amd64.deb
RUN rm wkhtmltox_0.12.5-1.bionic_amd64.deb

RUN rm -rf /var/lib/apt/lists/*

COPY ./target/app.jar .
COPY ./license/license-19.lic ./license/
COPY ./tessdata/ ./tessdata/

ENTRYPOINT ["java"]
CMD ["-jar", "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005", "app.jar"]
