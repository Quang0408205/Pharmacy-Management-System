FROM mongo:7
COPY entrypoint.sh /usr/local/bin/replica-entrypoint.sh
RUN chmod +x /usr/local/bin/replica-entrypoint.sh
ENTRYPOINT ["replica-entrypoint.sh"]
CMD ["mongod", "--replSet", "rs0", "--keyFile", "/data/keyfile", "--bind_ip_all"]
