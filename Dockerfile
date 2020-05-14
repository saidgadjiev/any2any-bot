FROM openjdk:11-jre

WORKDIR /app
COPY ./target/app.jar .
RUN sed 's/main$/main universe/' -i /etc/apt/sources.list
RUN apt-get update
RUN apt-get upgrade -y

# Download and install wkhtmltopdf
RUN apt-get install -y build-essential xorg libssl-dev libxrender-dev wget gdebi
RUN wget "https://github.com/wkhtmltopdf/wkhtmltopdf/releases/download/0.12.5/wkhtmltox_0.12.5-1.trusty_amd64.deb"
RUN gdebi --n "wkhtmltox-0.12.2.1_linux-trusty-amd64.deb"

ENTRYPOINT ["java"]
CMD ["-Duser.timezone=UTC", "-Duser.language=ru", "-jar", "app.jar"]
