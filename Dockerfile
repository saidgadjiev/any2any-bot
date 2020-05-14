FROM ubuntu:18.04

WORKDIR /app
ENV DEBIAN_FRONTEND noninteractive 

RUN apt-get update -y -qq
RUN apt-get install -y -qq openjdk-11-jre wget gdebi

RUN wget https://github.com/wkhtmltopdf/wkhtmltopdf/releases/download/0.12.5/wkhtmltox_0.12.5-1.bionic_amd64.deb
RUN gdebi --n wkhtmltox_0.12.5-1.bionic_amd64.deb
RUN rm wkhtmltox_0.12.5-1.bionic_amd64.deb

RUN rm -rf /var/lib/apt/lists/*

COPY ./target/app.jar .

ENTRYPOINT ["java"]
CMD ["-jar", "app.jar"]