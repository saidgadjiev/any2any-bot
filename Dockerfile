FROM ubuntu:20.04

RUN useradd -ms /bin/bash bot
USER bot

RUN mkdir -p /home/bot/app
WORKDIR /home/bot/app

USER root

ENV DEBIAN_FRONTEND noninteractive
ENV USE_SANDBOX false

RUN sed -Ei 's/^# deb-src /deb-src /' /etc/apt/sources.list
RUN apt-get update -y && \
apt-get -y build-dep imagemagick && \
apt-get install -y openjdk-11-jre build-essential curl wget p7zip-rar locales rar zip unzip \
libwebp-dev libopenjp2-7-dev librsvg2-dev libde265-dev libheif-dev

# Locale
ENV LC_ALL C.UTF-8

RUN echo 'Imagemagick 7.0.10-19'
RUN cd /usr/src/ && \
wget https://www.imagemagick.org/download/ImageMagick.tar.bz2 && \
tar xvf ImageMagick.tar.bz2 && cd ImageMagick* && \
./configure && make -j 4 && \
make install && \
make distclean && ldconfig

RUN apt-get clean -y && apt-get autoclean -y && apt-get autoremove -y && rm -rf /var/lib/apt/lists/* /var/tmp/*

USER bot

COPY ./api.json .
COPY ./target/app.jar .


EXPOSE 8080
ENTRYPOINT ["java"]
CMD ["-jar", "app.jar"]
#CMD ["-jar", "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005", "app.jar"]
