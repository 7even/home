FROM clojure:tools-deps

WORKDIR /tmp/build

COPY deps.edn .
RUN clojure -Spath > /dev/null

COPY . .
RUN clojure -Spom && clojure -A:release > /dev/null && \
    mkdir /app && \
    mv /tmp/build/home.jar /app/home.jar && rm -rf /tmp/build

WORKDIR /app

CMD ["java", "-jar", "home.jar", "-m", "home.core"]
