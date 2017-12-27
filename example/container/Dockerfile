FROM java

ADD *.jar /usr/local/bin
ADD entrypoint.sh /

RUN mkdir /etc/catapult
ADD server.yml.template /etc/catapult

RUN chmod +x /entrypoint.sh

ENTRYPOINT ["/entrypoint.sh"]