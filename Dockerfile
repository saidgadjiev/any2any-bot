FROM ubuntu:18.04

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

RUN wget https://gif.ski/gifski-0.10.1.zip
RUN unzip -j gifski-0.10.1.zip -d gifski
RUN gdebi --n gifski/gifski_0.10.1_amd64.deb
RUN rm -rf gifski
RUN rm gifski-0.10.1.zip

RUN wget https://raw.githubusercontent.com/saidgadjiev/tgs-to-gif/master/cli.js
RUN wget https://raw.githubusercontent.com/saidgadjiev/tgs-to-gif/master/index.js
RUN wget https://raw.githubusercontent.com/saidgadjiev/tgs-to-gif/master/package.json
RUN npm install

COPY ./target/app.jar .
COPY ./license/license-19.lic ./license/
COPY ./tessdata/ ./tessdata/

RUN rm -rf /var/lib/apt/lists/*

ENTRYPOINT ["java"]
CMD ["-jar", "app.jar"]
