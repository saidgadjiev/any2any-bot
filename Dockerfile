FROM ubuntu:20.04

WORKDIR /app
ENV DEBIAN_FRONTEND noninteractive 
ENV USE_SANDBOX false

RUN apt-get update -y -qq
RUN apt-get install -y -qq openjdk-11-jre build-essential curl wget gdebi p7zip-rar locales rar zip unzip

RUN curl -sL https://deb.nodesource.com/setup_12.x | bash -
RUN apt-get install -y -qq nodejs

RUN wget https://dl.google.com/linux/direct/google-chrome-stable_current_amd64.deb
RUN gdebi --n google-chrome-stable_current_amd64.deb
RUN rm google-chrome-stable_current_amd64.deb

# Locale
ENV LC_ALL C.UTF-8

RUN wget https://github.com/wkhtmltopdf/wkhtmltopdf/releases/download/0.12.5/wkhtmltox_0.12.5-1.bionic_amd64.deb
RUN gdebi --n wkhtmltox_0.12.5-1.bionic_amd64.deb
RUN rm wkhtmltox_0.12.5-1.bionic_amd64.deb

RUN wget https://gif.ski/gifski-0.10.1.zip
RUN unzip -j gifski-0.10.1.zip -d gifski
RUN gdebi --n gifski/gifski_0.10.1_amd64.deb
RUN rm -rf gifski
RUN rm gifski-0.10.1.zip

RUN wget https://raw.githubusercontent.com/saidgadjiev/tgs-to-gif/master/cli.js
RUN wget https://raw.githubusercontent.com/saidgadjiev/tgs-to-gif/master/index.js
RUN wget https://raw.githubusercontent.com/saidgadjiev/tgs-to-gif/master/package.json
RUN npm install

RUN wget https://github.com/jankovicsandras/imagetracerjava/raw/master/ImageTracer.jar

RUN sed -Ei 's/^# deb-src /deb-src /' /etc/apt/sources.list
RUN apt-get update -y -qq
RUN apt-get install -y -qq autoconf libtool git-core
RUN apt-get -y -qq build-dep imagemagick libmagickcore-dev libde265 libheif

RUN cd /usr/src/ && \
git clone https://github.com/strukturag/libde265.git && \
git clone https://github.com/strukturag/libheif.git

RUN cd /usr/src/libde265/ && \
./autogen.sh && \
./configure && \
make && \
make install

RUN cd /usr/src/libheif/ && \
./autogen.sh && \
./configure && \
make && \
make install

RUN cd /usr/src/ && \
wget https://www.imagemagick.org/download/ImageMagick-7.0.10-14.tar.bz2 && \
tar xvf ImageMagick-7.0.10-14.tar.bz2 && \
cd ImageMagick-7.0.10-14 && \
./configure --with-heic=yes && \
make && \
make install

RUN ldconfig

RUN apt-get install -y -qq tesseract-ocr

RUN npm install -g phantomjs
RUN wget https://raw.githubusercontent.com/ariya/phantomjs/master/examples/rasterize.js

RUN rm -rf /var/lib/apt/lists/*

COPY ./license/license-19.lic ./license/
COPY ./target/app.jar .

ENTRYPOINT ["java"]
CMD ["-jar", "app.jar"]
